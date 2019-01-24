package hibernate;

import hibernate.base.HibernateTestBase;
import hibernate.impl.HibernateQuery;
import hibernate.mock.Mock;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class HibernateQueryTest extends HibernateTestBase {
    private static final String DEFAULT_NAME = "$_name_$";
    private static final String NEW_NAME = "$_new_$";
    private static final Long MOCK_ID = 99999L;

    @Test
    public void findExists(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);
        session.persist(mock)
                .flatMapMaybe(m -> session.find(Mock.class, m.getId()))
                .doOnError(ctx::failNow)
                .subscribe(result -> {
                    ctx.verify(() -> {
                        assertNotNull(result);
                        assertEquals(mock.getId(), result.getId());
                    });
                    ctx.completeNow();
                });
    }

    @Test
    public void notFindMissing(VertxTestContext ctx) {
        Mock mock = new Mock(MOCK_ID, DEFAULT_NAME);
        session.find(Mock.class, mock.getId())
                .doOnError(ctx::failNow)
                .isEmpty()
                .subscribe(isEmpty -> {
                    ctx.verify(() -> assertTrue(isEmpty));
                    ctx.completeNow();
                });
    }

    @Test
    public void selectSingle(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);
        HibernateQuery query = new HibernateQuery("SELECT m FROM Mock m WHERE m.name = :name").put("name", DEFAULT_NAME);

        session.persist(mock)
                .flatMapCompletable(m -> session.flush())
                .andThen(Observable.defer(() -> session.select(query, Mock.class)))
                .count()
                .doOnError(ctx::failNow)
                .subscribe(count -> {
                    ctx.verify(() -> assertEquals(new Long(1), count));
                    ctx.completeNow();
                });
    }

    @Test
    public void selectMultiple(VertxTestContext ctx) {
        Mock mock1 = new Mock(DEFAULT_NAME);
        Mock mock2 = new Mock(DEFAULT_NAME);
        HibernateQuery query = new HibernateQuery("SELECT m FROM Mock m WHERE m.name = :name").put("name", DEFAULT_NAME);

        session.persist(mock1)
                .flatMap(mock -> session.persist(mock2))
                .flatMapCompletable(m -> session.flush())
                .andThen(Observable.defer(() -> session.select(query, Mock.class)))
                .count()
                .doOnError(ctx::failNow)
                .subscribe(count -> {
                    ctx.verify(() -> assertEquals(new Long(2), count));
                    ctx.completeNow();
                });
    }


    @Test
    public void executeUpdate(VertxTestContext ctx) {
        Mock mock = new Mock(DEFAULT_NAME);
        HibernateQuery query = new HibernateQuery("UPDATE Mock m SET m.name = :new_name WHERE m.name = :name")
                .put("new_name", NEW_NAME)
                .put("name", DEFAULT_NAME);

        session.persist(mock)
                .flatMapCompletable(m -> session.flush())
                .andThen(Single.defer(() -> session.execute(query)))
                .flatMap(rows -> session.refresh(mock))
                .subscribe(entity -> {
                    ctx.verify(() -> assertEquals(NEW_NAME, entity.getName()));
                    ctx.completeNow();
                });
    }
}
