package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建攻略请求对象 —— 前端发布新攻略时发送
 *
 * 用户在社区页面点击"发布攻略"后，填写攻略信息并提交，
 * 前端将填写的数据封装为这个对象发送给后端。
 *
 * 必填字段：heroId、title、description
 * 可选字段：augmentIds、itemIds（可以不推荐符文或装备）
 *
 * 数据流向：
 * 前端社区页 → POST /api/strategies（携带 CreateStrategyRequest 的 JSON）
 * → StrategyController → StrategyService
 *
 * 关联类：
 * - StrategyController：接收创建攻略请求
 * - StrategyService：处理创建逻辑（保存攻略 + 关联符文和装备）
 */
@Data
public class CreateStrategyRequest {

    /**
     * 关联英雄 ID —— 这个攻略是针对哪个英雄的
     *
     * @NotNull(message = "heroId is required")：英雄 ID 不能为空
     */
    @NotNull(message = "heroId is required")
    private Long heroId;

    /**
     * 攻略标题 —— 简短描述攻略核心内容
     *
     * @NotBlank：不能为空
     * @Size(min = 1, max = 100)：标题长度 1~100 字
     */
    @NotBlank(message = "title is required")
    @Size(min = 1, max = 100, message = "title length must be between 1 and 100")
    private String title;

    /**
     * 攻略正文 —— 详细的玩法说明
     *
     * @NotBlank：不能为空
     * @Size(min = 10)：至少 10 个字，防止发布无意义的内容
     */
    @NotBlank(message = "description is required")
    @Size(min = 10, message = "description must be at least 10 characters")
    private String description;

    /**
     * 推荐符文 ID 列表 —— 可选，关联 tb_augment 表
     * 如 [1, 5, 12] 表示推荐 ID 为 1、5、12 的三个符文
     */
    private List<Long> augmentIds;

    /**
     * 推荐装备 ID 列表 —— 可选，关联装备数据
     * 如 [101, 205] 表示推荐 ID 为 101、205 的两个装备
     */
    private List<Long> itemIds;
}
