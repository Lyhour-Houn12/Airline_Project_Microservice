package com.jing.flight;

import com.jing.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.jing.flight", "com.jing.common"})
@EnableFeignClients(basePackages = "com.jing.flight.client", defaultConfiguration = FeignConfig.class)
public class FlightOpsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlightOpsServiceApplication.class, args);
	}

}
