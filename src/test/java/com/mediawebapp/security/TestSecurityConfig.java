package com.mediawebapp.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test-only filter chain used by existing {@code @WebMvcTest} slices.
 * <p>
 * Permits every request and does <em>not</em> register {@link JwtAuthenticationFilter},
 * so Media/UserMedia controller tests keep asserting HTTP + validation without
 * sending a Bearer token. Production {@link SecurityConfig} is unchanged.
 * <p>
 * Do not import this into security-behavior tests — those must exercise the
 * real filter chain (401 without a token).
 */
@TestConfiguration
public class TestSecurityConfig {

	@Bean
	public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
}
