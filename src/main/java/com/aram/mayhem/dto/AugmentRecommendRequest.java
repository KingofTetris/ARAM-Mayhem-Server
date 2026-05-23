package com.aram.mayhem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 强化符文推荐请求对象 —— 前端请求智能推荐符文时发送的参数
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当用户在符文推荐页面选择了英雄和部分符文后，
 * 前端会发送这个请求对象，告诉后端：
 * 1. 你选了哪个英雄（heroId）
 * 2. 你已经选了哪些符文（selectedAugmentIds）
 *
 * 后端根据这些信息，计算推荐下一个最优符文。
 *
 * 就像去餐厅点菜：
 * - heroId = 你选的主菜（决定了推荐什么配菜）
 * - selectedAugmentIds = 你已经点的配菜（避免重复推荐）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 前端符文推荐页（AugmentRecommendFragment）
 *     ↓ HTTP POST /api/augments/recommend
 * AugmentController.getRecommendations()
 *     ↓ 解析请求参数
 * AugmentService.getRecommendations(AugmentRecommendRequest)
 *     ↓ 计算推荐结果
 * 返回 List<AugmentRecommendResponse>
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、参数校验
 * ═══════════════════════════════════════════════════════════════════
 *
 * @NotNull 注解确保必填字段不为空：
 * - heroId 不能为空：不知道英雄就无法推荐
 * - selectedAugmentIds 不能为空：至少传空列表（表示还没选任何符文）
 *
 * 如果校验失败，Spring 会自动返回 400 Bad Request
 */
@Data
@Schema(description = "强化符文推荐请求")
public class AugmentRecommendRequest {

    /**
     * 英雄ID —— 用户当前选择的英雄
     *
     * 为什么需要英雄ID？
     * - 不同英雄适合不同的符文（如法师适合法强符文，战士适合攻击符文）
     * - 推荐算法会根据英雄类型和属性，优先推荐匹配度高的符文
     *
     * 示例：heroId=1（亚索）→ 推荐攻击类符文
     *       heroId=5（拉克丝）→ 推荐法强类符文
     */
    @NotNull(message = "英雄ID不能为空")
    @Schema(description = "英雄ID")
    private Long heroId;

    /**
     * 已选符文ID列表 —— 用户已经选择的符文
     *
     * 为什么需要已选符文列表？
     * 1. 避免重复推荐：已经选了的符文不会再推荐
     * 2. 套装进度计算：根据已选符文计算当前套装激活进度
     * 3. 协同推荐：推荐与已选符文有协同效果的符文
     *
     * 示例：
     * - 空列表 []：还没选任何符文，推荐最通用的符文
     * - [1, 3, 5]：已经选了3个符文，推荐第4个符文
     *
     * ARAM Mayhem 模式中每局可选 4 个符文，所以列表最多 3 个（推荐第4个）
     */
    @NotNull(message = "已选符文ID列表不能为空")
    @Schema(description = "已选的强化符文ID列表")
    private List<Long> selectedAugmentIds;
}