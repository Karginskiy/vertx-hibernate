package hibernate;

import hibernate.impl.HibernateSession;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface IHibernateService {
    /**
     * Call it before using service to init
     */
    Completable start();

    /**
     * Call it after using service to destroy
     */
    Completable stop();

    Single<HibernateSession> createSession();
}
