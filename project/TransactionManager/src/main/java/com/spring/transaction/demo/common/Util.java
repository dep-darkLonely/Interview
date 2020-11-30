package com.spring.transaction.demo.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Util {
	
	public static List<String> String2List(String args) {
		return Arrays.asList(args.split(","));
	}
	
	/**
	 * 日期字符串 → 时间戳
	 * @param data String 类型 日期字符串
	 * @return 时间戳
	 */
	public static long String2Milles(String data) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(data);
		return date.getTime();
	}
}
