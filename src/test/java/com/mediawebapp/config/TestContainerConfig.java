package com.mediawebapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers Postgres for full-context Spring Boot tests.
 * <p>
 * Uses the pgvector image because Flyway {@code V1} requires the {@code vector} extension.
 * {@link ServiceConnection} wires JDBC (and Flyway) to the container automatically.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

	@Bean
	@ServiceConnection(name = "postgresql")
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer(
				DockerImageName.parse("pgvector/pgvector:pg17")
						.asCompatibleSubstituteFor("postgres"));
	}
}
