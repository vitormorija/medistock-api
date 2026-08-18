package br.com.medistock.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "medistock.persistencia=memoria")
class ApiApplicationTests {
	@Test
	void contextLoads() {
	}
}
