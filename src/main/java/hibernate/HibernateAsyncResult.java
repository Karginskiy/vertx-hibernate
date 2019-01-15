package hibernate;

import io.vertx.core.AsyncResult;

class HibernateAsyncResult<T> implements AsyncResult<T> {
    private final Throwable cause;
    private final T result;

    public HibernateAsyncResult(String cause) {
        this(new RuntimeException(cause), null);
    }

    public HibernateAsyncResult(Throwable cause) {
        this(cause, null);
    }

    public HibernateAsyncResult(Throwable cause, T result) {
        this.cause = cause;
        this.result = result;
    }

    public static <T> HibernateAsyncResult<T> of(AsyncResult<Object> res, Class<T> clazz) {
        return res.succeeded()
                ? new HibernateAsyncResult<>(null, clazz == Void.class ? null : (T) res.result())
                : new HibernateAsyncResult<>(res.cause(), null);
    }

    public static <T> HibernateAsyncResult<T> of(AsyncResult<Object> res) {
        return res.succeeded()
                ? new HibernateAsyncResult<>(null, (T) res.result())
                : new HibernateAsyncResult<>(res.cause(), null);
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean succeeded() {
        return cause == null;
    }

    @Override
    public boolean failed() {
        return cause != null;
    }
}
