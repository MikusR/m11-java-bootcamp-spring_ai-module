package com.accenture.springai_bootcamp_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringaiBootcampDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringaiBootcampDemoApplication.class, args);
	}

}
