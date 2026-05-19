package com.aram.mayhem.scheduler;

import com.aram.mayhem.client.RiotDataDragonClient;
import com.aram.mayhem.dto.SyncResult;
import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.service.CacheWarmupService;
import com.aram.mayhem.service.DataAggregatorService;
import com.aram.mayhem.service.DistributedLockService;
import com.aram.mayhem.service.MultiSourceValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncScheduler {

    private static final String SYNC_LOCK_KEY = "data:sync";
    private static final long LOCK_TTL_MS = 30 * 60 * 1000L;

    private final DataAggregatorService dataAggregatorService;
    private final MultiSourceValidator multiSourceValidator;
    private final DistributedLockService distributedLockService;
    private final RiotDataDragonClient riotDataDragonClient;
    private final CacheWarmupService cacheWarmupService;

    @Value("${sync.warmup-top-n:50}")
    private int warmupTopN;

    @Scheduled(cron = "0 0 0/6 * * ?")
    public SyncResult syncAllData() {
        log.info("[SYNC] scheduled data sync started");
        long startTime = System.currentTimeMillis();

        if (!distributedLockService.tryLock(SYNC_LOCK_KEY, LOCK_TTL_MS)) {
            log.warn("[SYNC] another sync is running, skip this round");
            return SyncResult.fail("Another sync is already running");
        }

        try {
            String version = riotDataDragonClient.fetchLatestVersion();
            if (version == null || version.isBlank()) {
                log.error("[SYNC] failed to fetch latest version from DataDragon");
                return SyncResult.fail("Failed to fetch latest version");
            }
            log.info("[SYNC] using DataDragon version={}", version);

            List<Hero> heroes = dataAggregatorService.aggregateAndSaveHeroData(version);
            List<Augment> augments = dataAggregatorService.aggregateAndSaveAugmentData();

            List<ValidationResult> heroValidations = multiSourceValidator.validateHeroStats(heroes);
            List<ValidationResult> augmentValidations = multiSourceValidator.validateAugmentStats(augments);

            int validationPassed = (int) heroValidations.stream().filter(ValidationResult::isPassed).count()
                    + (int) augmentValidations.stream().filter(ValidationResult::isPassed).count();
            int validationFailed = heroValidations.size() + augmentValidations.size() - validationPassed;

            cacheWarmupService.warmupHeroCache(warmupTopN);
            cacheWarmupService.warmupAugmentCache();
            cacheWarmupService.warmupHeroListCache();
            cacheWarmupService.cleanupStaleCache();
            log.info("[SYNC] cache warmup completed");

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("[SYNC] completed | heroes={} | augments={} | passed={} | failed={} | duration={}ms",
                    heroes.size(), augments.size(), validationPassed, validationFailed, durationMs);

            return SyncResult.success(
                    countInserted(heroes), countUpdated(heroes),
                    countInserted(augments), countUpdated(augments),
                    validationPassed, validationFailed, durationMs);
        } catch (Exception e) {
            log.error("[SYNC] failed with exception", e);
            return SyncResult.fail(e.getMessage());
        } finally {
            distributedLockService.unlock(SYNC_LOCK_KEY);
        }
    }

    private int countInserted(List<?> entities) {
        return (int) entities.stream().filter(e -> {
            if (e instanceof Hero h) return h.getId() == null;
            if (e instanceof Augment a) return a.getId() == null;
            return false;
        }).count();
    }

    private int countUpdated(List<?> entities) {
        return (int) entities.stream().filter(e -> {
            if (e instanceof Hero h) return h.getId() != null;
            if (e instanceof Augment a) return a.getId() != null;
            return false;
        }).count();
    }
}
