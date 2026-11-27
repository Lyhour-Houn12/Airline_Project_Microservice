package com.jing.seat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jing.seat", "com.jing.common"})
public class SeatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatServiceApplication.class, args);
	}

}
