package hibernate;

import hibernate.impl.HibernateQuery;

/**
 *  Wrapper for JPQL query string and arguments
 */
public interface IHibernateQuery {
    /**
     *  Call it to set arg (by example arg "id" from query "SELECT m FROM Mock m WHERE m.id = :id")
     *  To remove arg, set null as value
     */
    HibernateQuery put(String key, Object value);

    HibernateQuery setJpql(String jpql);

    String getJpql();
}
