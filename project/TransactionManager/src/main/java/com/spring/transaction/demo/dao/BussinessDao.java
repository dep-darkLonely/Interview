package com.spring.transaction.demo.dao;

import com.spring.transaction.demo.domain.Bussiness;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface BussinessDao extends Dao<Bussiness> {
}
