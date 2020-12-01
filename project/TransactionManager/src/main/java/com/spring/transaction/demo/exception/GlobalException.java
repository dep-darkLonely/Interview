package com.spring.transaction.demo.exception;

import com.spring.transaction.demo.common.ResponseJson;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice(
		basePackages = "com.spring.transaction.demo.controller"
)
public class GlobalException {

	@ExceptionHandler(value = Exception.class)
	ResponseJson handleControllerException(HttpServletRequest request, Throwable throwable) {
		ResponseJson response = new ResponseJson();
		String msg = throwable.getLocalizedMessage();
		return response;
	}
}
