package com.aram.mayhem.service;

import com.aram.mayhem.dto.AugmentListVO;
import com.aram.mayhem.dto.AugmentRecommendRequest;
import com.aram.mayhem.dto.AugmentRecommendResponse;
import com.aram.mayhem.dto.AugmentVO;
import com.aram.mayhem.dto.PageResult;
import com.aram.mayhem.dto.SynergyProgressResponse;

import java.util.List;

/**
 * 强化符文服务接口 —— 符文模块的"业务规则说明书"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个接口定义了"强化符文（海克斯）模块"所有对外提供的业务功能。
 * 强化符文是 ARAM 模式中的特殊增益效果，玩家可以在游戏中选择符文来增强英雄。
 *
 * 打个比方：
 * - 英雄 = 游戏角色（如盖伦、提莫）
 * - 强化符文 = 游戏中的增益道具（如"护盾"符文、"攻速"符文）
 * - 套装 = 同类符文的组合（集齐一定数量后激活额外效果）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、本接口提供的方法一览
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方法名                  | 功能说明                      | 返回类型
 * ------------------------|------------------------------|---------------------------
 * getAugmentList()        | 分页查询符文列表              | PageResult<AugmentListVO>
 * getAugmentDetail()      | 查询单个符文详情              | AugmentVO
 * getSynergyProgress()    | 计算已选符文的套装激活进度     | List<SynergyProgressResponse>
 * getRecommendations()    | 基于英雄和已选符文智能推荐     | List<AugmentRecommendResponse>
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、符文品质等级说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 符文按品质从低到高分为三档：
 * - 银色（Silver）  → 最常见的符文，效果较弱
 * - 金色（Legendary/Gold）→ 中等品质，效果适中
 * - 棱彩（Prismatic）→ 最稀有品质，效果最强
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、套装（Synergy）说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 套装是同类符文的组合，集齐一定数量后激活额外效果：
 * - shield（护盾）       → 需1个符文激活
 * - regeneration（再生）  → 需1个符文激活
 * - shield-break（破盾）  → 需1个符文激活
 * - attack-speed（攻速）  → 需2个符文激活
 * - ability-power（法强） → 需2个符文激活
 * - omnivamp（全能吸血）  → 需1个符文激活
 * - armor-penetration（穿甲）→ 需2个符文激活
 * - critical-strike（暴击）→ 需2个符文激活
 * - tenacity（韧性）      → 需1个符文激活
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、相关类关系图
 * ═══════════════════════════════════════════════════════════════════
 *
 *   AugmentController（接收HTTP请求）
 *        │
 *        ▼ 调用
 *   AugmentService（本接口，定义业务方法）
 *        │
 *        ▼ 实现
 *   AugmentServiceImpl（具体业务逻辑，查询数据库+缓存+推荐算法）
 *        │
 *        ▼ 调用
 *   AugmentMapper + HeroMapper（数据库操作）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、实现类
 * ═══════════════════════════════════════════════════════════════════
 *
 * @see com.aram.mayhem.service.impl.AugmentServiceImpl 强化符文服务实现类
 */
public interface AugmentService {

    /**
     * 获取符文列表 —— 符文模块的核心查询方法
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 分页查询强化符文列表，支持按品质和套装筛选。
     * 默认按胜率从高到低排序。
     *
     * 【品质筛选】
     * 选择"棱彩"只显示棱彩品质的符文，选择"银色"只显示银色品质的符文。
     * 不传品质参数则显示所有品质。
     *
     * 【套装筛选】
     * 选择"shield"只显示属于护盾套装的符文。
     * 一个符文最多可以属于3个套装（synergySet、synergySet2、synergySet3），
     * 筛选时会同时匹配这三个字段。
     *
     * ═══════════════════════════════════════════════════════════════
     * 参数说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @param page        页码，从1开始
     * @param size        每页数量
     * @param quality     品质筛选（可选，传null表示不筛选）
     *                    可选值："银色"、"金色"、"棱彩"
     * @param synergySet  套装筛选（可选，传null表示不筛选）
     *                    可选值："shield"、"regeneration"、"attack-speed"等
     * @return PageResult<AugmentListVO> 分页符文列表
     *
     * ═══════════════════════════════════════════════════════════════
     * 调用示例
     * ═══════════════════════════════════════════════════════════════
     *
     * // 获取第1页，每页20个符文，不筛选
     * PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 20, null, null);
     *
     * // 只看棱彩品质的符文
     * PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 20, "棱彩", null);
     *
     * // 只看护盾套装的符文
     * PageResult<AugmentListVO> result = augmentService.getAugmentList(1, 20, null, "shield");
     */
    PageResult<AugmentListVO> getAugmentList(int page, int size, String quality, String synergySet);

