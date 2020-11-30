package com.spring.transaction.demo.dao;

import com.spring.transaction.demo.domain.Bussiness;

import java.util.List;

public interface Dao<T> {
	
	// 新增
	int add(T t);
	
	// 修改
	int update(T t);
	
	// 删除
	int delete(long id);
	
	// 返回单条实例
	T get(long id);
	
	// 返回多条实例
	List<T> list(T t);
}
