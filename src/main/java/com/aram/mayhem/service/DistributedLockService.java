package com.aram.mayhem.service;

import java.util.function.Supplier;

public interface DistributedLockService {

    boolean tryLock(String lockKey, long ttlMs);

    void unlock(String lockKey);

    <T> T executeWithLock(String lockKey, long ttlMs, Supplier<T> task);

    void executeWithLock(String lockKey, long ttlMs, Runnable task);
}
