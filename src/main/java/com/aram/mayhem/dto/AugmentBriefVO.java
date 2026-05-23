package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 强化符文简要信息视图对象 —— 只包含最少的展示字段
 *
 * 这个类用于英雄详情页的"推荐符文"区域，只显示符文的 Chip（小卡片），
 * 不需要完整的统计数据，所以只保留 4 个字段。
 *
 * 就像商品详情页的"推荐搭配"区域，只显示搭配商品的缩略图和名称，
 * 点击后才跳转到搭配商品的详情页。
 *
 * @NoArgsConstructor：Lombok 生成无参构造函数（Jackson 反序列化需要）
 * @AllArgsConstructor：Lombok 生成全参构造函数（Service 层创建对象时方便）
 *
 * 数据流向：
 * HeroServiceImpl 查询推荐符文 → 创建 AugmentBriefVO 列表 → 放入 HeroDetailVO
 *
 * 关联类：
 * - HeroDetailVO：英雄详情 VO，包含 recommendedAugments 字段
 */
@Data
@NoArgsConstructor // 生成无参构造函数，Jackson 反序列化 JSON 时需要
@AllArgsConstructor // 生成全参构造函数，Service 层创建对象时方便
public class AugmentBriefVO {

    /** 符文 ID —— 点击时跳转到符文详情页 */
    private Long id;

    /** 符文中文名 —— Chip 上显示的名称 */
    private String nameZh;

    /** 符文品质 —— Chip 上用颜色区分品质 */
    private String quality;

    /** 符文图标 URL —— Chip 上显示的图标 */
    private String iconUrl;
}
