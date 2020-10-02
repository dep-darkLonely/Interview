package com.redis.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.redis.demo.Entity.Order;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/order")
public class OrderController {
	
	@Autowired
	@Qualifier("redisson")
	private RedissonClient redisson;
	
	private JsonMapper jsonMapper = new JsonMapper();
	
	@RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
	public boolean updateOrder(@PathVariable(value = "id", required = true) String id) {
		String lockKey = id;
		// 获取lock
		RLock rLock = redisson.getLock(lockKey);
		try {
			rLock.lock(10, TimeUnit.SECONDS);
			// 获取指定对象进行修改
			RBucket<Order> rBucket = redisson.getBucket(lockKey);
			Order order = rBucket.get();
			order.setNumber(order.getNumber() + 1);
			System.out.println(order.getNumber());
		} finally {
			// 释放lock
			rLock.unlock();
		}
		return true;
	}
	
	@RequestMapping(value = "put", method = RequestMethod.POST)
	public String putOrder(@RequestParam(name = "price") double price,
	                        @RequestParam(name = "number", required = true) int number,
							@RequestParam(name = "address") String address) throws JsonProcessingException {
		Order order = new Order();
		order.setId(String.valueOf(UUID.randomUUID()));
		order.setNumber(number);
		order.setPrice(price);
		order.setAddress(address);
		String orderKey = order.getId();
		RBucket<String> rBucket = redisson.getBucket(orderKey);
		String value = jsonMapper.writeValueAsString(order);
		rBucket.set("1");
		String result = String.format("{order_key：%s}", orderKey);
		return result;
	}
}
