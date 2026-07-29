package com.planwith.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
