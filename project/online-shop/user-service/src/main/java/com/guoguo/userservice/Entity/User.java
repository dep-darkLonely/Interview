package com.guoguo.userservice.Entity;

import org.springframework.stereotype.Component;

@Component
public class User {

	private int id;
	private String name;
	private String address;
	private String description;
	
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return "User{" +
				"id=" + id +
				", name='" + name + '\'' +
				", address='" + address + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}
