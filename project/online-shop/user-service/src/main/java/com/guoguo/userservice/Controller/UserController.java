package com.guoguo.userservice.Controller;

import com.guoguo.userservice.Entity.User;
import com.guoguo.userservice.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/user")
public class UserController {

	@Autowired
	@Qualifier("userService")
	private UserService userService;
	
	@PostMapping(value = "/add")
	public String addUser(
			@RequestParam(required = true, name = "name") String name,
			@RequestParam(required = false, name = "address") String address,
			@RequestParam(required = false, name = "description") String description) {
		return "";
	};
	
	@GetMapping(value = "/get/{userId}")
	public User getUserInfo(@PathVariable(required = true) int userId) {
		User user = userService.getUserInfo(userId);
//		if (user == null) {
//			return null;
//		}
		return user;
	};

}
