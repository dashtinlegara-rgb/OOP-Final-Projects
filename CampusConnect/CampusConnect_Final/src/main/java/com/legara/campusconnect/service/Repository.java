package com.legara.campusconnect.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Repository<T, ID> {
    private final Map<ID, T> items = new LinkedHashMap<>();

    public boolean contains(ID id) {
        return items.containsKey(id);
    }

    public T get(ID id) {
        return items.get(id);
    }

    public void put(ID id, T item) {
        items.put(id, item);
    }

    public void remove(ID id) {
        items.remove(id);
    }

    public List<T> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }

    public void clear() {
        items.clear();
    }

    public int size() {
        return items.size();
    }
}
