package com.aram.mayhem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 强化符文推荐请求对象
 *
 * 数据流向：前端符文推荐页 → AugmentController → AugmentService
 * 用途：基于英雄和已选符文，请求智能推荐下一个符文
 */
@Data
@Schema(description = "强化符文推荐请求")
public class AugmentRecommendRequest {

    @NotNull(message = "英雄ID不能为空")
    @Schema(description = "英雄ID")
    private Long heroId;

    @NotNull(message = "已选符文ID列表不能为空")
    @Schema(description = "已选的强化符文ID列表")
    private List<Long> selectedAugmentIds;
}