package com.guoguo.integrationservice.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EchoController {


	@GetMapping(value = "/echo/{string}")
	public String echo(@PathVariable(required = true) String string) {
		return "Hello Nacos Discovery" + string;
	}
}
