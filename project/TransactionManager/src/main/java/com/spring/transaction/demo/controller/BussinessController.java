package com.spring.transaction.demo.controller;

import com.spring.transaction.demo.common.BussinessResponseJson;
import com.spring.transaction.demo.common.ResponseJson;
import com.spring.transaction.demo.common.Util;
import com.spring.transaction.demo.domain.Bussiness;
import com.spring.transaction.demo.service.BussinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class BussinessController {
	
	@Autowired
	private MessageSource messageSource;
	
	@Resource
	private BussinessService service;
	
	@PostMapping(value = "add")
	public ResponseJson addBussiness(
			@RequestParam(name = "name", required = true) String name,
			@RequestParam(name = "alias", required = false) String alias,
			@RequestParam(name = "produceDate", required = true) String produceDate,
			@RequestParam(name = "production", required = true) String production,
			@RequestParam(name = "price", required = true) BigDecimal price,
			@RequestParam(name = "stock", required = true) long stock,
			@RequestParam(name = "tags",required = false) String tags
	) throws ParseException {
		BussinessResponseJson response = new BussinessResponseJson();
		Bussiness bussiness = new Bussiness();
		bussiness.setName(name);
		bussiness.setAlias(alias);
		bussiness.setProduction(production);
		bussiness.setPrice(price);
		bussiness.setStock(stock);
		bussiness.setTags(tags);
		bussiness.setProduceDate(Util.String2Milles(produceDate));
		int result = service.add(bussiness);
		if (result > 0) {
			response.setStatus(ResponseJson.STATUS_OK);
		} else {
			response.setStatus(ResponseJson.STATUS_NG);
			response.setMessage(
					messageSource.getMessage(
							"bussiness.insert.fail",
							null,
							LocaleContextHolder.getLocale()
					)
			);
		}
		return response;
	}
	
	
	@DeleteMapping(value = "delete")
	public ResponseJson deleteBussiness(
			@RequestParam(name = "id", required = true) long id
	) {
		BussinessResponseJson response = new BussinessResponseJson();
		Bussiness bussiness = service.get(id);
		if (bussiness == null) {
			response.setStatus(ResponseJson.STATUS_WARN);
			response.setMessage(messageSource.getMessage(
					"bussiness.not.exist",
					null,
					LocaleContextHolder.getLocale()
			));
		}
		int result = service.delete(id);
		if (result > 0) {
			response.setStatus(ResponseJson.STATUS_OK);
		} else {
			response.setStatus(ResponseJson.STATUS_NG);
			response.setMessage(
					messageSource.getMessage(
							"bussiness.delete.fail",
							null,
							LocaleContextHolder.getLocale()
					)
			);
		}
		return response;
	}
	
	@GetMapping(value = "list")
	public ResponseJson queryBussiness(
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "tag", required = false) String tags
	) {
		BussinessResponseJson response = new BussinessResponseJson();
		Bussiness bussiness = new Bussiness();
		bussiness.setName(name);
		bussiness.setTags(tags);
		List<Bussiness> list = service.list(bussiness);
		response.setStatus(ResponseJson.STATUS_OK);
		response.setData(list);
		return  response;
	};
	
	@PostMapping(value = "update")
	public ResponseJson updateBussiness(
			@RequestParam(name = "id", required = true) long id,
			@RequestParam(name = "alias", required = false) String alias,
			@RequestParam(name = "production", required = true) String production,
			@RequestParam(name = "price", required = false) BigDecimal price,
			@RequestParam(name = "tags",required = false) String tags
	) {
		BussinessResponseJson response = new BussinessResponseJson();
		Bussiness bussiness = service.get(id);
		bussiness.setAlias(alias);
		bussiness.setProduction(production);
		bussiness.setPrice(price);
		bussiness.setTags(tags);
		int result = service.update(bussiness);
		response.setStatus(ResponseJson.STATUS_OK);
		if (!(result > 0)) {
			response.setStatus(ResponseJson.STATUS_NG);
			response.setMessage(
					messageSource.getMessage(
							"bussiness.update.fail",
							null,
							LocaleContextHolder.getLocale()
					)
			);
		}
		return response;
	}
}
