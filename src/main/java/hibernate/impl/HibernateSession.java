package hibernate.impl;

import hibernate.IHibernateSession;
import hu.akarnokd.rxjava2.interop.ObservableInterop;
import io.netty.util.internal.ThreadLocalRandom;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.HibernateException;
import utils.vertx.VertxUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.function.Supplier;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class HibernateSession implements IHibernateSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateSession.class);

    private String id;
    private HibernateService service;
    private Vertx vertx;
    private EntityManager em;
    private EntityTransaction userTx;

    protected HibernateSession(HibernateService hibernateService) {
        this.vertx = hibernateService.getVertx();
        this.service = hibernateService;
        this.id = generateSessionId();
        this.em = hibernateService.getEntityManagerFactory().createEntityManager();
        this.service.getSessions().add(this);
    }

    @Override
    public Completable close() {
        Maybe<Boolean> closeMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            service.getSessions().remove(this);
            closeEm();
        });
        return Completable.fromMaybe(closeMaybe);
    }

    @Override
    public Completable clear() {
        Maybe<Boolean> clearMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            checkActive();
            em.clear();
        });
        return Completable.fromMaybe(clearMaybe);
    }

    @Override
    public Completable flush() {
        Maybe<Boolean> flushMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            checkActive();
            inTransaction(em::flush);
        });
        return Completable.fromMaybe(flushMaybe);
    }

    @Override
    public <T> Single<T> persist(T model) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> {
            em.persist(model);
            return model;
        }).toSingle();
    }

    @Override
    public <T> Single<T> merge(T model) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> {
            em.merge(model);
            return model;
        }).toSingle();
    }

    @Override
    public <T> Single<T> remove(T model) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> {
            em.remove(model);
            return model;
        }).toSingle();
    }

    @Override
    public <T> Single<T> refresh(T model) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> {
            em.refresh(model);
            return model;
        }).toSingle();
    }

    @Override
    public <T> Maybe<T> find(Class<T> clazz, Long id) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> em.find(clazz, id));
    }

    @Override
    public <T> Observable<T> select(HibernateQuery jpqlQuery, Class<T> clazz) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> {
            String jpql = jpqlQuery.getJpql();
            TypedQuery<T> selectQuery = em.createQuery(jpql, clazz);
            jpqlQuery.getValues().forEach(selectQuery::setParameter);
            return selectQuery.getResultStream();
        }).flatMapObservable(ObservableInterop::fromStream);
    }

    @Override
    public <T> Observable<T> select(String jpqlString, Class<T> clazz) {
        return select(new HibernateQuery(jpqlString), clazz);
    }

    @Override
    public Single<Integer> execute(HibernateQuery jpqlQuery) {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> {
            String jpql = jpqlQuery.getJpql();
            Query executeQuery = em.createQuery(jpql);
            jpqlQuery.getValues().forEach(executeQuery::setParameter);
            return inTransactionReturn(executeQuery::executeUpdate);
        }).toSingle();
    }

    @Override
    public Completable beginTransaction() {
        Maybe<Boolean> beginTxMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            if (userTx != null) {
                throw new HibernateException("HS: Transaction is already started: " + id);
            }
            userTx = em.getTransaction();
            userTx.begin();
        });
        return Completable.fromMaybe(beginTxMaybe);
    }

    @Override
    public Completable commitTransaction() {
        Maybe<Boolean> commitTxMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            if (userTx == null) {
                throw new HibernateException("HS: No active transaction: " + id);
            }
            userTx.commit();
            userTx = null;
        });
        return Completable.fromMaybe(commitTxMaybe);
    }

    @Override
    public Completable rollbackTransaction() {
        Maybe<Boolean> rollbackTxMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            if (userTx == null) {
                throw new HibernateException("HS: No active transaction: " + id);
            }
            userTx.rollback();
            userTx = null;
        });
        return Completable.fromMaybe(rollbackTxMaybe);
    }

    @Override
    public Single<Boolean> isActive() {
        return VertxUtils.rxExecuteAndSupply(vertx, this::isActiveSync).toSingle();
    }

    @Override
    public boolean isActiveSync() {
        return em != null && em.isOpen() && service.getSessions().contains(this);
    }

    //////////////////////////

    private void inTransaction(Runnable action) {
        if (userTx == null) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                action.run();
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } else {
            action.run();
        }
    }

    private <T> T inTransactionReturn(Supplier<T> supplier) {
        if (userTx == null) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                T value = supplier.get();
                tx.commit();
                return value;
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        } else {
            return supplier.get();
        }
    }

    private void closeEm() {
        try {
            if (em != null && em.isOpen()) {
                inTransaction(em::flush);
                em.close();
            }
        } catch (Exception e) {
            LOGGER.error("HS: " + e);
            throw new HibernateException(e);
        }
    }

    private String generateSessionId() {
        return "HS-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt() + "-" + service.getSessions().size();
    }

    private void checkActive() {
        if (!isActiveSync()) {
            throw new HibernateException("Session is inactive: " + id);
        }
    }
}
