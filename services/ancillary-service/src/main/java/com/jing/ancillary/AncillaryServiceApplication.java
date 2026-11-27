package com.jing.ancillary;

import com.jing.ancillary.storage.CloudinaryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.jing.ancillary", "com.jing.common"})
@EnableConfigurationProperties(CloudinaryProperties.class)
public class AncillaryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AncillaryServiceApplication.class, args);
	}

}
