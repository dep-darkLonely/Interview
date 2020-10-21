package com.guoguo.userservice.Service;

import com.guoguo.userservice.Entity.User;
import org.springframework.stereotype.Service;

public interface UserService {
	
	User getUserInfo(int id);
}
