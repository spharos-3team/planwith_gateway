package com.planwith.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"app.jwt.secret=test-member-gateway-jwt-secret-at-least-32-bytes"
})
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
