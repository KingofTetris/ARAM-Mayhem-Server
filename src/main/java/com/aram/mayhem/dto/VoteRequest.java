package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 投票请求对象 —— 前端对攻略进行点赞/踩时发送
 *
 * 投票逻辑：
 * - 用户第一次投票：创建新的投票记录
 * - 用户再次投票（相同类型）：取消投票
 * - 用户再次投票（不同类型）：切换投票类型（赞→踩 或 踩→赞）
 *
 * 数据流向：
 * 前端社区页 → POST /api/strategies/{id}/vote（携带 VoteRequest 的 JSON）
 * → VoteController → StrategyService
 *
 * 关联类：
 * - VoteController：接收投票请求
 * - StrategyService：处理投票逻辑
 * - Vote：投票记录实体类
 */
@Data
public class VoteRequest {

    /**
     * 投票类型 —— 只允许两个值
     *
     * @NotBlank(message = "voteType is required")：投票类型不能为空
     * @Pattern(regexp = "UP|DOWN")：只允许 "UP"（点赞）或 "DOWN"（踩）
     * 使用正则表达式校验，防止传入非法值
     */
    @NotBlank(message = "voteType is required")
    @Pattern(regexp = "UP|DOWN", message = "voteType must be UP or DOWN")
    private String voteType;
}
