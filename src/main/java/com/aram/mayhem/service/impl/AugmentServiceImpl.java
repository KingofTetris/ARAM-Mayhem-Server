package com.aram.mayhem.service.impl;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentRecommendRequest;
import com.aram.mayhem.dto.AugmentRecommendResponse;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.SynergyProgressResponse;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import com.aram.mayhem.service.AugmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 强化符文服务实现类
 *
 * 功能：符文列表查询、符文详情、套装进度计算、智能推荐算法
 * 关联：AugmentMapper, HeroMapper, AugmentService
 * 算法：推荐评分 = 胜率权重 + 套装协同权重 - 陷阱惩罚
 */
@Service
public class AugmentServiceImpl implements AugmentService {

    private static final Logger log = LoggerFactory.getLogger(AugmentServiceImpl.class);

    private static final String CACHE_KEY_PREFIX = "augment:list:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private static final Map<String, Integer> SYNERGY_THRESHOLDS = new HashMap<>();
    private static final List<String> ALL_SYNERGIES = Arrays.asList(
            "shield", "regeneration", "shield-break", "attack-speed",
            "ability-power", "omnivamp", "armor-penetration",
            "critical-strike", "tenacity"
    );

    static {
        SYNERGY_THRESHOLDS.put("shield", 1);
        SYNERGY_THRESHOLDS.put("regeneration", 1);
        SYNERGY_THRESHOLDS.put("shield-break", 1);
        SYNERGY_THRESHOLDS.put("attack-speed", 2);
        SYNERGY_THRESHOLDS.put("ability-power", 2);
        SYNERGY_THRESHOLDS.put("omnivamp", 1);
        SYNERGY_THRESHOLDS.put("armor-penetration", 2);
        SYNERGY_THRESHOLDS.put("critical-strike", 2);
        SYNERGY_THRESHOLDS.put("tenacity", 1);
    }

    private final AugmentMapper augmentMapper;
    private final HeroMapper heroMapper;
    private final StringRedisTemplate redisTemplate;

