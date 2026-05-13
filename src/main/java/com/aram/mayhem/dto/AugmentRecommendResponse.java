package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AugmentRecommendResponse {

    private Long id;

    private String nameZh;

    private String nameEn;

    private String quality;

    private String synergySet;

    private String iconUrl;

    private BigDecimal winRate;

    private BigDecimal pickRate;

    private BigDecimal avgPlacement;

    private String tier;

    private Boolean isTrap;

    private double score;

    private String recommendationReason;
}