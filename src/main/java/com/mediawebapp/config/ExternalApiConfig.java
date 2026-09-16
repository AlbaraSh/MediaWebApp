package com.mediawebapp.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/**
 * Wires external-provider configuration, a per-adapter {@link RestClient.Builder},
 * and an explicit {@link TransactionTemplate} so import can run HTTP fetches
 * outside a database transaction.
 */
@Configuration
@EnableConfigurationProperties({TmdbProperties.class, RawgProperties.class, JikanProperties.class})
public class ExternalApiConfig {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean(RestClient.Builder.class)
	RestClient.Builder restClientBuilder() {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(CONNECT_TIMEOUT)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return RestClient.builder().requestFactory(requestFactory);
	}

	@Bean
	TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
		return new TransactionTemplate(transactionManager);
	}
}
