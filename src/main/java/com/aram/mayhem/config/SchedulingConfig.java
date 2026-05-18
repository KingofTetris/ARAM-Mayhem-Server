package com.aram.mayhem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务调度配置
 *
 * 功能：启用 Spring @Scheduled 注解支持
 * 用途：DataSyncScheduler 每 6 小时自动全量同步数据
 * 关联：scheduler/DataSyncScheduler.java
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
