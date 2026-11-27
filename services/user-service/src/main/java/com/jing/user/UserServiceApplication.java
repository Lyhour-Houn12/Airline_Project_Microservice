package com.jing.user;

import com.jing.user.config.JwtProperties;
import com.jing.user.config.LoginProtectionProperties;
import com.jing.user.config.OtpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.jing.user", "com.jing.common"})
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
