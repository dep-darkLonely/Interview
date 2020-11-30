package com.spring.transaction.demo.service;

import com.spring.transaction.demo.domain.Bussiness;

import java.util.List;

public interface BussinessService {

	int add(Bussiness bussiness);
	int delete(long id);
	int update(Bussiness bussiness);
	Bussiness get(long id);
	 List<Bussiness> list(Bussiness bussiness);
}
