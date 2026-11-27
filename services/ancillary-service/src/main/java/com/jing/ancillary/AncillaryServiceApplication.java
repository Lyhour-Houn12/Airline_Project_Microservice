package com.jing.ancillary;

import org.springframework.cloud.openfeign.EnableFeignClients;
import com.jing.common.feign.FeignConfig;
import com.jing.ancillary.storage.CloudinaryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableFeignClients(basePackages = {"com.jing.ancillary.client", "com.jing.common.airline"}, defaultConfiguration = FeignConfig.class)
@SpringBootApplication(scanBasePackages = {"com.jing.ancillary", "com.jing.common"})
@EnableConfigurationProperties(CloudinaryProperties.class)
public class AncillaryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AncillaryServiceApplication.class, args);
	}

}
