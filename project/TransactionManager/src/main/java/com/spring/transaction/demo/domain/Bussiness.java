package com.spring.transaction.demo.domain;

import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Repository
public class Bussiness implements Serializable {
	
	private static final long serialVersionUID = -5359720158272935084L;
	// 商品id
	@NotNull
	private long id;
	
	// 名称
	@NotEmpty
	private String name;
	
	// 别名
	private String alias;
	
	// 生产日期
	@NotEmpty
	private long produceDate;
	
	// 产地
	@NotEmpty
	private String production;
	
	// 价格
	private BigDecimal price;
	
	// 库存
	private long stock;
	
	// 标签
	private String tags;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public long getProduceDate() {
		return produceDate;
	}
	
	public void setProduceDate(long produceDate) {
		this.produceDate = produceDate;
	}
	
	public String getProduction() {
		return production;
	}
	
	public void setProduction(String production) {
		this.production = production;
	}
	
	public BigDecimal getPrice() {
		return price;
	}
	
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	public long getStock() {
		return stock;
	}
	
	public void setStock(long stock) {
		this.stock = stock;
	}
	
	public String getTags() {
		return tags;
	}
	
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	@Override
	public String toString() {
		return "Bussiness{" +
				"id=" + id +
				", name='" + name + '\'' +
				", alias='" + alias + '\'' +
				", produceDate=" + produceDate +
				", production='" + production + '\'' +
				", price='" + price + '\'' +
				", stock=" + stock +
				'}';
	}
}
