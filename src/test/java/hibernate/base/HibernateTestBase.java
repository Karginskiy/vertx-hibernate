package hibernate.base;

import hibernate.impl.HibernateQuery;
import hibernate.impl.HibernateService;
import hibernate.impl.HibernateSession;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("CheckReturnValue")
public abstract class HibernateTestBase {

    protected static JsonObject config;
    protected static HibernateService service;
    protected static HibernateSession session;
    protected static Vertx vertx;

    @BeforeAll
    public static void before(VertxTestContext ctx) {
        vertx = Vertx.vertx();
        config = new JsonObject().put("persistence-unit", "test");
        service = new HibernateService(vertx, config);

        service.start()
                .andThen(service.createSession())
                .doOnError(ctx::failNow)
                .subscribe(s -> {
                    session = s;
                    ctx.completeNow();
                });
    }

    @AfterAll
    public static void after(VertxTestContext ctx) {
        if (service != null) {
            service.stop()
                    .doOnError(ctx::failNow)
                    .subscribe(ctx::completeNow);
        } else {
            ctx.completeNow();
        }
    }

    @BeforeEach
    public void clearDatabase(VertxTestContext ctx) {
        if (!session.isActiveSync()) {
            session = service.createSession().blockingGet();
        }
        session.execute(new HibernateQuery("DELETE FROM Mock"))
                .doOnError(ctx::failNow)
                .subscribe(count -> ctx.completeNow());
    }

}