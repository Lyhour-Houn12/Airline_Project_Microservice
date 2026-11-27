package com.jing.booking;

import com.jing.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.jing.booking", "com.jing.common"})
@EnableFeignClients(basePackages = "com.jing.booking.client", defaultConfiguration = FeignConfig.class)
@EnableScheduling            // BookingExpiryJob
@ConfigurationPropertiesScan
public class BookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}

}
