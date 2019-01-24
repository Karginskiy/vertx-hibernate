package hibernate;

import hibernate.base.HibernateTestBase;
import hibernate.impl.HibernateSession;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class HibernateSessionTest extends HibernateTestBase {

    @Test
    public void createSession(VertxTestContext ctx) {
        service.createSession()
                .doOnError(ctx::failNow)
                .subscribe(session -> {
                    ctx.verify(() -> {
                        assertNotNull(session);
                        assertNotNull(session.getId());
                    });
                    ctx.completeNow();
                });
    }

    @Test
    public void closeSession(VertxTestContext ctx) {
        session.close()
                .andThen(session.isActive())
                .subscribe(isActive -> {
                    ctx.verify(() -> assertFalse(isActive));
                    ctx.completeNow();
                });
    }

    @Test
    public void showIsSessionActive(VertxTestContext ctx) {
        session.isActive()
                .subscribe(isActive -> {
                    ctx.verify(() -> assertTrue(isActive));
                    ctx.completeNow();
                });
    }

    @Test
    public void clearSession(VertxTestContext ctx) {
        session.clear()
                .doOnError(ctx::failNow)
                .subscribe(ctx::completeNow);
    }
}