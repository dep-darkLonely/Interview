package com.guoguo.userservice.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ConsumerController {
	
	@Autowired
	private LoadBalancerClient loadBalancerClient;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${integration.service.name}")
	private String applicationName;


	@GetMapping("/echo/app-name")
	public String echoApplicationName() {
		ServiceInstance serviceInstance = loadBalancerClient.choose(applicationName);
		String path = String.format("http://%s:%s/echo/%s", serviceInstance.getHost(), serviceInstance.getPort(), applicationName);
		System.out.println("request path:" + path);
		return restTemplate.getForObject(path, String.class);
	}
}
