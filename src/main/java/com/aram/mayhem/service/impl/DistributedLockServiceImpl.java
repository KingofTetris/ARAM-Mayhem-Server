package com.aram.mayhem.service.impl;

import com.aram.mayhem.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryLock(String lockKey, long ttlMs) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("[LOCK] acquired | key={} | ttl={}ms", fullKey, ttlMs);
            return true;
        }

        log.debug("[LOCK] failed to acquire | key={}", fullKey);
        return false;
    }

    @Override
    public void unlock(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = stringRedisTemplate.opsForValue().get(fullKey);
        if (lockValue == null) {
            log.debug("[LOCK] already released | key={}", fullKey);
            return;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(fullKey), lockValue);

        if (result != null && result == 1L) {
            log.debug("[LOCK] released | key={}", fullKey);
        } else {
            log.warn("[LOCK] release failed (lock expired or held by another) | key={}", fullKey);
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, long ttlMs, Supplier<T> task) {
        if (!tryLock(lockKey, ttlMs)) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }
        try {
            return task.get();
        } finally {
            unlock(lockKey);
        }
    }

    @Override
    public void executeWithLock(String lockKey, long ttlMs, Runnable task) {
        executeWithLock(lockKey, ttlMs, () -> {
            task.run();
            return null;
        });
    }
}
