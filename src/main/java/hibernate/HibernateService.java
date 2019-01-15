package hibernate;

import io.netty.util.internal.ThreadLocalRandom;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import utils.vertx.VertxUtils;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  Call start() before and stop() after using it.
 */
public class HibernateService {

    /*
     *  //////////////////////////////////////////////////////////////
     *                          CONFIGURATION
     *  //////////////////////////////////////////////////////////////
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateService.class);

    private EntityManagerFactory entityManagerFactory;
    private Map<String, EntityManager> entityManagers;
    private Vertx vertx;
    private JsonObject config;

    /**
     *
     * @param vertx - current Vert.x instance
     * @param config - JPA config over persistence.xml
     */
    public HibernateService(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
        this.entityManagers = new ConcurrentHashMap<>();
    }

    /*
     *  //////////////////////////////////////////////////////////////
     *                      SERVICE LIFE CYCLE
     *  //////////////////////////////////////////////////////////////
     */

    /**
     * Call this init-method before using
     * @param startFuture
     */
    public void start(Future<Void> startFuture) {
        LOGGER.info("----- Startup Hibernate service");

        String persistenceUnit = config.getString("persistence-unit");
        if (persistenceUnit == null) {
            startFuture.fail("No persistence-unit specified in config");
            return;
        }
        vertx.executeBlocking(
                future ->
                        VertxUtils.completeWithCatch(
                                future,
                                () -> entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit)
                        ),
                res ->
                        VertxUtils.resolveAsyncResult(
                                res,
                                startFuture,
                                () -> LOGGER.info("----- Init done"),
                                () -> LOGGER.info("----- Init failed")
                        )
        );
    }

    /**
     * Call this destroy-method after using
     * @param stopFuture
     */
    public void stop(Future<Void> stopFuture) {
        LOGGER.info("----- Stop Hibernate service");
        entityManagers.values().forEach(this::closeSilently);
        entityManagers.clear();
        stopFuture.complete();
    }

    /*
     *  //////////////////////////////////////////////////////////////
     *                      SESSION LIFE CYCLE
     *  //////////////////////////////////////////////////////////////
     */

    /**
     * Creates entity manager and gives id as result
     * @param resultHandler
     */
    public void createSession(Handler<AsyncResult<String>> resultHandler) {
        vertx.executeBlocking(future -> {
            VertxUtils.supplyWithCatch(future, () -> {
                String sessionId = generateSessionId();
                EntityManager em = entityManagerFactory.createEntityManager();
                entityManagers.put(sessionId, em);
                return sessionId;
            });
        }, res -> resultHandler.handle(HibernateAsyncResult.of(res, String.class)));
    }

    /**
     * Executes part of code with entity manager associated with id
     * @param sessionId
     * @param blockingHandler - part of code with entity manager that should be executed
     * @param resultHandler
     */
    public <T> void withSession(String sessionId, Function<EntityManager, T> blockingHandler, Handler<AsyncResult<T>> resultHandler) {
        withEntityManager(() -> getManagerOrFailHandler(sessionId, resultHandler), blockingHandler, resultHandler);
    }

    /**
     * Clears entity manager with id
     * @param sessionId
     * @param resultHandler
     */
    public void clearSession(String sessionId, Handler<AsyncResult<Void>> resultHandler) {
        EntityManager entityManager = getManagerOrFailHandler(sessionId, resultHandler);
        vertx.executeBlocking(future -> {
            VertxUtils.completeWithCatch(future, entityManager::clear);
        }, res -> resultHandler.handle(HibernateAsyncResult.of(res, Void.class)));
    }

    /**
     * Flushes and closes entity manager with id
     * @param sessionId
     * @param resultHandler
     */
    public void closeSession(String sessionId, Handler<AsyncResult<Void>> resultHandler) {
        EntityManager entityManager = getManagerOrFailHandler(sessionId, resultHandler);
        vertx.executeBlocking(future -> {
            VertxUtils.completeWithCatch(future, () -> {
                entityManagers.remove(sessionId);
                entityManager.flush();
                if (entityManager.getTransaction() != null) {
                    entityManager.getTransaction().commit();
                }
                entityManager.close();
            });
        }, res -> resultHandler.handle(HibernateAsyncResult.of(res, Void.class)));
    }

    /**
     * Checks is session is active
     * @param sessionId
     * @return is session has been in map and has associated entity manager
     */
    public boolean isSessionActive(String sessionId) {
        Objects.requireNonNull(sessionId);
        return entityManagers.containsKey(sessionId)
                && entityManagers.get(sessionId) != null;
    }

    private String generateSessionId() {
        String generatedId;
        do {
            generatedId = "HS-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt();
        } while (entityManagers.containsKey(generatedId));
        return generatedId;
    }

    private EntityManager getManager(String sessionId) {
        return entityManagers.getOrDefault(sessionId, null);
    }

    private <T> EntityManager getManagerOrFailHandler(String sessionId, Handler<AsyncResult<T>> failHandler) {
        if (!isSessionActive(sessionId)) {
            failHandler.handle(new HibernateAsyncResult<>("No entity manager found with id : " + sessionId));
        }
        return entityManagers.get(sessionId);
    }

    /*
     *  //////////////////////////////////////////////////////////////
     *                      ENTITY MANAGER LIFE CYCLE
     *  //////////////////////////////////////////////////////////////
     */

    /**
     * Executes part of code with new entity manager
     * @param blockingHandler - part of code with entity manager that should be executed
     * @param resultHandler
     */
    public <T> void withEntityManager(Function<EntityManager, T> blockingHandler, Handler<AsyncResult<T>> resultHandler) {
        withEntityManager(() -> entityManagerFactory.createEntityManager(), blockingHandler, resultHandler);
    }

    private void closeSilently(EntityManager em) {
        try {
            if (em != null && em.isOpen()) {
                em.flush();
                em.close();
            }
        } catch (RuntimeException re) {
            LOGGER.error(re);
        }
    }

    private <T> void withEntityManager(Supplier<EntityManager> supplier, Function<EntityManager, T> blockingHandler, Handler<AsyncResult<T>> resultHandler) {
        vertx.executeBlocking(handler -> {
            EntityManager em = null;
            try {
                em = supplier.get();
                T result = blockingHandler.apply(em);
                handler.complete(result);
            } catch (Throwable t) {
                handler.fail(t);
            } finally {
                closeSilently(em);
            }
        }, resultHandler);
    }


    /*
     *  //////////////////////////////////////////////////////////////
     *                          TRANSACTIONS
     *  //////////////////////////////////////////////////////////////
     */

    /**
     * Executes part of code with new entity manager in transaction
     * @param blockingHandler - part of code with entity manager that should be executed
     * @param resultHandler
     */
    public <T> void withinTransaction(Function<EntityManager, T> blockingHandler, Handler<AsyncResult<T>> resultHandler) {
        vertx.executeBlocking(handler -> {
            EntityManager em = null;
            try {
                em = entityManagerFactory.createEntityManager();
                EntityTransaction tx = em.getTransaction();
                tx.begin();
                T result = blockingHandler.apply(em);
                tx.commit();
                handler.complete(result);
            } catch (Throwable t) {
                handler.fail(t);
            } finally {
                closeSilently(em);
            }
        }, resultHandler);
    }
}
