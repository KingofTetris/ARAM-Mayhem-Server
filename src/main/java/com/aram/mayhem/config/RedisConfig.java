package com.aram.mayhem.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置类
 *
 * Redis 是一个高性能的内存数据库，我们用它来做缓存，把频繁访问的数据存在内存中，
 * 这样就不用每次都去 MySQL 数据库查询，大大提升响应速度。
 *
 * 就像你把常用的电话号码存在手机通讯录里，而不是每次都去翻电话本。
 *
 * 本类配置了三个核心组件：
 * 1. RedisTemplate —— 通用的 Redis 操作工具，可以存取任意 Java 对象（自动序列化为 JSON）
 * 2. StringRedisTemplate —— 专用于存取字符串的工具（用于分布式锁等场景）
 * 3. CacheManager —— Spring Cache 的缓存管理器，配合 @Cacheable 注解使用
 *
 * 缓存过期时间（TTL）设计思路：
 * - 英雄详情：1 小时（英雄数据变化不频繁）
 * - 英雄列表：30 分钟（列表数据相对稳定）
 * - 符文列表/详情：30 分钟
 * - 公告列表/最新：10 分钟（公告更新频率较高，需要较短的过期时间）
 * - 默认：30 分钟（兜底策略）
 *
 * 序列化策略：
 * - Key（键）：使用 String 序列化，方便在 Redis 客户端中直接查看
 * - Value（值）：使用 JSON 序列化，保留对象类型信息，反序列化时能还原为正确的类型
 *
 * 关联类：
 * - HeroServiceImpl：使用 @Cacheable 缓存英雄数据
 * - AugmentServiceImpl：使用 @Cacheable 缓存符文数据
 * - DistributedLockServiceImpl：使用 StringRedisTemplate 实现分布式锁
 */
@Configuration // 声明这是一个 Spring 配置类
@EnableCaching // 启用 Spring Cache 注解支持（@Cacheable、@CacheEvict 等）
public class RedisConfig {

    // 默认缓存过期时间：30 分钟
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    // 英雄详情缓存过期时间：1 小时（英雄数据变化不频繁，可以缓存更久）
    private static final Duration HERO_DETAIL_TTL = Duration.ofHours(1);
    // 符文列表缓存过期时间：30 分钟
    private static final Duration AUGMENT_LIST_TTL = Duration.ofMinutes(30);
    // 公告缓存过期时间：10 分钟（公告更新频率较高，过期时间要短一些）
    private static final Duration BULLETIN_TTL = Duration.ofMinutes(10);

