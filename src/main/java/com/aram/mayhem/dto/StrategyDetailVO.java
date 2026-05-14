package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StrategyDetailVO {
    private Long id;
    private Long userId;
    private String authorNickname;
    private String authorAvatar;
    private Long heroId;
    private String heroName;
    private String heroIcon;
    private String title;
    private String description;
    private Integer upvotes;
    private Integer downvotes;
    private LocalDateTime createdAt;
    private List<AugmentVO> augments;
    private List<ItemVO> items;
    private String userVoteType;
}