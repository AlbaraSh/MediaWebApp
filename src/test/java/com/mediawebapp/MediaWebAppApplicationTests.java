package com.mediawebapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MediaWebAppApplicationTests {

	/** Smoke test: the Spring application context starts successfully with current config. */
	@Test
	void contextLoads() {
	}

}