    public AugmentServiceImpl(AugmentMapper augmentMapper, HeroMapper heroMapper, StringRedisTemplate redisTemplate) {
        this.augmentMapper = augmentMapper;
        this.heroMapper = heroMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取符文列表（符文模块）
     *
     * 作用：分页查询强化符文列表，支持按品质和套装筛选
     * 品质类型：prismatic(棱彩)/legendary(金色)/epic(史诗)/silver(银色)
     * 排序：按胜率降序
     *
     * @param page        页码（从1开始）
     * @param size        每页数量
     * @param quality     品质筛选（可选）
     * @param synergySet  套装筛选（可选）
     * @return PageResult<AugmentListVO> 分页符文列表
     */
    @Override
    public PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet) {
        String cacheKey = CACHE_KEY_PREFIX + quality + ":" + synergySet + ":" + page + ":" + size;

        Page<Augment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Augment> wrapper = new LambdaQueryWrapper<>();

        // 品质筛选
        if (quality != null && !quality.isEmpty()) {
            wrapper.eq(Augment::getQuality, quality);
        }
        // 套装筛选（支持多个套装字段）
        if (synergySet != null && !synergySet.isEmpty()) {
            wrapper.and(w -> w
                    .eq(Augment::getSynergySet, synergySet)
                    .or()
                    .eq(Augment::getSynergySet2, synergySet)
                    .or()
                    .eq(Augment::getSynergySet3, synergySet)
            );
        }

        // 按胜率降序排序
        wrapper.orderByDesc(Augment::getWinRate);
        Page<Augment> result = augmentMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        List<AugmentListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), voList);
    }

    /**
     * 获取符文详情（符文模块）
     *
     * 作用：根据符文ID获取详细信息，包括效果描述、品质、套装属性等
     *
     * @param id 符文ID
     * @return AugmentVO 符文详情，不存在返回null
     */
    @Override
    public AugmentVO getAugmentDetail(Long id) {
        Augment augment = augmentMapper.selectById(id);
        if (augment == null) {
            return null;
        }
        return convertToVO(augment);
    }

    /**
     * 获取套装进度（符文模块）
     *
     * 作用：根据已选符文ID列表，计算并返回各套装的激活进度
     * 套装类型：shield(护盾)/regeneration(再生)/shield-break(破盾)等
     *
     * @param augmentIds 已选符文ID列表（逗号分隔）
     * @return List<SynergyProgressResponse> 各套装进度列表
     */
    @Override
    public List<SynergyProgressResponse> getSynergyProgress(String augmentIds) {
        // 参数校验：空输入返回空进度
        if (augmentIds == null || augmentIds.isEmpty()) {
            return buildEmptyProgress();
        }

        // 解析ID列表
        List<Long> ids = Arrays.stream(augmentIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return buildEmptyProgress();
        }

        // 批量查询符文
        List<Augment> augments = augmentMapper.selectBatchIds(ids);

        // 按套装分组
        Map<String, List<Augment>> synergyMap = new HashMap<>();
        for (String synergy : ALL_SYNERGIES) {
            synergyMap.put(synergy, new ArrayList<>());
        }

        // 填充套装映射（支持符文的多个套装属性）
        for (Augment augment : augments) {
            addToSynergyMap(synergyMap, augment.getSynergySet(), augment);
            addToSynergyMap(synergyMap, augment.getSynergySet2(), augment);
            addToSynergyMap(synergyMap, augment.getSynergySet3(), augment);
        }

        // 构建进度响应并排序
        return synergyMap.entrySet().stream()
                .map(entry -> buildSynergyProgress(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SynergyProgressResponse::getSynergyName))
                .collect(Collectors.toList());
    }

    /**
     * 获取符文推荐（符文模块）
     *
     * 作用：根据用户选择的英雄和已选符文，智能推荐合适的强化符文
     * 推荐算法：胜率权重 + 套装协同权重 + 品质加成 - 陷阱惩罚
     * 排除规则：不推荐已选择的符文和陷阱符文
     *
     * @param request 推荐请求体（英雄ID、已选符文ID列表）
     * @return List<AugmentRecommendResponse> 符文推荐列表（按评分降序）
     */
    @Override
    public List<AugmentRecommendResponse> getRecommendations(AugmentRecommendRequest request) {
        log.info("Getting augment recommendations: heroId={}, selectedAugments={}", request.getHeroId(), request.getSelectedAugmentIds());

        // 查询英雄信息
        Hero hero = heroMapper.selectById(request.getHeroId());
        if (hero == null) {
            log.warn("Hero not found for recommendations: heroId={}", request.getHeroId());
            return new ArrayList<>();
        }

        // 已选符文ID集合（用于排除）
        Set<Long> excludeIds = new HashSet<>(request.getSelectedAugmentIds());

        // 查询候选符文（排除已选和陷阱符文）
        LambdaQueryWrapper<Augment> wrapper = new LambdaQueryWrapper<>();
        wrapper.notIn(!excludeIds.isEmpty(), Augment::getId, excludeIds);
        wrapper.eq(Augment::getIsTrap, false);
        wrapper.orderByDesc(Augment::getWinRate);

        // 限制查询数量（取前20个候选）
        Page<Augment> page = new Page<>(1, 20);
        Page<Augment> result = augmentMapper.selectPage(page, wrapper);

        String heroRole = hero.getRole();

        // 转换为推荐响应并按评分排序
        return result.getRecords().stream()
                .map(augment -> convertToRecommendResponse(augment, heroRole, request.getSelectedAugmentIds()))
                .sorted(Comparator.comparingDouble(AugmentRecommendResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    private void addToSynergyMap(Map<String, List<Augment>> synergyMap, String synergy, Augment augment) {
        if (synergy != null && !synergy.isEmpty() && synergyMap.containsKey(synergy)) {
            synergyMap.get(synergy).add(augment);
        }
    }

    private List<SynergyProgressResponse> buildEmptyProgress() {
        return ALL_SYNERGIES.stream()
                .map(synergy -> {
                    SynergyProgressResponse resp = new SynergyProgressResponse();
                    resp.setSynergyName(synergy);
                    resp.setCurrentCount(0);
                    resp.setTotalCount(SYNERGY_THRESHOLDS.getOrDefault(synergy, 1));
                    resp.setProgress(0.0);
                    resp.setStatus("inactive");
                    return resp;
                })
                .collect(Collectors.toList());
    }

    private SynergyProgressResponse buildSynergyProgress(String synergyName, List<Augment> augments) {
        int total = SYNERGY_THRESHOLDS.getOrDefault(synergyName, 1);
        int count = (int) augments.stream().distinct().count();
        double progress = Math.min(1.0, (double) count / total);

        String status;
        if (count == 0) {
            status = "inactive";
        } else if (count >= total) {
            status = "completed";
        } else {
            status = "partial";
        }

        BigDecimal avgWinRate = augments.stream()
                .map(Augment::getWinRate)
                .filter(wr -> wr != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, augments.size())), 4, BigDecimal.ROUND_HALF_UP);

        SynergyProgressResponse resp = new SynergyProgressResponse();
        resp.setSynergyName(synergyName);
        resp.setCurrentCount(count);
        resp.setTotalCount(total);
        resp.setProgress(progress);
        resp.setStatus(status);
        resp.setAvgWinRate(avgWinRate);
        return resp;
    }

    private AugmentRecommendResponse convertToRecommendResponse(Augment augment, String heroRole, List<Long> selectedIds) {
        AugmentRecommendResponse resp = new AugmentRecommendResponse();
        resp.setId(augment.getId());
        resp.setNameZh(augment.getNameZh());
        resp.setNameEn(augment.getNameEn());
        resp.setQuality(augment.getQuality());
        resp.setSynergySet(augment.getSynergySet());
        resp.setIconUrl(augment.getIconUrl());
        resp.setWinRate(augment.getWinRate());
        resp.setPickRate(augment.getPickRate());
        resp.setAvgPlacement(augment.getAvgPlacement());
        resp.setTier(augment.getTier());
        resp.setIsTrap(augment.getIsTrap());

        double score = calculateScore(augment, heroRole, selectedIds);
        resp.setScore(score);
        resp.setRecommendationReason(generateReason(augment, heroRole));

        return resp;
    }

    private double calculateScore(Augment augment, String heroRole, List<Long> selectedIds) {
        double score = 50.0;

        if (augment.getWinRate() != null) {
            score += augment.getWinRate().doubleValue() * 30;
        }

        if (augment.getPickRate() != null) {
            score += augment.getPickRate().doubleValue() * 10;
        }

        if (augment.getAvgPlacement() != null && augment.getAvgPlacement().compareTo(BigDecimal.ZERO) > 0) {
            score += (4.0 - augment.getAvgPlacement().doubleValue()) * 5;
        }

        String synergy = augment.getSynergySet();
        if (synergy != null && isRoleSynergyMatch(synergy, heroRole)) {
            score += 15;
        }

        if ("PRISMATIC".equals(augment.getQuality())) {
            score += 5;
        } else if ("LEGENDARY".equals(augment.getQuality())) {
            score += 3;
        }

        return Math.max(0, Math.min(100, score));
    }

    private boolean isRoleSynergyMatch(String synergy, String role) {
        if (role == null) return false;
        role = role.toLowerCase();

        if (synergy.contains("shield") && (role.contains("tank") || role.contains("support"))) {
            return true;
        }
        if (synergy.contains("regeneration") && (role.contains("support") || role.contains("healer"))) {
            return true;
        }
        if (synergy.contains("attack-speed") && (role.contains("marksman") || role.contains("assassin"))) {
            return true;
        }
        if (synergy.contains("ability-power") && (role.contains("mage") || role.contains("ap"))) {
            return true;
        }
        if (synergy.contains("critical-strike") && (role.contains("marksman") || role.contains("assassin"))) {
            return true;
        }

        return false;
    }

    private String generateReason(Augment augment, String heroRole) {
        StringBuilder reason = new StringBuilder();

        if (augment.getWinRate() != null && augment.getWinRate().compareTo(new BigDecimal("50")) > 0) {
            reason.append("高胜率 ");
        }

        if (isRoleSynergyMatch(augment.getSynergySet(), heroRole)) {
            reason.append("契合当前英雄定位 ");
        }

        if ("PRISMATIC".equals(augment.getQuality())) {
            reason.append("顶级品质");
        } else if ("LEGENDARY".equals(augment.getQuality())) {
            reason.append("高品质");
        }

        if (reason.length() == 0) {
            reason.append("综合表现优秀");
        }

        return reason.toString().trim();
    }

    private AugmentListVO convertToListVO(Augment augment) {
        AugmentListVO vo = new AugmentListVO();
        vo.setId(augment.getId());
        vo.setNameZh(augment.getNameZh());
        vo.setNameEn(augment.getNameEn());
        vo.setQuality(augment.getQuality());
        vo.setSynergySet(augment.getSynergySet());
        vo.setIconUrl(augment.getIconUrl());
        vo.setWinRate(augment.getWinRate());
        vo.setPickRate(augment.getPickRate());
        vo.setAvgPlacement(augment.getAvgPlacement());
        vo.setTier(augment.getTier());
        return vo;
    }

    private AugmentVO convertToVO(Augment augment) {
        AugmentVO vo = new AugmentVO();
        BeanUtils.copyProperties(augment, vo);
        return vo;
    }
}