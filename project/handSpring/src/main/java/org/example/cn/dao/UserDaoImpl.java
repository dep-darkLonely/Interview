package org.example.cn.dao;

import org.example.cn.annotation.Repository;
import org.example.cn.entity.User;

@Repository
public class UserDaoImpl implements Dao<User> {

    @Override
    public void insert(User user) {

    }

    @Override
    public int delete(String id) {
        return 0;
    }

    @Override
    public void update(User user) {

    }

    @Override
    public User get(String id) {
        return null;
    }

    @Override
    public User login(String id) {
        User user = new User();
        user.setId(id);
        user.setAge(25);
        user.setAddress("C#");
        user.setName("wangwu");
        return user;
    }
}
