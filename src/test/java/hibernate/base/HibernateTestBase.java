package hibernate.base;

import hibernate.HibernateService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class HibernateTestBase {

    protected static JsonObject config;
    protected static HibernateService service;
    protected static Vertx vertx;

    @BeforeAll
    public static void before(VertxTestContext ctx) {
        vertx = Vertx.vertx();
        config = new JsonObject().put("persistence-unit", "test");
        service = new HibernateService(vertx, config);

        Future<Void> future = Future.future();
        future.setHandler(result -> {
            if (result.cause() != null) {
                result.cause().printStackTrace();
            }
            assertTrue(result.succeeded());
            ctx.completeNow();
        });
        service.start(future);
    }

    @AfterAll
    public static void after(VertxTestContext ctx) {
        if (service != null) {
            Future<Void> future = Future.future();
            future.setHandler(result -> {
                assertTrue(result.succeeded());
                ctx.completeNow();
            });
            service.stop(future);
        }
    }
}