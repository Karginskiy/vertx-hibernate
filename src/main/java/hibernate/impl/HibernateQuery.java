package hibernate.impl;

import hibernate.IHibernateQuery;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class HibernateQuery implements IHibernateQuery {
    @NonNull
    private String jpql;
    private Map<String, Object> values = new HashMap<>();

    @Override
    public HibernateQuery put(String key, Object value) {
        if (Objects.isNull(value)) {
            values.remove(key);
        }
        values.put(key, value);
        return this;
    }

    @Override
    public HibernateQuery setJpql(String jpql) {
        this.jpql = jpql;
        return this;
    }
}
