package hibernate;

import hibernate.base.HibernateTestBase;
import hibernate.mock.Mock;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.EntityTransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(VertxExtension.class)
public class WrapperTest extends HibernateTestBase {

    private static final String DEFAULT_NAME = "$_NAME_$";

    @Test
    public void wrapAndSave(VertxTestContext ctx) {
        final Mock beforeMock = new Mock(DEFAULT_NAME);
        service.withEntityManager(
                firstEm -> {
                    EntityTransaction tx = firstEm.getTransaction();
                    tx.begin();
                    firstEm.persist(beforeMock);
                    tx.commit();
                    return beforeMock;
                }, firstResult -> {
                    if (firstResult.cause() != null) {
                        firstResult.cause().printStackTrace();
                    }
                    ctx.verify(() -> assertTrue(firstResult.succeeded()));
                    service.withEntityManager(
                            secondEm -> secondEm.find(Mock.class, beforeMock.getId()),
                            secondResult -> {
                                ctx.verify(() -> assertTrue(secondResult.succeeded()));
                                ctx.verify(() -> assertEquals(secondResult.result(), beforeMock));
                                ctx.completeNow();
                            });
                });
    }

    @Test
    public void wrapAndSaveWithinTransaction(VertxTestContext ctx) {
        final Mock beforeMock = new Mock(DEFAULT_NAME);
        service.withinTransaction(
                firstEm -> {
                    firstEm.persist(beforeMock);
                    return beforeMock;
                }, firstResult -> {
                    if (firstResult.cause() != null) {
                        firstResult.cause().printStackTrace();
                    }
                    ctx.verify(() -> assertTrue(firstResult.succeeded()));
                    service.withinTransaction(
                            secondEm -> secondEm.find(Mock.class, beforeMock.getId()),
                            secondResult -> {
                                ctx.verify(() -> assertTrue(secondResult.succeeded()));
                                ctx.verify(() -> assertEquals(secondResult.result(), beforeMock));
                                ctx.completeNow();
                            });
                });
    }

    @Test
    public void wrapSessionAndSave(VertxTestContext ctx) {
        final Mock beforeMock = new Mock(DEFAULT_NAME);
        service.createSession(id -> {
            String idResult = id.result();
            ctx.verify(() -> assertNotNull(id.result()));
            service.withSession(
                    id.result(),
                    firstEm -> {
                        firstEm.persist(beforeMock);
                        return beforeMock;
                    }, firstResult -> {
                        if (firstResult.cause() != null) {
                            firstResult.cause().printStackTrace();
                        }
                        ctx.verify(() -> assertTrue(firstResult.succeeded()));
                        service.withSession(
                                idResult,
                                secondEm -> secondEm.find(Mock.class, beforeMock.getId()),
                                secondResult -> {
                                    ctx.verify(() -> assertTrue(secondResult.succeeded()));
                                    ctx.verify(() -> assertEquals(beforeMock, secondResult.result()));
                                    ctx.completeNow();
                                });
                    });
        });
    }
}