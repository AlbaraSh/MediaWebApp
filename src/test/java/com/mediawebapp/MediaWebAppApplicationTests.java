package com.mediawebapp;

import com.mediawebapp.config.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
class MediaWebAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
