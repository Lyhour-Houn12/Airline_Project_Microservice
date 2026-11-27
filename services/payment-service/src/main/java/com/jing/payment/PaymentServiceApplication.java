package com.jing.payment;

import com.jing.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.jing.payment", "com.jing.common"})
@ConfigurationPropertiesScan
@EnableScheduling
@EnableFeignClients(basePackages = "com.jing.payment.client", defaultConfiguration = FeignConfig.class)
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
