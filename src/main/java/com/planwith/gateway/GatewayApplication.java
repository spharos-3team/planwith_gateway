package com.planwith.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.planwith.gateway.config.LocalDotenvLoader;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		LocalDotenvLoader.load("planwith_gateway");
		SpringApplication.run(GatewayApplication.class, args);
	}

}
