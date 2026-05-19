package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.ValidationResult;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.service.MultiSourceValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MultiSourceValidatorImpl implements MultiSourceValidator {

    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("2.0");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("5.0");

    @Override
    public List<ValidationResult> validateHeroStats(List<Hero> heroes) {
        log.info("[VALIDATE] hero stats validation started | count={}", heroes.size());

        List<ValidationResult> results = new ArrayList<>();
        for (Hero hero : heroes) {
            ValidationResult result = validateSingleHero(hero);
            results.add(result);
        }

        long highCount = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
        long mediumCount = results.stream().filter(r -> "MEDIUM".equals(r.getConfidenceLevel())).count();
        long lowCount = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();

        log.info("[VALIDATE] hero stats validation completed | HIGH={} | MEDIUM={} | LOW={}",
                highCount, mediumCount, lowCount);
        return results;
    }

    @Override
    public List<ValidationResult> validateAugmentStats(List<Augment> augments) {
        log.info("[VALIDATE] augment stats validation started | count={}", augments.size());

        List<ValidationResult> results = new ArrayList<>();
        for (Augment augment : augments) {
            ValidationResult result = validateSingleAugment(augment);
            results.add(result);
        }

        long highCount = results.stream().filter(r -> "HIGH".equals(r.getConfidenceLevel())).count();
        long mediumCount = results.stream().filter(r -> "MEDIUM".equals(r.getConfidenceLevel())).count();
        long lowCount = results.stream().filter(r -> "LOW".equals(r.getConfidenceLevel())).count();

        log.info("[VALIDATE] augment stats validation completed | HIGH={} | MEDIUM={} | LOW={}",
                highCount, mediumCount, lowCount);
        return results;
    }

    private ValidationResult validateSingleHero(Hero hero) {
        List<ValidationResult.FieldDifference> differences = new ArrayList<>();

        checkRange(hero.getWinRate(), new BigDecimal("40"), new BigDecimal("60"), "winRate", differences);
        checkRange(hero.getPickRate(), BigDecimal.ZERO, new BigDecimal("30"), "pickRate", differences);
        checkNullField(hero.getTier(), "tier", differences);
        checkConfidenceLevel(hero.getConfidenceLevel(), differences);

        String confidence = determineConfidence(differences);
        boolean passed = !"LOW".equals(confidence);

        if ("LOW".equals(confidence)) {
            log.debug("[VALIDATE] hero={} | confidence=LOW | differences={}", hero.getNameEn(), differences.size());
        }

        return ValidationResult.builder()
                .targetName(hero.getNameEn())
                .targetType("hero")
                .confidenceLevel(confidence)
                .passed(passed)
                .differences(differences)
                .build();
    }

    private ValidationResult validateSingleAugment(Augment augment) {
        List<ValidationResult.FieldDifference> differences = new ArrayList<>();

        checkRange(augment.getWinRate(), new BigDecimal("40"), new BigDecimal("60"), "winRate", differences);
        checkRange(augment.getPickRate(), BigDecimal.ZERO, new BigDecimal("30"), "pickRate", differences);
        if (augment.getAvgPlacement() != null) {
            checkRange(augment.getAvgPlacement(), BigDecimal.ONE, new BigDecimal("8"), "avgPlacement", differences);
        }
        checkNullField(augment.getTier(), "tier", differences);
        checkNullField(augment.getQuality(), "quality", differences);

        String confidence = determineConfidence(differences);
        boolean passed = !"LOW".equals(confidence);

        return ValidationResult.builder()
                .targetName(augment.getNameEn())
                .targetType("augment")
                .confidenceLevel(confidence)
                .passed(passed)
                .differences(differences)
                .build();
    }

    private void checkRange(BigDecimal value, BigDecimal expectedMin, BigDecimal expectedMax,
                            String fieldName, List<ValidationResult.FieldDifference> differences) {
        if (value == null) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName(fieldName)
                    .source1Value("null")
                    .description(fieldName + " is null")
                    .build());
            return;
        }

        BigDecimal delta = BigDecimal.ZERO;
        if (value.compareTo(expectedMin) < 0) {
            delta = expectedMin.subtract(value);
        } else if (value.compareTo(expectedMax) > 0) {
            delta = value.subtract(expectedMax);
        }

        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName(fieldName)
                    .source1Value(value.toPlainString())
                    .delta(delta.toPlainString())
                    .description(fieldName + "=" + value + " outside expected range [" + expectedMin + ", " + expectedMax + "]")
                    .build());
        }
    }

    private void checkNullField(Object value, String fieldName, List<ValidationResult.FieldDifference> differences) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName(fieldName)
                    .source1Value(value == null ? "null" : "blank")
                    .description(fieldName + " is null or blank")
                    .build());
        }
    }

    private void checkConfidenceLevel(String confidenceLevel, List<ValidationResult.FieldDifference> differences) {
        if (confidenceLevel == null || confidenceLevel.isBlank()) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName("confidenceLevel")
                    .source1Value("null")
                    .description("confidenceLevel is null or blank")
                    .build());
            return;
        }
        if ("low".equalsIgnoreCase(confidenceLevel)) {
            differences.add(ValidationResult.FieldDifference.builder()
                    .fieldName("confidenceLevel")
                    .source1Value(confidenceLevel)
                    .description("confidenceLevel is LOW - data from single source or default values")
                    .build());
        }
    }

    private String determineConfidence(List<ValidationResult.FieldDifference> differences) {
        if (differences.isEmpty()) {
            return "HIGH";
        }

        for (ValidationResult.FieldDifference diff : differences) {
            if (diff.getDelta() != null) {
                BigDecimal delta = new BigDecimal(diff.getDelta());
                if (delta.compareTo(MEDIUM_THRESHOLD) > 0) {
                    return "LOW";
                }
            }
            if (diff.getDelta() == null) {
                return "LOW";
            }
        }

        for (ValidationResult.FieldDifference diff : differences) {
            if (diff.getDelta() != null) {
                BigDecimal delta = new BigDecimal(diff.getDelta());
                if (delta.compareTo(HIGH_THRESHOLD) > 0) {
                    return "MEDIUM";
                }
            }
        }

        return "MEDIUM";
    }
}
