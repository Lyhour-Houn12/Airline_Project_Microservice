package com.jing.pricing;

import org.springframework.cloud.openfeign.EnableFeignClients;
import com.jing.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableFeignClients(basePackages = {"com.jing.pricing.client", "com.jing.common.airline"}, defaultConfiguration = FeignConfig.class)
@SpringBootApplication(scanBasePackages = {"com.jing.pricing", "com.jing.common"})
public class PricingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PricingServiceApplication.class, args);
	}

}
