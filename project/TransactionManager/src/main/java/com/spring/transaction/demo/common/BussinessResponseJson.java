package com.spring.transaction.demo.common;

import com.spring.transaction.demo.domain.Bussiness;
import java.util.List;

public class BussinessResponseJson extends ResponseJson {
	
	private List<Bussiness> data;
	
	public List<Bussiness> getData() {
		return data;
	}
	
	public void setData(List<Bussiness> data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return "BussinessResponseJson{" +
				"data=" + data +
				'}';
	}
}
