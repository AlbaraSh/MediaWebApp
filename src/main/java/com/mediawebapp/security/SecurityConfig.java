package com.mediawebapp.security;

import com.mediawebapp.config.CatalogAdminProperties;
import com.mediawebapp.config.CorsProperties;
import com.mediawebapp.repository.UserRepository;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stateless JWT security: no HTTP sessions, no CSRF cookie token, BCrypt passwords.
 * <p>
 * Public: register/login, catalog reads, catalog genre names, similar-media discovery,
 * and actuator health. Catalog writes except import are limited to the configured
 * catalog-admin user. Everything else requires a valid Bearer token.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, CatalogAdminProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtService jwtService;
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final JsonAccessDeniedHandler accessDeniedHandler;
	private final UserRepository userRepository;
	private final CatalogAdminProperties catalogAdminProperties;

	@Bean
	public SearchRateLimiter searchRateLimiter() {
		return new SearchRateLimiter();
	}

	@Bean
	public SearchRateLimitFilter searchRateLimitFilter(
			SearchRateLimiter searchRateLimiter,
			JsonMapper jsonMapper) {
		return new SearchRateLimitFilter(searchRateLimiter, jsonMapper);
	}

	@Bean
	public FilterRegistrationBean<SearchRateLimitFilter> searchRateLimitFilterRegistration(
			SearchRateLimitFilter searchRateLimitFilter) {
		FilterRegistrationBean<SearchRateLimitFilter> registration =
				new FilterRegistrationBean<>(searchRateLimitFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtService, authenticationEntryPoint, userRepository);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = corsProperties.allowedOrigins() == null
				? List.of()
				: corsProperties.allowedOrigins();
		config.setAllowedOrigins(origins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			SearchRateLimitFilter searchRateLimitFilter) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/media/**")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/genres")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/recommendations/similar/**")
						.permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**")
						.permitAll()
						.requestMatchers("/api/user-media/**")
						.authenticated()
						.requestMatchers(HttpMethod.POST, "/api/media/import")
						.authenticated()
						.requestMatchers(HttpMethod.POST, "/api/media", "/api/media/**")
						.access(this::isCatalogAdmin)
						.requestMatchers(HttpMethod.PUT, "/api/media/**")
						.access(this::isCatalogAdmin)
						.requestMatchers(HttpMethod.DELETE, "/api/media/**")
						.access(this::isCatalogAdmin)
						.anyRequest()
						.authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(searchRateLimitFilter, JwtAuthenticationFilter.class);

		return http.build();
	}

	private AuthorizationDecision isCatalogAdmin(
			Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		Authentication auth = authentication.get();
		if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
			return new AuthorizationDecision(false);
		}
		return new AuthorizationDecision(user.userId().equals(catalogAdminProperties.userId()));
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Prevents Spring Boot from generating a default in-memory user. Login is
	 * handled by {@code AuthService}, not {@link UserDetailsService}.
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager();
	}
}
