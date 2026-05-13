package com.aram.mayhem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SynergyProgressResponse {

    private String synergyName;

    private int currentCount;

    private int totalCount;

    private double progress;

    private String status;

    private BigDecimal avgWinRate;
}