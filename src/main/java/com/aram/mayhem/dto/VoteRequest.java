package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 投票请求对象
 *
 * 数据流向：前端社区 → VoteController → StrategyService → Vote Entity
 */
@Data
public class VoteRequest {

    /** 投票类型（UP=点赞, DOWN=踩） */
    @NotBlank(message = "voteType is required")
    @Pattern(regexp = "UP|DOWN", message = "voteType must be UP or DOWN")
    private String voteType;
}