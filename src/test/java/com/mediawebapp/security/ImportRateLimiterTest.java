package com.mediawebapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImportRateLimiterTest {

	@Test
	void allowsDailyQuotaThenRejects() {
		ImportRateLimiter limiter = new ImportRateLimiter();
		for (int i = 0; i < ImportRateLimiter.DAILY_LIMIT; i++) {
			assertThat(limiter.tryAcquire("user:1")).isTrue();
		}
		assertThat(limiter.tryAcquire("user:1")).isFalse();
		assertThat(limiter.tryAcquire("user:2")).isTrue();
	}
}
