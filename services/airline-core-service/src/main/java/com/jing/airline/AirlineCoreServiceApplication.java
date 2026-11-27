package com.jing.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jing.airline", "com.jing.common"})
public class AirlineCoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirlineCoreServiceApplication.class, args);
	}

}
