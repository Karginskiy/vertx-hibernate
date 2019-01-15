package hibernate;

import hibernate.base.HibernateTestBase;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class SessionTest extends HibernateTestBase {

    @Test
    public void createSession(VertxTestContext ctx) {
        service.createSession(id -> {
            ctx.verify(() -> assertNotNull(id.result()));
            ctx.completeNow();
        });
    }

    @Test
    public void closeSession(VertxTestContext ctx) {
        service.createSession(id -> {
            String idResult = id.result();
            service.closeSession(idResult, h -> {
                ctx.verify(() -> assertFalse(service.isSessionActive(idResult)));
                ctx.completeNow();
            });
        });
    }

    @Test
    public void showIsSessionActive(VertxTestContext ctx) {
        service.createSession(id -> {
            ctx.verify(() -> assertTrue(service.isSessionActive(id.result())));
            ctx.completeNow();
        });
    }
}