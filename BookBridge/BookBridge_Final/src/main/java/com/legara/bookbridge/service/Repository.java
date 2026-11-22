package com.legara.bookbridge.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.legara.bookbridge.exception.DuplicateEntityException;
import com.legara.bookbridge.exception.EntityNotFoundException;

public class Repository<T, ID> {
    private final Map<ID, T> items = new LinkedHashMap<>();
    private final Function<T, ID> idOf;
    private final String entityName;
    private final String idLabel;

    public Repository(String entityName, String idLabel, Function<T, ID> idOf) {
        this.entityName = entityName;
        this.idLabel = idLabel;
        this.idOf = idOf;
    }

    public void ensureAbsent(T item) throws DuplicateEntityException {
        ID id = idOf.apply(item);
        if (items.containsKey(id)) {
            throw new DuplicateEntityException(entityName + " " + idLabel + " already registered: " + id);
        }
    }

    public void add(T item) throws DuplicateEntityException {
        ensureAbsent(item);
        items.put(idOf.apply(item), item);
    }

    public T require(ID id) throws EntityNotFoundException {
        T item = items.get(id);
        if (item == null) {
            throw new EntityNotFoundException("No " + entityName.toLowerCase() + " with " + idLabel + ": " + id);
        }
        return item;
    }

    public boolean contains(ID id) {
        return items.containsKey(id);
    }

    public List<T> list() {
        return List.copyOf(items.values());
    }

    public Map<ID, T> copyAsMap() {
        return new LinkedHashMap<>(items);
    }

    public void replaceAll(List<T> newItems) {
        items.clear();
        for (T item : newItems) {
            items.put(idOf.apply(item), item);
        }
    }

    public List<T> listWhere(Predicate<T> condition) {
        List<T> result = new ArrayList<>();
        for (T item : items.values()) {
            if (condition.test(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
