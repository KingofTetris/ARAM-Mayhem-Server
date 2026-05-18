package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 强化符文简要信息视图对象
 *
 * 用途：英雄详情页推荐符文展示，仅包含 Chip 展示所需的最小字段
 * 数据流向：HeroServiceImpl → HeroDetailVO → 前端英雄详情页
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AugmentBriefVO {

    private Long id;

    private String nameZh;

    private String quality;

    private String iconUrl;
}
