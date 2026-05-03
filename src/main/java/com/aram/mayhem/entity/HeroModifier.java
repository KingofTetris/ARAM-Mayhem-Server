package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("tb_hero_modifier")
public class HeroModifier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long heroId;

    private String modifierName;

    private BigDecimal modifierValue;
}
