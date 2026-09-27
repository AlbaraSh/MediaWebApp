package com.mediawebapp.security;

/**
 * Caps signed-in imports so a public demo cannot run up the embeddings bill.
 * 10 per minute and 25 per day per user. In-memory, so it resets on restart
 * and is counted per app instance. Catalog seeding does not pass through this limiter.
 */
public class ImportRateLimiter extends SearchRateLimiter {

	static final int BURST_LIMIT = 10;
	static final int DAILY_LIMIT = 25;

	public ImportRateLimiter() {
		super(BURST_LIMIT, BURST_WINDOW_MS, DAILY_LIMIT, DAILY_WINDOW_MS);
	}
}
