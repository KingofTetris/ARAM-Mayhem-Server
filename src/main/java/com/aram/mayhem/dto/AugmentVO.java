package com.aram.mayhem.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class AugmentVO extends AugmentListVO {

    private String description;

    private String synergySet2;

    private String synergySet3;

    private Boolean isTrap;
}
