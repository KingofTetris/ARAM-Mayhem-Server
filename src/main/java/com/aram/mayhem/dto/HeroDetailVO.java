package com.aram.mayhem.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeroDetailVO extends HeroListVO {

    private String description;

    private List<SkillInfo> skills;

    private List<String> counterTips;

    private List<String> synergies;

    private BigDecimal avgKills;

    private BigDecimal avgDeaths;

    private BigDecimal avgAssists;

    private String recommendedBuild;

    @Data
    public static class SkillInfo {
        private String key;
        private String name;
        private String description;
    }
}