    /**
     * 获取符文详情 —— 查看单个符文的完整信息
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 根据符文ID获取该符文的详细信息，包括：
     * - 名称（中英文）、品质、套装属性
     * - 胜率、选取率、平均排名、梯级
     * - 图标URL、效果描述
     * - 是否为陷阱符文（版本陷阱，看起来强但实际弱）
     *
     * ═══════════════════════════════════════════════════════════════
     * 参数说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @param id 符文的唯一ID（数据库主键）
     *           如果传入不存在的ID，返回null
     *
     * @return AugmentVO 符文详情对象，不存在时返回null
     *
     * ═══════════════════════════════════════════════════════════════
     * 缓存说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 实现类使用了 @Cacheable 注解：
     * - 缓存key：augmentDetail::{符文ID}
     * - 相同ID的重复查询会直接从缓存返回
     */
    AugmentVO getAugmentDetail(Long id);

    /**
     * 计算套装激活进度 —— 根据已选符文计算各套装的完成情况
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 玩家在游戏中选择符文后，需要知道哪些套装已经被激活、
     * 哪些套装还差几个符文就能激活。这个方法就是计算这些进度的。
     *
     * 进度状态有三种：
     * - inactive  → 还没有选择该套装的任何符文
     * - partial   → 已选择部分符文，但还没达到激活门槛
     * - completed → 已达到激活门槛，套装效果已激活
     *
     * ═══════════════════════════════════════════════════════════════
     * 参数说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @param augmentIds 已选符文的ID列表，用逗号分隔
     *                   例如："1,2,3" 表示选择了ID为1、2、3的三个符文
     *                   传null或空字符串则返回所有套装的初始进度（全部inactive）
     *
     * @return List<SynergyProgressResponse> 各套装的进度列表，每个元素包含：
     *   - synergyName → 套装名称（如"shield"）
     *   - currentCount → 当前已选符文数量
     *   - totalCount → 激活所需符文数量
     *   - progress → 完成进度（0.0~1.0）
     *   - status → 状态（inactive/partial/completed）
     *   - avgWinRate → 该套装下已选符文的平均胜率
     *
     * ═══════════════════════════════════════════════════════════════
     * 调用示例
     * ═══════════════════════════════════════════════════════════════
     *
     * // 选择了ID为1、5、8的三个符文
     * List<SynergyProgressResponse> progress = augmentService.getSynergyProgress("1,5,8");
     * // 查看护盾套装的进度
     * progress.stream()
     *     .filter(p -> "shield".equals(p.getSynergyName()))
     *     .forEach(p -> System.out.println(p.getStatus()));  // 可能输出 "completed"
     */
    List<SynergyProgressResponse> getSynergyProgress(String augmentIds);

    /**
     * 智能推荐符文 —— 根据英雄和已选符文推荐下一个符文
     *
     * ═══════════════════════════════════════════════════════════════
     * 功能说明
     * ═══════════════════════════════════════════════════════════════
     *
     * 这是符文模块的"智能大脑"，根据以下因素推荐符文：
     *
     * 【推荐评分算法】
     * 评分 = 基础分(50) + 胜率权重(30%) + 选取率权重(10%)
     *      + 平均排名权重(5%) + 套装协同加成(15%) + 品质加成(3~5%)
     *
     * 【排除规则】
     * - 不推荐已选择的符文（避免重复）
     * - 不推荐陷阱符文（isTrap=true的符文）
     *
     * 【套装协同】
     * 如果符文的套装与英雄定位匹配，额外加分：
     * - 坦克/辅助 + 护盾套装 → 加分
     * - 射手/刺客 + 攻速套装 → 加分
     * - 法师 + 法强套装 → 加分
     *
     * ═══════════════════════════════════════════════════════════════
     * 参数说明
     * ═══════════════════════════════════════════════════════════════
     *
     * @param request 推荐请求体，包含：
     *   - heroId → 当前选择的英雄ID（用于匹配英雄定位）
     *   - selectedAugmentIds → 已选符文ID列表（用于排除和计算套装协同）
     *
     * @return List<AugmentRecommendResponse> 推荐符文列表，按评分从高到低排序，每个元素包含：
     *   - 符文基本信息（id、名称、品质、套装、图标等）
     *   - winRate → 胜率
     *   - pickRate → 选取率
     *   - score → 推荐评分（0~100，越高越推荐）
     *   - recommendationReason → 推荐理由（如"高胜率 契合当前英雄定位 顶级品质"）
     *
     * ═══════════════════════════════════════════════════════════════
     * 调用示例
     * ═══════════════════════════════════════════════════════════════
     *
     * // 为英雄ID=1推荐符文，已选了符文ID=5和8
     * AugmentRecommendRequest request = new AugmentRecommendRequest();
     * request.setHeroId(1L);
     * request.setSelectedAugmentIds(List.of(5L, 8L));
     * List<AugmentRecommendResponse> recommendations = augmentService.getRecommendations(request);
     * // 取第一个推荐
     * System.out.println(recommendations.get(0).getNameZh());  // 输出推荐符文名
     * System.out.println(recommendations.get(0).getScore());    // 输出推荐评分
     */
    List<AugmentRecommendResponse> getRecommendations(AugmentRecommendRequest request);
}
