package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HeroListVO {

    private Long id;

    private String nameEn;

    private String nameZh;

    private String title;

    private String role;

    private String tier;

    private BigDecimal winRate;

    private BigDecimal pickRate;

    private String imageUrl;

    private Boolean isVersionTrap;
}
