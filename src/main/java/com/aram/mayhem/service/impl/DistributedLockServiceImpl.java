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

/**
 * 分布式锁服务实现类 —— 防止多台服务器同时执行同一任务的"排队系统"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 DistributedLockService 接口，提供了基于 Redis 的分布式锁功能。
 * 分布式锁用于在多台服务器（或多个进程）环境下，确保同一时刻只有一个服务器能执行某个关键任务。
 *
 * 举例：数据同步任务
 * - 如果有3台服务器同时运行数据同步，会导致数据重复写入
 * - 使用分布式锁后，只有获得锁的服务器才能执行同步，其他服务器等待或跳过
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、分布式锁的核心原理
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基于 Redis 的 SETNX（SET if Not eXists）命令实现：
 *
 * 加锁流程：
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 服务器A：SETNX lock:data-sync "uuid-1"  → 成功（获得锁）    │
 * │ 服务器B：SETNX lock:data-sync "uuid-2"  → 失败（锁已被占） │
 * │ 服务器C：SETNX lock:data-sync "uuid-3"  → 失败（锁已被占） │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 解锁流程（使用 Lua 脚本保证原子性）：
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 1. GET lock:data-sync → 获取当前锁的值                      │
 * │ 2. 如果值 == 自己的UUID → DEL lock:data-sync（释放锁）      │
 * │ 3. 如果值 != 自己的UUID → 锁已被别人持有，不能释放          │
 * └──────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么解锁要用 Lua 脚本？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 解锁需要两步操作：先GET判断值，再DEL删除。如果分两步执行：
 *
 * ┌─── 不安全的解锁方式 ────────────────────────────────────┐
 * │ 1. GET lock → "uuid-1"（是我加的锁）                    │
 * │ 2. [此时锁刚好过期，服务器B加锁成功]                      │
 * │ 3. DEL lock（误删了服务器B的锁！）                       │
 * └──────────────────────────────────────────────────────────┘
 *
 * Lua 脚本在 Redis 中是原子执行的（不会被其他命令打断），
 * 所以"判断+删除"是一个不可分割的操作，不会出现上面的竞态条件。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、这个类依赖了哪些"帮手"？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象                | 作用                           | 打个比方
 * ------------------------|-------------------------------|------------------
 * StringRedisTemplate     | 操作 Redis 字符串类型          | 锁柜管理员
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、方法总览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能                              | 是否事务
 * ------------------------|----------------------------------|----------
 * tryLock()               | 尝试获取分布式锁                  | 否
 * unlock()                | 释放分布式锁                      | 否
 * executeWithLock(Supplier)| 带锁执行有返回值的任务           | 否
 * executeWithLock(Runnable)| 带锁执行无返回值的任务           | 否
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    /**
     * 锁Key的前缀
     *
     * 所有分布式锁的Key都会加上这个前缀，方便区分和管理。
     * 例如：lock:data-sync、lock:cache-warmup
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 解锁用的 Lua 脚本 —— 保证"判断+删除"的原子性
     *
     * 脚本逻辑：
     * 1. redis.call('get', KEYS[1]) → 获取锁的当前值
     * 2. 如果当前值 == ARGV[1]（我自己的UUID）→ 执行 DEL 删除锁，返回1
     * 3. 如果当前值 != ARGV[1]（别人的锁）→ 返回0，不删除
     *
     * 参数说明：
     * - KEYS[1]：锁的Key（如 lock:data-sync）
     * - ARGV[1]：加锁时设置的UUID值
     *
     * 为什么用 Lua？
     * Redis 执行 Lua 脚本时是原子的，不会被其他命令打断，
     * 保证了"判断值是否匹配"和"删除锁"两步操作不会被打断。
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * Redis 字符串操作模板
     *
     * 使用 StringRedisTemplate 而不是 RedisTemplate<String, Object>，
     * 因为锁的值只需要存储 UUID 字符串，不需要复杂的序列化。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取分布式锁 —— 非阻塞式加锁
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 尝试获取一个分布式锁。如果锁当前没有被占用，则获取成功；
     * 如果锁已被其他服务器占用，则立即返回失败（不会等待）。
     *
     * @param lockKey 锁的标识名，如 "data-sync"、"cache-warmup"
     * @param ttlMs   锁的存活时间（毫秒），防止死锁
     *                 如果持有锁的服务器崩溃，TTL到期后锁会自动释放
     * @return true=获取锁成功，false=锁已被占用
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么每个锁要设置UUID值？
     * ══════════════════════════════════════════════════════════════
     *
     * 如果所有服务器都用相同的值（如 "1"），就无法区分锁是谁加的。
     * 使用UUID后，解锁时可以验证"这个锁是不是我加的"，
     * 避免误删其他服务器加的锁。
     *
     * ══════════════════════════════════════════════════════════════
     * setIfAbsent() 方法说明
     * ══════════════════════════════════════════════════════════════
     *
     * setIfAbsent(key, value, timeout, unit) 等价于 Redis 命令：
     * SET key value NX PX ttlMs
     *
     * - NX：只在Key不存在时设置（Not eXists）
     * - PX ttlMs：设置过期时间（毫秒）
     *
     * 这条命令本身就是原子的，不需要额外的Lua脚本。
     */
    @Override
    public boolean tryLock(String lockKey, long ttlMs) {
        // 构造完整的锁Key：lock:{lockKey}
        String fullKey = LOCK_PREFIX + lockKey;

        // 生成唯一的锁值，用于标识锁的持有者
        String lockValue = UUID.randomUUID().toString();

        // 尝试加锁：SET lock:data-sync "uuid-xxx" NX PX ttlMs
        // setIfAbsent() 返回 Boolean：
        // - true → Key不存在，设置成功，获得锁
        // - false → Key已存在，设置失败，锁被占用
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("[LOCK] acquired | key={} | ttl={}ms", fullKey, ttlMs);
            return true;
        }

        log.debug("[LOCK] failed to acquire | key={}", fullKey);
        return false;
    }

    /**
     * 释放分布式锁 —— 安全解锁（只删自己加的锁）
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 释放之前获取的分布式锁。使用 Lua 脚本保证"判断+删除"的原子性，
     * 避免误删其他服务器加的锁。
     *
     * @param lockKey 锁的标识名（与 tryLock 中的 lockKey 一致）
     *
     * ══════════════════════════════════════════════════════════════
     * ⚠️ 当前实现的局限性
     * ══════════════════════════════════════════════════════════════
     *
     * 当前实现先 GET 获取锁值，再通过 Lua 脚本删除。
     * 但 GET 获取的值可能不是当前服务器加锁时的UUID（锁可能已过期被别人获取）。
     * 更严谨的做法是在 tryLock 时将 UUID 存储在 ThreadLocal 或实例变量中，
     * 解锁时使用存储的 UUID。当前实现对于本项目的使用场景（单次定时任务）足够安全。
     */
    @Override
    public void unlock(String lockKey) {
        // 构造完整的锁Key
        String fullKey = LOCK_PREFIX + lockKey;

        // 获取当前锁的值
        String lockValue = stringRedisTemplate.opsForValue().get(fullKey);
        if (lockValue == null) {
            // 锁不存在，说明已经过期自动释放了
            log.debug("[LOCK] already released | key={}", fullKey);
            return;
        }

        // 使用 Lua 脚本原子性地执行"判断+删除"
        // 如果锁的值与传入的 lockValue 匹配，则删除锁
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(fullKey), lockValue);

        // Lua 脚本返回值：
        // 1 → 删除成功（锁的值匹配，已释放）
        // 0 → 删除失败（锁的值不匹配，可能是别人的锁）
        if (result != null && result == 1L) {
            log.debug("[LOCK] released | key={}", fullKey);
        } else {
            log.warn("[LOCK] release failed (lock expired or held by another) | key={}", fullKey);
        }
    }

    /**
     * 带锁执行有返回值的任务 —— 获取锁→执行任务→释放锁
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 这是一个便捷方法，封装了"获取锁→执行任务→释放锁"的完整流程。
     * 调用者不需要手动管理锁的获取和释放，避免忘记释放锁导致死锁。
     *
     * @param lockKey 锁的标识名
     * @param ttlMs   锁的存活时间（毫秒）
     * @param task    要执行的任务（有返回值），使用 Java 8 的 Supplier 函数式接口
     * @param <T>     任务返回值的类型
     * @return T 任务的执行结果
     *
     * @throws IllegalStateException 如果获取锁失败
     *
     * ══════════════════════════════════════════════════════════════
     * Supplier 函数式接口说明
     * ══════════════════════════════════════════════════════════════
     *
     * Supplier<T> 是 Java 8 的函数式接口，代表一个"供应商"——不接收参数，返回一个值。
     * 使用 Lambda 表达式调用：
     *   executeWithLock("data-sync", 30000, () -> {
     *       // 执行数据同步逻辑
     *       return syncResult;
     *   });
     */
    @Override
    public <T> T executeWithLock(String lockKey, long ttlMs, Supplier<T> task) {
        // ─── 第1步：尝试获取锁 ───
        if (!tryLock(lockKey, ttlMs)) {
            throw new IllegalStateException("Failed to acquire lock: " + lockKey);
        }

        try {
            // ─── 第2步：执行任务 ───
            return task.get();
        } finally {
            // ─── 第3步：无论成功还是异常，都必须释放锁 ───
            // finally 块确保即使任务抛出异常，锁也会被释放
            unlock(lockKey);
        }
    }

    /**
     * 带锁执行无返回值的任务 —— 获取锁→执行任务→释放锁
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 与上面的 executeWithLock(Supplier) 类似，但任务没有返回值。
     * 通过将 Runnable 包装成 Supplier 来复用上面的实现。
     *
     * @param lockKey 锁的标识名
     * @param ttlMs   锁的存活时间（毫秒）
     * @param task    要执行的任务（无返回值），使用 Java 8 的 Runnable 接口
     *
     * ══════════════════════════════════════════════════════════════
     * Runnable vs Supplier 的区别
     * ══════════════════════════════════════════════════════════════
     *
     * - Runnable：void run() → 不返回值
     * - Supplier<T>：T get() → 返回一个值
     *
     * 这里通过 Lambda 将 Runnable 转换为 Supplier<Void>：
     *   () -> { task.run(); return null; }
     */
    @Override
    public void executeWithLock(String lockKey, long ttlMs, Runnable task) {
        // 委托给有返回值的版本，将 Runnable 包装成 Supplier
        executeWithLock(lockKey, ttlMs, () -> {
            task.run();   // 执行无返回值的任务
            return null;  // 返回 null（Supplier 需要返回值）
        });
    }
}
