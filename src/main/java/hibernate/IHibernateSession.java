package hibernate;

import hibernate.impl.HibernateQuery;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 *  Wraps methods EntityManager to async RxJava2 results
 */
public interface IHibernateSession {
    /**
     *  Call it after using session to destroy
     */
    Completable close();

    /**
     *  persist(), merge(), remove(), refresh() - return arg
     */

    Completable clear();

    Completable flush();

    <T> Single<T> persist(T model);

    <T> Single<T> merge(T model);

    <T> Single<T> remove(T model);

    <T> Single<T> refresh(T model);

    /**
     *  find(), select() - return empty rx-objects if query result is empty
     */

    <T> Maybe<T> find(Class<T> clazz, Long id);

    <T> Observable<T> select(HibernateQuery jpqlQuery, Class<T> clazz);

    <T> Observable<T> select(String jpqlString, Class<T> clazz);

    /**
     *  Returns affected rows count
     */
    Single<Integer> execute(HibernateQuery jpqlQuery);

    /**
     *  beginTransaction(), commitTransaction() and rollbackTransaction() - manage per-session transaction
     */

    Completable beginTransaction();

    Completable commitTransaction();

    Completable rollbackTransaction();

    /**
     *  Returns true if session hasn't removed from service and EntityManager is open (sync and async relevantly)
     */

    Single<Boolean> isActive();

    boolean isActiveSync();
}
