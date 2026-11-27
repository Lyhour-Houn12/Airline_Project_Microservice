package com.jing.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jing.pricing", "com.jing.common"})
public class PricingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PricingServiceApplication.class, args);
	}

}
