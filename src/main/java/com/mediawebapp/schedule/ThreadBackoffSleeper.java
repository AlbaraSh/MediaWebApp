package com.mediawebapp.schedule;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ThreadBackoffSleeper implements BackoffSleeper {

	@Override
	public void sleep(Duration duration) throws InterruptedException {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			return;
		}
		Thread.sleep(duration.toMillis());
	}
}
