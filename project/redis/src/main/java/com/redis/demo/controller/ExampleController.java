package com.redis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.Request;

import java.util.List;


@RestController
@RequestMapping(value = "/example")
class ExampleController {

    @Autowired
    private RedisTemplate<String, String> template;

    @RequestMapping(value = "/set", method = RequestMethod.GET)
    public String setRedisObject(@RequestParam String userId,
                                 @RequestParam String url) {
        ListOperations<String, String> listOperations = template.opsForList();
        listOperations.leftPush(userId, url);
        return "success";
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public List<String> getRedisValueByKey(@RequestParam String userId) {
        ListOperations<String, String> listOperations = template.opsForList();
        Long length = listOperations.size(userId);
        List<String> result = listOperations.range(userId, 0, length);
        return result;
    }


    @RequestMapping(value = "/setString", method = RequestMethod.GET)
    public String setRedisString(@RequestParam String key,
                                 @RequestParam String value) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        valueOperations.set(key,value);
        return "success";
    }

    @RequestMapping(value = "/getString", method = RequestMethod.GET)
    public String getRedisString(@RequestParam String key) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        return valueOperations.get(key);
    }
}
