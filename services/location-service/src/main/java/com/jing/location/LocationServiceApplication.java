package com.jing.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jing.location", "com.jing.common"})
public class LocationServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(LocationServiceApplication.class, args);
	}
}
