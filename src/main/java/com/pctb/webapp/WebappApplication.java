package com.pctb.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebappApplication {

	public static void main(String[] args) {
		System.out.println("DB_URL = " + System.getenv("DB_URL"));
		System.out.println("SPRING_DATASOURCE_URL = " + System.getenv("SPRING_DATASOURCE_URL"));

		SpringApplication.run(WebappApplication.class, args);
	}

}
