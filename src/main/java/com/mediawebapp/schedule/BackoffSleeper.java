package com.mediawebapp.schedule;

import java.time.Duration;

/**
 * Wait strategy for rating-refresh backoff. Production sleeps; tests no-op.
 */
@FunctionalInterface
public interface BackoffSleeper {

	void sleep(Duration duration) throws InterruptedException;
}
