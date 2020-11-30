package com.spring.transaction.demo.service;

import com.spring.transaction.demo.dao.BussinessDao;
import com.spring.transaction.demo.domain.Bussiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("BussinessService")
public class BussinessServiceImpl implements BussinessService {
	
	@Autowired
	public BussinessDao dao;
	
	@Transactional(rollbackFor = Exception.class)
	public int add(Bussiness bussiness) {
		return dao.add(bussiness);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public int delete(long id) {
		return dao.delete(id);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public int update(Bussiness bussiness) {
		return dao.update(bussiness);
	}
	
	public Bussiness get(long id) {
		Bussiness bussiness = dao.get(id);
		return  bussiness;
	}
	
	public List<Bussiness> list(Bussiness bussiness) {
		List<Bussiness> list = dao.list(bussiness);
		return list;
	}
}
