package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiSourceValidatorImplTest {

    private MultiSourceValidatorImpl validator;

    @BeforeEach
    void setUp() {
        validator = new MultiSourceValidatorImpl();
    }

    private Hero buildHero(String nameEn, String tier, double winRate, double pickRate, String confidence) {
        Hero hero = new Hero();
        hero.setNameEn(nameEn);
        hero.setTier(tier);
        hero.setWinRate(BigDecimal.valueOf(winRate));
        hero.setPickRate(BigDecimal.valueOf(pickRate));
        hero.setConfidenceLevel(confidence);
        return hero;
    }

    private Augment buildAugment(String nameEn, String quality, String tier, double winRate, double pickRate, double avgPlacement) {
        Augment augment = new Augment();
        augment.setNameEn(nameEn);
        augment.setQuality(quality);
        augment.setTier(tier);
        augment.setWinRate(BigDecimal.valueOf(winRate));
        augment.setPickRate(BigDecimal.valueOf(pickRate));
        augment.setAvgPlacement(BigDecimal.valueOf(avgPlacement));
        return augment;
    }

    @Nested
    @DisplayName("validateHeroStats 测试")
    class ValidateHeroStatsTest {

        @Test
        @DisplayName("正常范围内的英雄数据 → HIGH 置信度")
        void shouldReturnHighConfidenceForNormalData() {
            Hero hero = buildHero("Aatrox", "S", 52.0, 12.0, "high");
            List<ValidationResult> results = validator.validateHeroStats(List.of(hero));

            assertEquals(1, results.size());
            assertEquals("HIGH", results.get(0).getConfidenceLevel());
            assertTrue(results.get(0).isPassed());
            assertTrue(results.get(0).getDifferences().isEmpty());
        }

        @Test
        @DisplayName("胜率略偏离范围 → MEDIUM 置信度")
        void shouldReturnMediumConfidenceForSlightlyOutOfRange() {
            Hero hero = buildHero("Aatrox", "S", 38.5, 12.0, "high");
            List<ValidationResult> results = validator.validateHeroStats(List.of(hero));

            assertEquals("MEDIUM", results.get(0).getConfidenceLevel());
            assertTrue(results.get(0).isPassed());
        }

        @Test
        @DisplayName("胜率严重偏离范围 → LOW 置信度")
        void shouldReturnLowConfidenceForSeverelyOutOfRange() {
            Hero hero = buildHero("Aatrox", "S", 25.0, 12.0, "high");
            List<ValidationResult> results = validator.validateHeroStats(List.of(hero));

            assertEquals("LOW", results.get(0).getConfidenceLevel());
            assertFalse(results.get(0).isPassed());
        }

        @Test
        @DisplayName("tier 为 null → LOW 置信度")
        void shouldReturnLowConfidenceForNullTier() {
            Hero hero = buildHero("Aatrox", null, 52.0, 12.0, "high");
            List<ValidationResult> results = validator.validateHeroStats(List.of(hero));

            assertEquals("LOW", results.get(0).getConfidenceLevel());
        }

        @Test
        @DisplayName("空英雄列表返回空结果")
        void shouldReturnEmptyForEmptyList() {
            List<ValidationResult> results = validator.validateHeroStats(Collections.emptyList());
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("多英雄批量验证")
        void shouldValidateMultipleHeroes() {
            List<Hero> heroes = List.of(
                    buildHero("Aatrox", "S", 52.0, 12.0, "high"),
                    buildHero("Ahri", "A", 51.0, 8.0, "high"),
                    buildHero("Teemo", null, 25.0, 12.0, "low")
            );
            List<ValidationResult> results = validator.validateHeroStats(heroes);

            assertEquals(3, results.size());
            assertEquals("HIGH", results.get(0).getConfidenceLevel());
            assertEquals("HIGH", results.get(1).getConfidenceLevel());
            assertEquals("LOW", results.get(2).getConfidenceLevel());
        }
    }

    @Nested
    @DisplayName("validateAugmentStats 测试")
    class ValidateAugmentStatsTest {

        @Test
        @DisplayName("正常范围内的符文数据 → HIGH 置信度")
        void shouldReturnHighConfidenceForNormalData() {
            Augment augment = buildAugment("Small MR", "银色", "S", 52.0, 15.0, 3.5);
            List<ValidationResult> results = validator.validateAugmentStats(List.of(augment));

            assertEquals(1, results.size());
            assertEquals("HIGH", results.get(0).getConfidenceLevel());
            assertTrue(results.get(0).isPassed());
        }

        @Test
        @DisplayName("quality 为 null → LOW 置信度")
        void shouldReturnLowConfidenceForNullQuality() {
            Augment augment = buildAugment("Test", null, "S", 52.0, 15.0, 3.5);
            List<ValidationResult> results = validator.validateAugmentStats(List.of(augment));

            assertEquals("LOW", results.get(0).getConfidenceLevel());
        }

        @Test
        @DisplayName("avgPlacement 超出范围 → LOW 置信度")
        void shouldReturnLowConfidenceForOutOfRangePlacement() {
            Augment augment = buildAugment("Test", "银色", "S", 52.0, 15.0, 15.0);
            List<ValidationResult> results = validator.validateAugmentStats(List.of(augment));

            assertEquals("LOW", results.get(0).getConfidenceLevel());
        }

        @Test
        @DisplayName("空符文列表返回空结果")
        void shouldReturnEmptyForEmptyList() {
            List<ValidationResult> results = validator.validateAugmentStats(Collections.emptyList());
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("验证结果包含差异详情")
        void shouldIncludeDifferenceDetails() {
            Augment augment = buildAugment("Test", "银色", "S", 25.0, 15.0, 3.5);
            List<ValidationResult> results = validator.validateAugmentStats(List.of(augment));

            ValidationResult result = results.get(0);
            assertFalse(result.getDifferences().isEmpty());
            boolean hasWinRateDiff = result.getDifferences().stream()
                    .anyMatch(d -> "winRate".equals(d.getFieldName()));
            assertTrue(hasWinRateDiff);
        }
    }
}
