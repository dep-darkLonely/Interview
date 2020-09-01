package org.example.cn.controller;

import org.example.cn.annotation.*;
import org.example.cn.entity.User;
import org.example.cn.service.UserService;

@RestController
public class UserController {

    @Autowired
    private UserService userService;


    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public User login(@RequestParam(name="id", required = true) String id) {
        User user = userService.login(id);
        return user;
    }
}
