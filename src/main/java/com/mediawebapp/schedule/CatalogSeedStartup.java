package com.mediawebapp.schedule;

import com.mediawebapp.service.CatalogSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fills an empty catalog after boot without blocking health checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "catalog.seed.on-startup", havingValue = "true")
public class CatalogSeedStartup implements ApplicationRunner {

	private final CatalogSeedService catalogSeedService;

	@Override
	public void run(ApplicationArguments args) {
		Thread worker = new Thread(catalogSeedService::seedIfNeeded, "catalog-seed");
		worker.setDaemon(true);
		worker.start();
		log.info("Catalog seed scheduled on background thread");
	}
}
