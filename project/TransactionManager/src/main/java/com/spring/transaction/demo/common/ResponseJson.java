package com.spring.transaction.demo.common;

public class ResponseJson {

	// 正常
	public static final int STATUS_OK = 0;
	
	// 异常
	public static final int STATUS_NG = 1;
	
	// 输入错误
	public static final int STATUS_INPUT_ERROR = 2;
	
	// 警告
	public static final int STATUS_WARN = 3;
	
	// 执行状态
	private int status;
	
	// message 信息
	private String message;
	
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "ResponseJson{" +
				"status=" + status +
				", message='" + message + '\'' +
				'}';
	}
}
