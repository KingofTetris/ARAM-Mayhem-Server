package com.aram.mayhem.service;

import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;

import java.util.List;

/**
 * 数据聚合服务接口
 *
 * 功能：合并 RiotDataDragon（英雄基础信息）+ AramDataCollector（ARAM 胜率数据）→ Hero/Augment 实体
 * 数据流：
 *   RiotDataDragonClient.fetchChampionList() ─┐
 *   AramDataCollector.collectAramStats()  ─────┤→ DataAggregatorService.aggregateHeroData() → List<Hero>
 *                                              │
 *   AramDataCollector.collectAugmentStats() ───┘→ DataAggregatorService.aggregateAugmentData() → List<Augment>
 * 关联：DataSyncScheduler（定时调用聚合方法）
 */
public interface DataAggregatorService {

    /**
     * 聚合英雄数据
     *
     * 合并 RiotDataDragon 英雄基础信息（名称、称号、技能、图标）与 AramDataCollector ARAM 统计数据（胜率、选取率、梯级）
     * 匹配规则：以 championName（英文名）为关联键
     * 清洗规则：胜率范围 0-100%，选取率范围 0-100%，缺值填充默认值
     *
     * @param version Data Dragon 版本号
     * @return 聚合后的英雄实体列表
     */
    List<Hero> aggregateHeroData(String version);

    /**
     * 聚合强化符文数据
     *
     * 将 AramDataCollector 采集的符文统计数据转换为 Augment 实体
     * 清洗规则：胜率范围 0-100%，选取率范围 0-100%，avgPlacement 范围 1-8
     *
     * @return 聚合后的符文实体列表
     */
    List<Augment> aggregateAugmentData();

    /**
     * 聚合英雄数据并写入数据库（upsert）
     *
     * 流程：aggregateHeroData() → 按 nameEn 查询已有记录 → 存在则更新，不存在则插入
     *
     * @param version Data Dragon 版本号
     * @return 写入的英雄实体列表
     */
    List<Hero> aggregateAndSaveHeroData(String version);

    /**
     * 聚合符文数据并写入数据库（upsert）
     *
     * 流程：aggregateAugmentData() → 按 nameEn 查询已有记录 → 存在则更新，不存在则插入
     *
     * @return 写入的符文实体列表
     */
    List<Augment> aggregateAndSaveAugmentData();
}
