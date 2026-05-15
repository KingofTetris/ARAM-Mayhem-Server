package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "tb_hero", autoResultMap = true)
public class Hero {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer riotId;

    private String nameEn;

    private String nameZh;

    private String title;

    private String role;

    private String imageUrl;

    private String tier;

    private BigDecimal winRate;

    private BigDecimal pickRate;

    private String confidenceLevel;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<SkillData> skills;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> counterTips;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> synergies;

    private BigDecimal avgKills;

    private BigDecimal avgDeaths;

    private BigDecimal avgAssists;

    private String recommendedBuild;

    private String version;

    private Boolean isVersionTrap;

    private LocalDateTime versionTrapSince;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Data
    public static class SkillData {
        private String key;
        private String name;
        private String description;
    }
}
