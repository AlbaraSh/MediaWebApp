package com.mediawebapp.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding windows for web search: 20 per minute and 300 per day per key
 * (authenticated user id or client IP). Resets on process restart; per-instance.
 */
public class SearchRateLimiter {

	static final int BURST_LIMIT = 20;
	static final long BURST_WINDOW_MS = 60_000L;
	static final int DAILY_LIMIT = 300;
	static final long DAILY_WINDOW_MS = 86_400_000L;

	private final ConcurrentHashMap<String, Window> burst = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Window> daily = new ConcurrentHashMap<>();

	public boolean tryAcquire(String key) {
		long now = System.currentTimeMillis();
		return allow(burst, key, now, BURST_LIMIT, BURST_WINDOW_MS)
				&& allow(daily, key, now, DAILY_LIMIT, DAILY_WINDOW_MS);
	}

	private static boolean allow(
			ConcurrentHashMap<String, Window> store,
			String key,
			long now,
			int max,
			long windowMs) {
		while (true) {
			Window current = store.get(key);
			if (current == null || now - current.startMs >= windowMs) {
				Window fresh = new Window(now, 1);
				if (current == null) {
					if (store.putIfAbsent(key, fresh) == null) {
						return true;
					}
				} else if (store.replace(key, current, fresh)) {
					return true;
				}
				continue;
			}
			if (current.count >= max) {
				return false;
			}
			Window next = new Window(current.startMs, current.count + 1);
			if (store.replace(key, current, next)) {
				return true;
			}
		}
	}

	private record Window(long startMs, int count) {
	}
}
