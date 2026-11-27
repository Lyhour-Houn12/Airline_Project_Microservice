package com.jing.seat;

import org.springframework.cloud.openfeign.EnableFeignClients;
import com.jing.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableFeignClients(basePackages = {"com.jing.seat.client", "com.jing.common.airline"}, defaultConfiguration = FeignConfig.class)
@SpringBootApplication(scanBasePackages = {"com.jing.seat", "com.jing.common"})
public class SeatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatServiceApplication.class, args);
	}

}
