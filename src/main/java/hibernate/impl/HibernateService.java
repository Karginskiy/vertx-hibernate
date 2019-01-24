package hibernate.impl;

import hibernate.IHibernateService;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.HibernateException;
import utils.vertx.VertxUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashSet;

@Getter(AccessLevel.PROTECTED)
public class HibernateService implements IHibernateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateService.class);

    private io.vertx.reactivex.core.Vertx vertx;
    private JsonObject config;
    private EntityManagerFactory entityManagerFactory;
    private HashSet<HibernateSession> sessions;

    public HibernateService(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = io.vertx.reactivex.core.Vertx.newInstance(vertx);
        this.sessions = new HashSet<>();
    }

    @Override
    public Completable start() {
        Maybe<Boolean> startMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            LOGGER.info("----- HS: Startup");
            String persistenceUnit = config.getString("persistence-unit");
            if (persistenceUnit == null) {
                throw new HibernateException("HS: No persistence-unit specified in config: ");
            }
            this.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
        });
        return Completable.fromMaybe(startMaybe)
                .doOnComplete(() -> LOGGER.info("----- HS: Init done"))
                .doOnError(t -> LOGGER.info("----- HS: Init failed"));
    }

    @Override
    public Completable stop() {
        Maybe<Boolean> stopMaybe = VertxUtils.rxExecuteAndComplete(vertx, () -> {
            LOGGER.info("----- HS: Stop Hibernate service");
            sessions.forEach(HibernateSession::close);
            sessions.clear();
        });
        return Completable.fromMaybe(stopMaybe)
                .doOnComplete(() -> LOGGER.info("----- HS: Destroy done"))
                .doOnError(t -> LOGGER.info("----- HS: Destroy failed"));
    }

    @Override
    public Single<HibernateSession> createSession() {
        return VertxUtils.rxExecuteAndSupply(vertx, () -> new HibernateSession(this)).toSingle();
    }
}