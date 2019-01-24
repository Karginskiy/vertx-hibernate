package hibernate;

import hibernate.base.HibernateTestBase;
import hibernate.mock.Mock;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class HibernatePersistenceTest extends HibernateTestBase {
    private static final String DEFAULT_NAME = "$_name_$";
    private static final String NEW_NAME = "$_new_$";

    @Test
    public void persist(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);
        session.persist(mock)
                .doOnError(ctx::failNow)
                .subscribe(entity -> {
                    ctx.verify(() -> assertNotNull(mock.getId()));
                    ctx.verify(() -> assertEquals(mock.getName(), entity.getName()));
                    ctx.completeNow();
                });
    }

    @Test
    public void merge(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);
        session.persist(mock)
                .flatMap(mock1 -> {
                    mock.setName(NEW_NAME);
                    return session.merge(mock1);
                })
                .doOnError(ctx::failNow)
                .subscribe(entity -> {
                    ctx.verify(() -> {
                        assertNotNull(mock.getId());
                        assertEquals(NEW_NAME, entity.getName());
                        assertEquals(mock.getName(), entity.getName());
                    });
                    ctx.completeNow();
                });
    }

    @Test
    public void remove(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);
        session.persist(mock)
                .flatMap(session::remove)
                .doOnError(ctx::failNow)
                .subscribe(entity -> ctx.completeNow());
    }

    @Test
    public void flushAndRefresh(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);

        session.persist(mock)
                .flatMapCompletable(mock1 -> session.flush())
                .andThen(session.refresh(mock))
                .subscribe(entity -> {
                    ctx.verify(() -> assertNotNull(mock.getId()));
                    ctx.completeNow();
                });
    }
}
