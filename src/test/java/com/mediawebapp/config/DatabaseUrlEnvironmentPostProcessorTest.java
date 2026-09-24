package com.mediawebapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

	@Test
	void mapsFlyPostgresUrlToJdbc() {
		Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toJdbc(
				"postgres://myshelf:p%40ss@myshelf-db.flycast:5432/myshelf?sslmode=require");

		assertThat(props.get("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://myshelf-db.flycast:5432/myshelf?sslmode=require");
		assertThat(props.get("spring.datasource.username")).isEqualTo("myshelf");
		assertThat(props.get("spring.datasource.password")).isEqualTo("p@ss");
	}

	@Test
	void addsSslWhenQueryMissing() {
		Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toJdbc(
				"postgres://u:p@db.internal:5432/app");

		assertThat(props.get("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://db.internal:5432/app?sslmode=require");
	}
}