    /**
     * 创建 RedisTemplate Bean —— 通用的 Redis 操作工具
     *
     * RedisTemplate 是 Spring Data Redis 提供的核心工具类，用于和 Redis 服务器交互。
     * 它就像一个"远程遥控器"，可以往 Redis 里存数据、取数据、删数据。
     *
     * 为什么需要自定义 RedisTemplate？
     * Spring Boot 默认的 RedisTemplate 使用 JDK 序列化，存到 Redis 里的数据是一堆二进制乱码，
     * 既不便于调试查看，也存在跨平台兼容性问题。我们改用 JSON 序列化，数据可读性好。
     *
     * @param connectionFactory Redis 连接工厂，由 Spring Boot 根据 application.yml 自动创建
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂 —— 这是和 Redis 服务器通信的"桥梁"
        template.setConnectionFactory(connectionFactory);

        // 配置 Jackson ObjectMapper —— 用于将 Java 对象和 JSON 互相转换
        ObjectMapper objectMapper = new ObjectMapper();
        // 设置所有属性的可见性：让 Jackson 能访问类的所有字段（包括 private）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 激活默认类型信息：在 JSON 中保存对象的类名，反序列化时能还原为正确的类型
        // 比如存入一个 Hero 对象，JSON 里会带上 "@class": "com.aram.mayhem.entity.Hero"
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, // 允许所有子类型（安全场景下应限制）
                ObjectMapper.DefaultTyping.NON_FINAL   // 对非 final 类型的对象都保存类型信息
        );
        // 注册 Java 8 时间模块：让 Jackson 能正确处理 LocalDateTime 等时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 创建 JSON 序列化器，使用上面配置好的 ObjectMapper
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        // 创建字符串序列化器，直接将字符串转为字节
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key 使用 String 序列化 —— 这样在 Redis 客户端中看到的是可读的字符串，而不是乱码
        template.setKeySerializer(stringSerializer);
        // Hash 的 Key 也使用 String 序列化
        template.setHashKeySerializer(stringSerializer);
        // Value 使用 JSON 序列化 —— 将 Java 对象转为 JSON 字符串存储
        template.setValueSerializer(jsonSerializer);
        // Hash 的 Value 也使用 JSON 序列化
        template.setHashValueSerializer(jsonSerializer);

        // 初始化 RedisTemplate 的内部配置（必须在设置完序列化器之后调用）
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建 StringRedisTemplate Bean —— 专用于字符串操作的 Redis 工具
     *
     * StringRedisTemplate 是 RedisTemplate<String, String> 的简化版，
     * 所有 Key 和 Value 都是 String 类型，不需要序列化/反序列化。
     *
     * 使用场景：分布式锁（SETNX 命令）、简单的计数器等
     *
     * @param connectionFactory Redis 连接工厂
     * @return StringRedisTemplate 实例
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * 创建缓存管理器 —— Spring Cache 的核心组件
     *
     * CacheManager 是 Spring Cache 抽象层的核心接口，配合 @Cacheable 注解使用。
     * 当你在 Service 方法上标注 @Cacheable(value="heroDetail")，Spring 会：
     * 1. 先去 Redis 里查有没有 key 为 "heroDetail::参数" 的缓存
     * 2. 如果有，直接返回缓存数据，不执行方法体
     * 3. 如果没有，执行方法体，把返回值存入 Redis
     *
     * 不同的缓存空间可以有不同的过期时间，就像冰箱不同层有不同保鲜期。
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisCacheManager 实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 创建 JSON 和 String 序列化器（与 RedisTemplate 保持一致）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 默认缓存配置 —— 所有缓存空间都遵循的"基本规则"
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL) // 默认过期时间 30 分钟
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer)) // Key 用 String 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)) // Value 用 JSON 序列化
                .disableCachingNullValues(); // 不缓存 null 值（防止缓存穿透）

        // 为不同的缓存空间定制过期时间
        // 就像不同食物有不同的保鲜期：肉类冷冻1个月，蔬菜冷藏3天
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 英雄详情缓存 1 小时
        cacheConfigurations.put("heroDetail", defaultConfig.entryTtl(HERO_DETAIL_TTL));
        // 英雄列表缓存 30 分钟
        cacheConfigurations.put("heroList", defaultConfig.entryTtl(DEFAULT_TTL));
        // 符文列表缓存 30 分钟
        cacheConfigurations.put("augmentList", defaultConfig.entryTtl(AUGMENT_LIST_TTL));
        // 符文详情缓存 30 分钟
        cacheConfigurations.put("augmentDetail", defaultConfig.entryTtl(AUGMENT_LIST_TTL));
        // 公告列表缓存 10 分钟
        cacheConfigurations.put("bulletinList", defaultConfig.entryTtl(BULLETIN_TTL));
        // 最新公告缓存 10 分钟
        cacheConfigurations.put("bulletinLatest", defaultConfig.entryTtl(BULLETIN_TTL));

        // 构建并返回缓存管理器
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig) // 设置默认配置
                .withInitialCacheConfigurations(cacheConfigurations) // 设置各缓存空间的定制配置
                .transactionAware() // 支持事务：如果数据库事务回滚，缓存操作也会回滚
                .build();
    }
}
