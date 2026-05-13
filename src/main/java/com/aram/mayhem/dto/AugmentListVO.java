package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AugmentListVO {

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
}
