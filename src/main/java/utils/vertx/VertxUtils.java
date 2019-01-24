package utils.vertx;


import io.reactivex.Maybe;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;

import java.util.function.Supplier;

public abstract class VertxUtils {

    public static Maybe<Boolean> rxExecuteAndComplete(Vertx vertx, Runnable action) {
        return vertx.rxExecuteBlocking(future -> completeWithCatch(future, action));
    }

    public static <T> Maybe<T> rxExecuteAndSupply(Vertx vertx, Supplier<T> supplier) {
        return vertx.rxExecuteBlocking(future -> supplyWithCatch(future, supplier));
    }

    public static void completeWithCatch(Future<Boolean> future, Runnable tryAction) {
        try {
            tryAction.run();
            future.complete(true); // RxJava2 hates nulls
        } catch (Exception e) {
            future.fail(e);
        }
    }

    public static <T> void supplyWithCatch(Future<T> future, Supplier<T> supplier) {
        try {
            future.complete(supplier.get());
        } catch (Exception e) {
            future.fail(e);
        }
    }
}
