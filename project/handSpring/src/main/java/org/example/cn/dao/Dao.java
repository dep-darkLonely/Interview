package org.example.cn.dao;

import org.example.cn.entity.User;

public interface Dao<T> {

    public void insert(T t);

    public int delete(String id);

    public void update(T t);

    public T get(String id);

    public User login(String id);
}
