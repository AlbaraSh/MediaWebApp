package com.mediawebapp.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps Fly.io {@code DATABASE_URL=postgres://user:pass@host:port/db} to Spring JDBC properties.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String PROPERTY_SOURCE_NAME = "flyDatabaseUrl";

	@Override
	public void postProcessEnvironment(
			ConfigurableEnvironment environment,
			SpringApplication application) {
		String raw = firstNonBlank(
				environment.getProperty("DATABASE_URL"),
				environment.getProperty("database.url"));
		if (raw == null || raw.startsWith("jdbc:")) {
			return;
		}
		if (!(raw.startsWith("postgres://") || raw.startsWith("postgresql://"))) {
			return;
		}
		environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, toJdbc(raw)));
	}

	static Map<String, Object> toJdbc(String raw) {
		URI uri = URI.create(raw.replaceFirst("^postgres(ql)?://", "http://"));
		String userInfo = uri.getUserInfo();
		String user = null;
		String password = null;
		if (userInfo != null && !userInfo.isBlank()) {
			int colon = userInfo.indexOf(':');
			if (colon >= 0) {
				user = decode(userInfo.substring(0, colon));
				password = decode(userInfo.substring(colon + 1));
			} else {
				user = decode(userInfo);
			}
		}
		String path = uri.getPath();
		String database = (path == null || path.isBlank()) ? "" : path.substring(1);
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
				.append(uri.getHost())
				.append(':')
				.append(port)
				.append('/')
				.append(database);
		if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
			jdbc.append('?').append(uri.getQuery());
		} else {
			jdbc.append("?sslmode=require");
		}
		Map<String, Object> props = new HashMap<>();
		props.put("spring.datasource.url", jdbc.toString());
		if (user != null) {
			props.put("spring.datasource.username", user);
		}
		if (password != null) {
			props.put("spring.datasource.password", password);
		}
		return props;
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
