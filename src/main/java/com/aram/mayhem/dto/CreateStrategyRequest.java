package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建攻略请求对象
 *
 * 数据流向：前端社区 → StrategyController → StrategyService → Strategy Entity
 */
@Data
public class CreateStrategyRequest {

    /** 关联英雄 ID */
    @NotNull(message = "heroId is required")
    private Long heroId;

    /** 攻略标题（1~100字） */
    @NotBlank(message = "title is required")
    @Size(min = 1, max = 100, message = "title length must be between 1 and 100")
    private String title;

    /** 攻略正文（至少10字） */
    @NotBlank(message = "description is required")
    @Size(min = 10, message = "description must be at least 10 characters")
    private String description;

    /** 推荐符文 ID 列表 */
    private List<Long> augmentIds;

    /** 推荐装备 ID 列表 */
    private List<Long> itemIds;
}