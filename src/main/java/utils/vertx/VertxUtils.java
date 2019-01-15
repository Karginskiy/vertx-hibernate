package utils.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class VertxUtils {
    public static void completeWithCatch(Future future, Runnable tryAction) {
        try {
            tryAction.run();
            future.complete();
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

    public static <T> void resolveFuture(Future<T> future, AsyncResult result, Supplier<T> supplier) {
        if (result.succeeded()) {
            future.complete(supplier.get());
        } else {
            future.fail(result.cause());
        }
    }

    public static <T> void resolveFutureByOptional(Future<Optional<T>> future, AsyncResult result, Supplier<T> supplier) {
        if (result.succeeded()) {
            future.complete(Optional.ofNullable(supplier.get()));
        } else {
            future.fail(result.cause());
        }
    }

    public static void resolveAsyncResult(AsyncResult result, Future future, Runnable succeededAction, Runnable failedAction) {
        if (result.succeeded()) {
            succeededAction.run();
            future.complete();
        } else {
            failedAction.run();
            future.fail(result.cause());
        }
    }
}
