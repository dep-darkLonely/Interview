package org.example.cn.service;

import org.example.cn.annotation.Autowired;
import org.example.cn.annotation.Service;
import org.example.cn.dao.Dao;
import org.example.cn.entity.User;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private Dao<User> dao;

    @Override
    public User login(String id) {
        User user = dao.login(id);
        if (user == null) {
            user = new User();
            user.setId("2");
            user.setAge(26);
            user.setName("zhangsan");
            user.setAddress("Java");
        }
        return user;
    }
}
