package com.aram.mayhem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务调度配置类
 *
 * 这个配置类的唯一作用就是启用 Spring 的定时任务功能。
 * 加上 @EnableScheduling 注解后，Spring 会自动扫描所有标注了 @Scheduled 的方法，
 * 并按照指定的时间规则定时执行它们。
 *
 * 就像设置了一个闹钟，到了指定时间就会自动响铃提醒你做某件事。
 *
 * 本项目的定时任务：
 * - DataSyncScheduler：每 6 小时自动全量同步英雄和符文数据
 *   - 从 Riot DataDragon 和 U.GG 拉取最新数据
 *   - 更新到本地数据库，确保数据始终是最新的
 *
 * 注意事项：
 * - Spring 默认使用单线程执行定时任务，如果某个任务执行时间很长，会阻塞其他任务
 * - 如果将来有多个定时任务，需要配置线程池（实现 SchedulingConfigurer 接口）
 * - 定时任务在应用重启后会重新计时，不会补执行错过的任务
 *
 * 关联类：
 * - DataSyncScheduler：具体的定时任务逻辑
 *
 * @see com.aram.mayhem.scheduler.DataSyncScheduler
 */
@Configuration // 声明这是一个 Spring 配置类
@EnableScheduling // 启用定时任务支持，让 @Scheduled 注解生效
public class SchedulingConfig {
    // 不需要额外代码，@EnableScheduling 注解已经完成了所有配置
    // Spring 会自动检测并注册所有 @Scheduled 方法
}
