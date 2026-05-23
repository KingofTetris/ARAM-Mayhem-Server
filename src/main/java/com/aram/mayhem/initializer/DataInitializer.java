package com.aram.mayhem.initializer;

import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.HeroMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 英雄数据初始化器 —— 应用启动时的"种子数据播种机"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类实现了 Spring Boot 的 CommandLineRunner 接口，在应用启动完成后自动执行。
 * 它的作用是检查 tb_hero 表是否为空，如果为空则插入一批"种子数据"（初始英雄数据）。
 *
 * 打个比方：
 * - 如果数据库是一块"空地"，那么本类就是"播种机"
 * - 第一次启动时，空地上什么都没有，播种机自动播下种子
 * - 之后启动时，空地上已经有庄稼了，播种机就不再播种（避免重复）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、CommandLineRunner 是什么？
 * ═══════════════════════════════════════════════════════════════════
 *
 * CommandLineRunner 是 Spring Boot 提供的接口，只有一个方法：run(String... args)
 * 所有实现了这个接口的 @Component，都会在 Spring 容器启动完成后自动执行 run 方法。
 *
 * 执行时机：ApplicationContext 完全初始化之后，Application.run() 返回之前
 * 执行顺序：可以通过 @Order 注解或 Ordered 接口控制多个 Runner 的执行顺序
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、幂等性设计
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类是幂等的（Idempotent），即多次执行的结果与一次执行相同：
 * - 第一次启动：tb_hero 为空 → 插入种子数据
 * - 第二次启动：tb_hero 不为空 → 跳过插入
 *
 * 判断依据：heroMapper.selectCount(null) > 0
 * 如果表中已有数据，说明之前已经初始化过，不需要重复插入。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、种子数据说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 种子数据包含 167 个英雄的基础信息：
 * - 英雄名称（英文/中文）、称号、角色定位
 * - 梯级评级（S+/S/A/B）、胜率、选取率
 * - 置信度等级（高/中/低，表示统计数据的可靠性）
 *
 * 注意：这些是"初始数据"，后续会被 DataSyncScheduler 的定时同步任务
 * 从 Riot DataDragon 和 U.GG 获取最新数据覆盖。
 * 种子数据的作用是确保应用首次启动时就有可展示的数据。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、事务说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Transactional 注解确保整个 run 方法在一个数据库事务中执行：
 * - 如果插入过程中出现异常，所有已插入的数据都会回滚
 * - 保证数据一致性：要么全部插入成功，要么全部不插入
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Hero → 英雄实体类，对应 tb_hero 表
 * - HeroMapper → MyBatis-Plus Mapper，提供 CRUD 操作
 * - AugmentDataInitializer → 符文数据初始化器（类似的播种机）
 * - BulletinDataInitializer → 公告数据初始化器（类似的播种机）
 * - DataSyncScheduler → 定时同步调度器，后续会更新种子数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    /**
     * HeroMapper —— MyBatis-Plus 提供的英雄表 CRUD 操作接口
     *
     * 主要使用的方法：
     * - selectCount(null)：查询表中总记录数（null 表示无条件）
     * - insert(hero)：插入一条英雄记录
     */
    private final HeroMapper heroMapper;

    /**
     * 构造函数 —— Spring 自动注入 HeroMapper
     *
     * @param heroMapper 英雄表 Mapper（由 MyBatis-Plus 自动生成实现类）
     */
    public DataInitializer(HeroMapper heroMapper) {
        this.heroMapper = heroMapper;
    }

    /**
     * 按角色定位分组的克制建议
     *
     * ══════════════════════════════════════════════════════════════
     * 数据结构
     * ══════════════════════════════════════════════════════════════
     *
     * Key：角色定位（Fighter/Mage/Assassin/Tank/Marksman/Support）
     * Value：克制该角色的建议列表（4条建议）
     *
     * 示例：对抗战士（Fighter）的建议：
     * - "保持距离风筝" → 战士需要贴身，保持距离就能避免伤害
     * - "利用控制技能打断突进" → 战士依赖突进技能近身
     * - "集火优先击杀" → 战士通常不是最肉的目标
     * - "购买护甲装备对抗" → 战士主要造成物理伤害
     */
    private static final Map<String, List<String>> COUNTER_TIPS_BY_ROLE = Map.of(
            "Fighter", List.of("保持距离风筝", "利用控制技能打断突进", "集火优先击杀", "购买护甲装备对抗"),
            "Mage", List.of("利用突进贴身", "购买魔抗装备", "躲避关键技能后反打", "侧翼切入绕过前排"),
            "Assassin", List.of("抱团避免落单", "购买中娅沙漏", "携带虚弱召唤师技能", "辅助优先保护C位"),
            "Tank", List.of("忽略坦克集火C位", "购买百分比生命值伤害装备", "利用真实伤害英雄", "避免被开团"),
            "Marksman", List.of("刺客侧翼切入", "利用突进贴身", "购买兰顿之兆减暴击", "闪现开团秒杀"),
            "Support", List.of("优先击杀辅助", "忽略辅助打C位", "利用AOE同时伤害", "购买重伤克制治疗")
    );

    /**
     * 按角色定位分组的搭配建议
     *
     * Key：角色定位
     * Value：与该角色搭配良好的队友类型列表（4条建议）
     *
     * 示例：战士（Fighter）适合搭配：
     * - "控制型坦克" → 坦克控制敌人，战士跟进输出
     * - "增益型辅助" → 辅助提供增益，战士更强
     * - "AOE法师" → 法师群体伤害，战士收割残血
     * - "保护型辅助" → 辅助保护战士不被集火
     */
    private static final Map<String, List<String>> SYNERGIES_BY_ROLE = Map.of(
            "Fighter", List.of("控制型坦克", "增益型辅助", "AOE法师", "保护型辅助"),
            "Mage", List.of("前排坦克", "控制型辅助", "突进战士", "开团型坦克"),
            "Assassin", List.of("控制型辅助", "减速型法师", "保护型辅助", "开团型坦克"),
            "Tank", List.of("AOE法师", "持续输出射手", "增益型辅助", "突进战士"),
            "Marksman", List.of("保护型辅助", "前排坦克", "控制型法师", "增益型辅助"),
            "Support", List.of("持续输出射手", "突进战士", "AOE法师", "前排坦克")
    );

    /**
     * 按角色定位分组的推荐出装路线
     *
     * Key：角色定位
     * Value：推荐出装路线（6件装备，按购买顺序排列）
     *
     * 示例：战士（Fighter）推荐出装：
     * "渴血战斧 → 斯特拉克的挑战护手 → 破败王者之刃 → 振奋盔甲 → 兰顿之兆 → 铁板靴"
     * - 渴血战斧：核心输出装备
     * - 斯特拉克的挑战护手：提供护盾和韧性
     * - 破败王者之刃：百分比伤害 + 续航
     * - 振奋盔甲：魔抗 + 治疗/护盾增强
     * - 兰顿之兆：护甲 + 暴击减免
     * - 铁板靴：护甲鞋
     */
    private static final Map<String, String> RECOMMENDED_BUILD_BY_ROLE = Map.of(
            "Fighter", "渴血战斧 → 斯特拉克的挑战护手 → 破败王者之刃 → 振奋盔甲 → 兰顿之兆 → 铁板靴",
            "Mage", "卢登的伙伴 → 影焰 → 灭世者的死亡之帽 → 虚空之杖 → 中娅沙漏 → 法师之靴",
            "Assassin", "幽梦之灵 → 收集者 → 赛瑞尔达的怨恨 → 夜之锋刃 → 守护天使 → 明悟之靴",
            "Tank", "凛冬之临 → 振奋盔甲 → 兰顿之兆 → 石像鬼石板甲 → 骑士之誓 → 铁板靴",
            "Marksman", "海妖杀手 → 无尽之刃 → 幻影之舞 → 收集者 → 守护天使 → 狂战士胫甲",
            "Support", "帝国指令 → 流水法杖 → 救赎 → 香炉 → 骑士之誓 → 明悟之靴"
    );

    /**
     * 应用启动后自动执行的方法 —— 检查并初始化英雄种子数据
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 检查 tb_hero 表是否已有数据（selectCount > 0）
     * 2. 如果已有数据 → 直接返回（幂等性保证）
     * 3. 如果没有数据 → 创建 167 个英雄的种子数据
     * 4. 逐条插入到数据库
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么不用批量插入？
     * ══════════════════════════════════════════════════════════════
     *
     * MyBatis-Plus 的 insert 方法一次只插入一条记录。
     * 虽然可以使用 SQL 批量插入提升性能，但：
     * - 种子数据只在首次启动时执行一次，性能不是关键
     * - 逐条插入更安全，如果某条数据有问题不会影响其他数据
     * - @Transactional 保证整体原子性
     *
     * @param args Spring Boot 启动参数（本类不使用）
     */
    @Override
    @Transactional
    public void run(String... args) {
        // 幂等性检查：如果表中已有数据，跳过初始化
        if (heroMapper.selectCount(null) > 0) {
            return;
        }

        // 创建 167 个英雄的种子数据
        // 每个英雄包含：Riot ID、英文名、中文名、称号、角色、梯级、胜率、选取率、置信度
        List<Hero> heroes = List.of(
                createHero(1, "Aatrox", "亚托克斯", "暗裔剑魔", "Fighter", "S", 51.2, 8.5, "高"),
                createHero(2, "Ahri", "阿狸", "九尾妖狐", "Mage", "A", 50.1, 12.3, "中"),
                createHero(3, "Akali", "阿卡丽", "离群之刺", "Assassin", "B", 48.5, 7.2, "高"),
                createHero(4, "Akshan", "阿克尚", "哨兵", "Marksman", "A", 50.3, 5.1, "中"),
                createHero(5, "Alistar", "阿利斯塔", "牛头酋长", "Tank", "A", 50.8, 5.1, "中"),
                createHero(6, "Amumu", "阿木木", "殇之木乃伊", "Tank", "S+", 53.1, 9.8, "低"),
                createHero(7, "Anivia", "艾尼维亚", "冰晶凤凰", "Mage", "S", 52.5, 3.2, "中"),
                createHero(8, "Annie", "安妮", "黑暗之女", "Mage", "A", 51.5, 4.2, "低"),
                createHero(9, "Aphelios", "厄斐琉斯", "残月之肃", "Marksman", "B", 48.1, 3.8, "高"),
                createHero(10, "Ashe", "艾希", "寒冰射手", "Marksman", "A", 50.3, 10.1, "低"),
                createHero(11, "AurelionSol", "奥瑞利安索尔", "铸星龙王", "Mage", "S", 52.8, 3.5, "高"),
                createHero(12, "Aurora", "奥萝拉", "灵罗女巫", "Mage", "A", 50.5, 4.8, "中"),
                createHero(13, "Azir", "阿兹尔", "沙漠皇帝", "Mage", "B", 47.2, 2.1, "高"),
                createHero(14, "Bard", "巴德", "星界游神", "Support", "A", 50.5, 4.8, "高"),
                createHero(15, "Belveth", "贝蕾亚", "虚空女皇", "Fighter", "S", 52.1, 6.5, "中"),
                createHero(16, "Blitzcrank", "布里茨", "蒸汽机器人", "Tank", "S", 52.1, 8.9, "低"),
                createHero(17, "Brand", "布兰德", "复仇焰魂", "Mage", "S+", 54.2, 11.5, "中"),
                createHero(18, "Braum", "布隆", "弗雷尔卓德之心", "Support", "A", 50.7, 3.9, "低"),
                createHero(19, "Briar", "布里尔", "狂怒蔷薇", "Fighter", "A", 50.9, 5.2, "高"),
                createHero(20, "Caitlyn", "凯特琳", "皮城女警", "Marksman", "A", 50.2, 6.7, "中"),
                createHero(21, "Camille", "卡蜜尔", "青钢影", "Fighter", "B", 48.7, 4.1, "高"),
                createHero(22, "Cassiopeia", "卡西奥佩娅", "魔蛇之拥", "Mage", "B", 49.1, 2.8, "高"),
                createHero(23, "Chogath", "科加斯", "虚空恐惧", "Tank", "S", 52.5, 7.3, "低"),
                createHero(24, "Corki", "库奇", "英勇投弹手", "Marksman", "B", 48.1, 2.8, "中"),
                createHero(25, "Darius", "德莱厄斯", "诺克萨斯之手", "Fighter", "S", 52.3, 9.1, "低"),
                createHero(26, "Diana", "戴安娜", "皎月女神", "Fighter", "A", 50.9, 5.6, "中"),
                createHero(27, "Draven", "德莱文", "荣耀行刑官", "Marksman", "B", 49.1, 4.3, "高"),
                createHero(28, "DrMundo", "蒙多医生", "祖安狂人", "Tank", "A", 51.0, 4.5, "低"),
                createHero(29, "Ekko", "艾克", "时间刺客", "Assassin", "A", 50.4, 6.2, "高"),
                createHero(30, "Elise", "伊莉丝", "蜘蛛女皇", "Mage", "B", 48.8, 2.5, "高"),
                createHero(31, "Evelynn", "伊芙琳", "痛苦之拥", "Assassin", "B", 49.3, 3.7, "高"),
                createHero(32, "Ezreal", "伊泽瑞尔", "探险家", "Marksman", "A", 50.6, 15.2, "中"),
                createHero(33, "Fiddlesticks", "费德提克", "远古恐惧", "Mage", "S", 52.7, 5.4, "中"),
                createHero(34, "Fiora", "菲奥娜", "无双剑姬", "Fighter", "B", 48.3, 3.2, "高"),
                createHero(35, "Fizz", "菲兹", "潮汐海灵", "Assassin", "A", 50.1, 5.8, "中"),
                createHero(36, "Galio", "加里奥", "正义巨像", "Tank", "A", 50.8, 4.1, "中"),
                createHero(37, "Gangplank", "普朗克", "海洋之灾", "Fighter", "B", 48.9, 3.4, "高"),
                createHero(38, "Garen", "盖伦", "德玛西亚之力", "Fighter", "S+", 53.5, 10.2, "低"),
                createHero(39, "Gnar", "纳尔", "迷失之牙", "Fighter", "B", 49.2, 3.6, "高"),
                createHero(40, "Gragas", "古拉加斯", "酒桶", "Mage", "A", 50.3, 6.5, "中"),
                createHero(41, "Graves", "格雷福斯", "法外狂徒", "Marksman", "B", 49.2, 4.7, "中"),
                createHero(42, "Gwen", "格温", "灵罗娃娃", "Fighter", "B", 48.7, 3.1, "高"),
                createHero(43, "Hecarim", "赫卡里姆", "战争之影", "Fighter", "A", 50.5, 5.9, "中"),
                createHero(44, "Heimerdinger", "黑默丁格", "大发明家", "Mage", "S", 52.4, 4.6, "中"),
                createHero(45, "Hwei", "慧", "百相学家", "Mage", "A", 50.7, 5.4, "高"),
                createHero(46, "Illaoi", "俄洛伊", "海兽祭司", "Fighter", "A", 51.2, 5.3, "中"),
                createHero(47, "Irelia", "艾瑞莉娅", "刀锋舞者", "Fighter", "B", 48.4, 4.9, "高"),
                createHero(48, "Ivern", "艾翁", "翠神", "Support", "B", 49.5, 1.8, "高"),
                createHero(49, "Janna", "迦娜", "风暴之怒", "Support", "A", 50.9, 4.4, "中"),
                createHero(50, "JarvanIV", "嘉文四世", "德玛西亚皇子", "Fighter", "A", 50.6, 5.8, "中"),
                createHero(51, "Jax", "贾克斯", "武器大师", "Fighter", "A", 50.7, 6.8, "中"),
                createHero(52, "Jayce", "杰斯", "未来守护者", "Fighter", "B", 48.5, 3.9, "高"),
                createHero(53, "Jhin", "烬", "戏命师", "Marksman", "S", 52.3, 14.5, "中"),
                createHero(54, "Jinx", "金克丝", "暴走萝莉", "Marksman", "S", 52.0, 8.3, "低"),
                createHero(55, "Kaisa", "卡莎", "虚空之女", "Marksman", "A", 50.4, 9.5, "中"),
                createHero(56, "Kalista", "卡莉丝塔", "复仇之矛", "Marksman", "B", 48.3, 2.1, "高"),
                createHero(57, "Karma", "卡尔玛", "天启者", "Mage", "A", 50.6, 3.8, "低"),
                createHero(58, "Karthus", "卡尔萨斯", "死亡颂唱者", "Mage", "S+", 54.5, 6.1, "低"),
                createHero(59, "Kassadin", "卡萨丁", "虚空行者", "Assassin", "A", 50.8, 4.2, "中"),
                createHero(60, "Katarina", "卡特琳娜", "不祥之刃", "Assassin", "S", 52.6, 10.7, "高"),
                createHero(61, "Kayle", "凯尔", "正义天使", "Fighter", "S", 52.9, 5.7, "低"),
                createHero(62, "Kayn", "凯隐", "影流之镰", "Assassin", "A", 50.2, 7.4, "高"),
                createHero(63, "Kennen", "凯南", "狂暴之心", "Mage", "A", 50.5, 3.3, "中"),
                createHero(64, "Khazix", "卡兹克", "虚空掠夺者", "Assassin", "A", 50.9, 5.5, "中"),
                createHero(65, "Kindred", "千珏", "永猎双子", "Marksman", "B", 48.6, 2.4, "高"),
                createHero(66, "Kled", "克烈", "暴怒骑士", "Fighter", "B", 49.1, 2.9, "中"),
                createHero(67, "KogMaw", "克格莫", "深渊巨口", "Marksman", "A", 51.1, 5.2, "低"),
                createHero(68, "Leblanc", "乐芙兰", "诡术妖姬", "Assassin", "B", 48.2, 3.6, "高"),
                createHero(69, "LeeSin", "李青", "盲僧", "Fighter", "A", 50.3, 11.2, "高"),
                createHero(70, "Leona", "蕾欧娜", "曙光女神", "Tank", "S", 52.2, 7.8, "低"),
                createHero(71, "Lillia", "莉莉娅", "含羞蓓蕾", "Mage", "S", 52.8, 6.3, "中"),
                createHero(72, "Lissandra", "丽桑卓", "冰霜女巫", "Mage", "A", 50.4, 3.1, "中"),
                createHero(73, "Lucian", "卢锡安", "圣枪游侠", "Marksman", "A", 50.1, 8.9, "中"),
                createHero(74, "Lulu", "璐璐", "仙灵女巫", "Support", "A", 50.8, 5.7, "低"),
                createHero(75, "Lux", "拉克丝", "光辉女郎", "Mage", "S+", 54.8, 15.3, "低"),
                createHero(76, "Malphite", "墨菲特", "熔岩巨兽", "Tank", "S+", 53.8, 9.5, "低"),
                createHero(77, "Malzahar", "玛尔扎哈", "荒漠屠夫", "Mage", "S", 52.6, 5.1, "低"),
                createHero(78, "Maokai", "茂凯", "扭曲树精", "Tank", "S", 52.3, 6.2, "低"),
                createHero(79, "MasterYi", "易", "无极剑圣", "Assassin", "S+", 54.1, 12.8, "低"),
                createHero(80, "Milio", "米利欧", "明烛", "Support", "A", 50.6, 3.9, "低"),
                createHero(81, "MissFortune", "厄运小姐", "赏金猎人", "Marksman", "S", 52.7, 11.4, "低"),
                createHero(82, "Mordekaiser", "莫德凯撒", "铁铠冥魂", "Fighter", "S", 52.4, 7.6, "低"),
                createHero(83, "Morgana", "莫甘娜", "堕落天使", "Mage", "S", 52.1, 8.7, "低"),
                createHero(84, "Naafiri", "纳亚菲利", "百裂狂犬", "Assassin", "B", 49.3, 2.8, "中"),
                createHero(85, "Nami", "娜美", "唤潮鲛姬", "Support", "A", 50.7, 5.3, "低"),
                createHero(86, "Nasus", "内瑟斯", "沙漠死神", "Fighter", "S", 52.5, 6.9, "低"),
                createHero(87, "Nautilus", "诺提勒斯", "深海泰坦", "Tank", "A", 50.9, 7.2, "低"),
                createHero(88, "Neeko", "妮蔻", "万花通灵", "Mage", "A", 50.3, 3.5, "中"),
                createHero(89, "Nidalee", "奈德丽", "狂野女猎手", "Assassin", "B", 48.1, 2.3, "高"),
                createHero(90, "Nilah", "妮拉", "不羁之悦", "Marksman", "B", 48.7, 2.6, "高"),
                createHero(91, "Nocturne", "魔腾", "永恒梦魇", "Assassin", "S", 52.9, 7.1, "低"),
                createHero(92, "Nunu", "努努和威朗普", "雪原双子", "Tank", "A", 50.5, 4.7, "低"),
                createHero(93, "Olaf", "奥拉夫", "狂战士", "Fighter", "A", 50.8, 5.4, "低"),
                createHero(94, "Orianna", "奥莉安娜", "发条魔灵", "Mage", "A", 50.2, 4.8, "高"),
                createHero(95, "Ornn", "奥恩", "山隐之焰", "Tank", "A", 50.6, 4.3, "中"),
                createHero(96, "Pantheon", "潘森", "不屈之枪", "Fighter", "A", 50.4, 5.1, "中"),
                createHero(97, "Poppy", "波比", "圣锤之毅", "Tank", "S", 52.1, 4.9, "低"),
                createHero(98, "Pyke", "派克", "血港鬼影", "Support", "S", 52.4, 9.3, "中"),
                createHero(99, "Qiyana", "奇亚娜", "元素女皇", "Assassin", "B", 48.6, 3.4, "高"),
                createHero(100, "Quinn", "奎因", "德玛西亚之翼", "Marksman", "B", 49.1, 2.5, "中"),
                createHero(101, "Rakan", "洛", "幻翎", "Support", "A", 50.5, 4.6, "高"),
                createHero(102, "Rammus", "拉莫斯", "披甲龙龟", "Tank", "S+", 53.6, 7.4, "低"),
                createHero(103, "RekSai", "雷克塞", "虚空遁地兽", "Fighter", "B", 48.9, 2.1, "高"),
                createHero(104, "Rell", "芮尔", "镕铁少女", "Tank", "A", 50.3, 2.8, "中"),
                createHero(105, "Renata", "蕾娜塔", "炼金男爵", "Support", "B", 49.2, 2.3, "高"),
                createHero(106, "Renekton", "雷克顿", "荒漠屠夫", "Fighter", "B", 48.4, 3.7, "低"),
                createHero(107, "Rengar", "雷恩加尔", "傲之追猎者", "Assassin", "B", 49.1, 4.8, "高"),
                createHero(108, "Riven", "锐雯", "放逐之刃", "Fighter", "B", 48.7, 5.3, "高"),
                createHero(109, "Rumble", "兰博", "机械公敌", "Mage", "S", 52.3, 4.1, "中"),
                createHero(110, "Ryze", "瑞兹", "符文法师", "Mage", "B", 48.3, 2.6, "高"),
                createHero(111, "Samira", "莎弥拉", "沙漠玫瑰", "Marksman", "B", 49.3, 5.7, "高"),
                createHero(112, "Sejuani", "瑟庄妮", "北地之怒", "Tank", "A", 50.4, 3.2, "中"),
                createHero(113, "Senna", "赛娜", "涤魂圣枪", "Support", "A", 50.6, 7.1, "中"),
                createHero(114, "Seraphine", "萨勒芬妮", "星籁歌姬", "Mage", "S", 52.6, 6.8, "低"),
                createHero(115, "Sett", "瑟提", "腕豪", "Fighter", "A", 50.8, 6.4, "中"),
                createHero(116, "Shaco", "萨科", "恶魔小丑", "Assassin", "S+", 53.9, 7.5, "中"),
                createHero(117, "Shen", "慎", "暮光之眼", "Tank", "A", 50.5, 4.3, "中"),
                createHero(118, "Shyvana", "希瓦娜", "龙血武姬", "Fighter", "S", 52.1, 5.6, "低"),
                createHero(119, "Singed", "辛吉德", "炼金术士", "Tank", "S", 52.4, 3.8, "中"),
                createHero(120, "Sion", "赛恩", "亡灵战神", "Tank", "A", 50.7, 5.2, "低"),
                createHero(121, "Sivir", "希维尔", "战争女神", "Marksman", "A", 50.3, 5.4, "低"),
                createHero(122, "Skarner", "斯卡纳", "水晶先锋", "Fighter", "B", 49.2, 2.7, "中"),
                createHero(123, "Smolder", "斯莫德", "焰尾", "Marksman", "A", 50.5, 4.2, "中"),
                createHero(124, "Sona", "娑娜", "琴瑟仙女", "Support", "S+", 54.3, 6.3, "低"),
                createHero(125, "Soraka", "索拉卡", "众星之子", "Support", "S", 52.5, 7.1, "低"),
                createHero(126, "Swain", "斯维因", "诺克萨斯统领", "Mage", "S", 52.8, 5.9, "中"),
                createHero(127, "Sylas", "塞拉斯", "解脱者", "Mage", "A", 50.4, 6.7, "高"),
                createHero(128, "Syndra", "辛德拉", "暗黑元首", "Mage", "A", 50.1, 4.5, "高"),
                createHero(129, "TahmKench", "塔姆", "河流之王", "Tank", "A", 50.6, 3.9, "低"),
                createHero(130, "Taliyah", "塔莉垭", "岩雀", "Mage", "B", 48.8, 2.4, "高"),
                createHero(131, "Talon", "泰隆", "刀锋之影", "Assassin", "B", 49.2, 3.8, "高"),
                createHero(132, "Taric", "塔里克", "宝石骑士", "Support", "A", 50.4, 2.1, "中"),
                createHero(133, "Teemo", "提莫", "迅捷斥候", "Marksman", "S+", 54.6, 10.3, "低"),
                createHero(134, "Thresh", "锤石", "魂锁典狱长", "Support", "A", 50.3, 8.5, "中"),
                createHero(135, "Tristana", "崔丝塔娜", "麦林炮手", "Marksman", "A", 50.7, 5.8, "低"),
                createHero(136, "Trundle", "特朗德尔", "巨魔之王", "Fighter", "S", 52.3, 5.1, "低"),
                createHero(137, "Tryndamere", "泰达米尔", "蛮族之王", "Fighter", "B", 49.3, 4.2, "低"),
                createHero(138, "TwistedFate", "崔斯特", "卡牌大师", "Mage", "A", 50.5, 4.7, "中"),
                createHero(139, "Twitch", "图奇", "瘟疫之源", "Marksman", "S", 52.6, 7.3, "中"),
                createHero(140, "Udyr", "乌迪尔", "兽灵行者", "Fighter", "A", 50.8, 4.5, "中"),
                createHero(141, "Urgot", "厄加特", "无畏战车", "Fighter", "A", 50.4, 3.8, "中"),
                createHero(142, "Varus", "韦鲁斯", "惩戒之箭", "Marksman", "A", 50.2, 6.1, "中"),
                createHero(143, "Vayne", "薇恩", "暗夜猎手", "Marksman", "A", 50.5, 9.2, "中"),
                createHero(144, "Veigar", "维迦", "邪恶小法师", "Mage", "S+", 53.7, 8.1, "低"),
                createHero(145, "Velkoz", "维克兹", "虚空之眼", "Mage", "S", 52.4, 4.3, "中"),
                createHero(146, "Vex", "薇古丝", "愁云使者", "Mage", "A", 50.6, 4.8, "中"),
                createHero(147, "Vi", "蔚", "皮城执法官", "Fighter", "A", 50.9, 5.6, "中"),
                createHero(148, "Viego", "佛耶戈", "破败之王", "Assassin", "A", 50.3, 6.3, "高"),
                createHero(149, "Viktor", "维克托", "机械先驱", "Mage", "A", 50.4, 4.1, "中"),
                createHero(150, "Vladimir", "弗拉基米尔", "猩红收割者", "Mage", "S", 52.2, 6.5, "中"),
                createHero(151, "Volibear", "沃利贝尔", "雷霆咆哮", "Fighter", "A", 50.7, 4.8, "低"),
                createHero(152, "Warwick", "沃里克", "嗜血猎手", "Fighter", "S", 52.8, 6.7, "低"),
                createHero(153, "Wukong", "孙悟空", "齐天大圣", "Fighter", "A", 50.5, 4.3, "低"),
                createHero(154, "Xayah", "霞", "逆羽", "Marksman", "A", 50.3, 4.6, "中"),
                createHero(155, "Xerath", "泽拉斯", "远古巫灵", "Mage", "S", 52.1, 5.8, "低"),
                createHero(156, "XinZhao", "赵信", "德邦总管", "Fighter", "A", 50.6, 5.3, "低"),
                createHero(157, "Yasuo", "亚索", "疾风剑豪", "Fighter", "A", 50.4, 13.7, "高"),
                createHero(158, "Yone", "永恩", "封魔剑魂", "Fighter", "A", 50.2, 9.8, "高"),
                createHero(159, "Yorick", "约里克", "牧魂人", "Fighter", "S", 52.3, 3.9, "低"),
                createHero(160, "Yuumi", "悠米", "魔法猫咪", "Support", "B", 48.5, 3.2, "低"),
                createHero(161, "Zac", "扎克", "生化魔人", "Tank", "S", 52.5, 4.7, "中"),
                createHero(162, "Zed", "劫", "影流之主", "Assassin", "B", 48.9, 8.1, "高"),
                createHero(163, "Zeri", "泽丽", "祖安花火", "Marksman", "B", 48.2, 3.5, "高"),
                createHero(164, "Ziggs", "吉格斯", "爆破鬼才", "Mage", "S", 52.7, 6.2, "低"),
                createHero(165, "Zilean", "基兰", "时光守护者", "Support", "S+", 53.4, 4.1, "低"),
                createHero(166, "Zoe", "佐伊", "星界女神", "Mage", "B", 49.1, 3.7, "高"),
                createHero(167, "Zyra", "婕拉", "荆棘之兴", "Mage", "S", 52.9, 6.8, "低")
        );

        // 逐条插入到 tb_hero 表
        // forEach + 方法引用是 Java 8 的简洁写法，等价于 heroes.forEach(h -> heroMapper.insert(h))
        heroes.forEach(heroMapper::insert);
    }

    /**
     * 创建单个英雄实体 —— 种子数据的工厂方法
     *
     * ══════════════════════════════════════════════════════════════
     * 参数说明
     * ══════════════════════════════════════════════════════════════
     *
     * @param riotId          Riot Games 官方英雄 ID（如 Aatrox = 266，这里简化为序号）
     * @param nameEn          英文名（如 "Aatrox"），用于拼接图片 URL
     * @param nameZh          中文名（如 "亚托克斯"），用于前端显示
     * @param title           称号（如 "暗裔剑魔"），英雄的副标题
     * @param role            角色定位（Fighter/Mage/Assassin/Tank/Marksman/Support）
     * @param tier            梯级评级（S+/S/A/B），表示英雄在 ARAM 中的强度
     * @param winRate         胜率（如 51.2 表示 51.2%）
     * @param pickRate        选取率（如 8.5 表示 8.5%）
     * @param confidenceLevel 置信度等级（高/中/低），表示统计数据的样本量可靠性
     * @return 完整的 Hero 实体对象
     */
    private Hero createHero(int riotId, String nameEn, String nameZh, String title,
                            String role, String tier, double winRate,
                            double pickRate, String confidenceLevel) {
        Hero hero = new Hero();
        hero.setRiotId(riotId);                                    // Riot 官方 ID
        hero.setNameEn(nameEn);                                    // 英文名
        hero.setNameZh(nameZh);                                    // 中文名
        hero.setTitle(title);                                      // 称号
        hero.setRole(role);                                        // 角色定位
        hero.setImageUrl(String.format("https://ddragon.leagueoflegends.com/cdn/14.10/img/champion/%s.png", nameEn));     // 图片 URL，使用 Riot 官方 CDN
        hero.setTier(tier);                                        // 梯级评级
        hero.setWinRate(BigDecimal.valueOf(winRate));              // 胜率
        hero.setPickRate(BigDecimal.valueOf(pickRate));            // 选取率
        hero.setConfidenceLevel(confidenceLevel);                  // 置信度
        hero.setDescription(generateDescription(nameZh, title, role));  // 自动生成的描述
        hero.setSkills(generateSkills(nameEn, role));              // 按角色生成的技能列表
        hero.setCounterTips(COUNTER_TIPS_BY_ROLE.getOrDefault(role, List.of()));  // 按角色查找克制建议
        hero.setSynergies(SYNERGIES_BY_ROLE.getOrDefault(role, List.of()));        // 按角色查找搭配建议
        hero.setAvgKills(generateAvgKills(role));                  // 按角色生成的场均击杀
        hero.setAvgDeaths(generateAvgDeaths(role));                // 按角色生成的场均死亡
        hero.setAvgAssists(generateAvgAssists(role));              // 按角色生成的场均助攻
        hero.setRecommendedBuild(RECOMMENDED_BUILD_BY_ROLE.getOrDefault(role, "通用出装路线"));  // 推荐出装
        hero.setVersion("14.10");                                  // 数据版本号
        hero.setUpdatedAt(LocalDateTime.now());                    // 更新时间
        return hero;
    }

    /**
     * 生成英雄描述文本
     *
     * 格式："{中文名}，{称号}。在ARAM模式中定位为{角色中文}，需要根据队伍阵容灵活调整打法。合理利用技能组合和站位是取胜关键。"
     * 示例："亚托克斯，暗裔剑魔。在ARAM模式中定位为战士，需要根据队伍阵容灵活调整打法。合理利用技能组合和站位是取胜关键。"
     *
     * @param nameZh 中文名
     * @param title  称号
     * @param role   角色定位（英文）
     * @return 描述文本
     */
    private String generateDescription(String nameZh, String title, String role) {
        return nameZh + "，" + title + "。在ARAM模式中定位为" + getRoleZh(role) +
                "，需要根据队伍阵容灵活调整打法。合理利用技能组合和站位是取胜关键。";
    }

    /**
     * 将英文角色定位转换为中文
     *
     * @param role 英文角色定位（如 "Fighter"）
     * @return 中文角色定位（如 "战士"）
     */
    private String getRoleZh(String role) {
        return switch (role) {
            case "Fighter" -> "战士";
            case "Mage" -> "法师";
            case "Assassin" -> "刺客";
            case "Tank" -> "坦克";
            case "Marksman" -> "射手";
            case "Support" -> "辅助";
            default -> role;
        };
    }

    /**
     * 生成英雄技能列表（5个技能：被动 + Q/W/E/R）
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么是"生成"而不是"真实数据"？
     * ══════════════════════════════════════════════════════════════
     *
     * 种子数据中的技能名称和描述是按角色模板生成的通用内容，
     * 不是每个英雄的真实技能。这是因为：
     * - 167 个英雄的真实技能数据量太大，不适合硬编码
     * - 真实技能数据会由 DataSyncScheduler 从 Riot DataDragon 同步
     * - 种子数据只需要保证应用首次启动时有可展示的内容
     *
     * @param nameEn 英雄英文名（预留参数，当前未使用）
     * @param role   角色定位
     * @return 5个技能的数据列表
     */
    private List<Hero.SkillData> generateSkills(String nameEn, String role) {
        List<Hero.SkillData> skills = new ArrayList<>();
        String[] keys = {"P", "Q", "W", "E", "R"};                          // 技能按键
        String[] names = generateSkillNames(nameEn, role);                    // 技能名称
        String[] descriptions = generateSkillDescriptions(role);              // 技能描述
        for (int i = 0; i < 5; i++) {
            Hero.SkillData skill = new Hero.SkillData();
            skill.setKey(keys[i]);                                            // P/Q/W/E/R
            skill.setName(names[i]);                                          // 技能名称
            skill.setDescription(descriptions[i]);                            // 技能描述
            skills.add(skill);
        }
        return skills;
    }

    /**
     * 按角色生成技能名称模板
     *
     * 每个角色有5个技能名称（被动+Q/W/E/R），按角色类型使用不同的名称模板。
     *
     * @param nameEn 英雄英文名（预留参数）
     * @param role   角色定位
     * @return 5个技能名称的数组
     */
    private String[] generateSkillNames(String nameEn, String role) {
        return switch (role) {
            case "Fighter" -> new String[]{"战斗本能", "突进斩击", "防御姿态", "战意冲锋", "终极裁决"};
            case "Mage" -> new String[]{"魔力涌动", "能量弹射", "法力护盾", "空间位移", "毁灭魔法"};
            case "Assassin" -> new String[]{"暗影步", "致命突刺", "隐匿之雾", "疾风步", "暗杀标记"};
            case "Tank" -> new String[]{"坚韧体魄", "重击", "护盾壁垒", "嘲讽冲锋", "不灭意志"};
            case "Marksman" -> new String[]{"精准射击", "穿透箭矢", "快速闪避", "陷阱布置", "弹幕风暴"};
            case "Support" -> new String[]{"生命祝福", "治愈之光", "护盾庇护", "加速光环", "群体治疗"};
            default -> new String[]{"被动技能", "技能Q", "技能W", "技能E", "技能R"};
        };
    }

    /**
     * 按角色生成技能描述模板
     *
     * 每个角色有5个技能描述，描述了技能的效果机制。
     * 注意：这些是通用模板，不是英雄的真实技能描述。
     *
     * @param role 角色定位
     * @return 5个技能描述的数组
     */
    private String[] generateSkillDescriptions(String role) {
        return switch (role) {
            case "Fighter" -> new String[]{
                    "每次攻击或受到攻击时获得攻击力加成，最多叠加5层",
                    "向前方突进并对路径上的敌人造成物理伤害",
                    "激活后获得护盾，持续3秒，期间减少受到的伤害",
                    "向目标方向冲锋，击飞沿途敌人0.5秒",
                    "对大范围内敌人造成物理伤害，已损失生命值越高伤害越高"
            };
            case "Mage" -> new String[]{
                    "施放技能后获得移动速度加成，持续2秒",
                    "发射能量弹，命中敌人后造成魔法伤害并弹射至附近敌人",
                    "创造法力护盾，吸收即将到来的伤害",
                    "短距离传送至目标位置，留下残影迷惑敌人",
                    "在目标区域释放毁灭性魔法，造成大量AOE魔法伤害"
            };
            case "Assassin" -> new String[]{
                    "脱离战斗后获得移动速度加成和穿透效果",
                    "对目标发动快速突刺，造成物理伤害并标记敌人",
                    "释放迷雾遮蔽自身，进入隐身状态1秒",
                    "向目标方向快速冲刺，穿过敌人造成伤害",
                    "标记目标后发动致命一击，目标已损失生命值越高伤害越高"
            };
            case "Tank" -> new String[]{
                    "受到伤害时获得护甲和魔抗加成，持续6秒",
                    "对前方敌人造成物理伤害并减速30%",
                    "激活护盾，吸收伤害并反弹部分伤害给攻击者",
                    "向目标冲锋并嘲讽周围敌人1秒，强制攻击自己",
                    "获得大量生命值和伤害减免，持续8秒，期间无法被击杀"
            };
            case "Marksman" -> new String[]{
                    "连续攻击同一目标时攻击速度逐渐提升",
                    "发射穿透箭矢，对直线上的敌人造成物理伤害",
                    "快速翻滚闪避，重置普攻计时器",
                    "在地面放置陷阱，触发后减速并暴露敌人",
                    "向前方扇形区域发射弹幕，造成大量物理伤害"
            };
            case "Support" -> new String[]{
                    "附近友军获得生命回复加成",
                    "发射治愈光束，为友军回复生命值",
                    "为目标友军施加护盾，吸收伤害持续4秒",
                    "提升附近友军移动速度30%，持续3秒",
                    "大范围治疗所有友军，并清除一个负面效果"
            };
            default -> new String[]{"被动效果", "主动技能Q", "主动技能W", "主动技能E", "终极技能R"};
        };
    }

    /**
     * 按角色生成场均击杀数（含随机波动）
     *
     * 不同角色的场均击杀数基准不同：
     * - 刺客（7.5~10.5）：最高，因为刺客就是负责击杀的
     * - 法师（6.0~9.0）：较高，AOE 伤害容易收割
     * - 战士（5.5~8.5）：中等，近战输出
     * - 射手（5.0~8.0）：中等，持续输出
     * - 坦克（3.5~5.5）：较低，主要不是输出
     * - 辅助（2.0~4.0）：最低，主要不是击杀
     *
     * @param role 角色定位
     * @return 场均击杀数
     */
    private BigDecimal generateAvgKills(String role) {
        return switch (role) {
            case "Assassin" -> BigDecimal.valueOf(7.5 + Math.random() * 3);
            case "Mage" -> BigDecimal.valueOf(6.0 + Math.random() * 3);
            case "Fighter" -> BigDecimal.valueOf(5.5 + Math.random() * 3);
            case "Marksman" -> BigDecimal.valueOf(5.0 + Math.random() * 3);
            case "Tank" -> BigDecimal.valueOf(3.5 + Math.random() * 2);
            case "Support" -> BigDecimal.valueOf(2.0 + Math.random() * 2);
            default -> BigDecimal.valueOf(5.0);
        };
    }

    /**
     * 按角色生成场均死亡数（含随机波动）
     *
     * 不同角色的场均死亡数基准不同：
     * - 刺客（5.5~7.5）：最高，近身输出容易被反杀
     * - 射手（5.0~7.0）：较高，脆皮容易被秒
     * - 法师（4.5~6.5）：中等
     * - 战士（4.0~6.0）：中等
     * - 坦克（3.5~5.5）：较低，肉不容易死
     * - 辅助（4.0~6.0）：中等
     *
     * @param role 角色定位
     * @return 场均死亡数
     */
    private BigDecimal generateAvgDeaths(String role) {
        return switch (role) {
            case "Assassin" -> BigDecimal.valueOf(5.5 + Math.random() * 2);
            case "Marksman" -> BigDecimal.valueOf(5.0 + Math.random() * 2);
            case "Mage" -> BigDecimal.valueOf(4.5 + Math.random() * 2);
            case "Fighter" -> BigDecimal.valueOf(4.0 + Math.random() * 2);
            case "Tank" -> BigDecimal.valueOf(3.5 + Math.random() * 2);
            case "Support" -> BigDecimal.valueOf(4.0 + Math.random() * 2);
            default -> BigDecimal.valueOf(4.5);
        };
    }

    /**
     * 按角色生成场均助攻数（含随机波动）
     *
     * 不同角色的场均助攻数基准不同：
     * - 辅助（10.0~14.0）：最高，辅助就是帮助队友的
     * - 坦克（8.0~11.0）：较高，开团和控制贡献助攻
     * - 法师（7.0~10.0）：中等，AOE 伤害蹭助攻
     * - 战士（6.0~9.0）：中等
     * - 刺客（5.0~8.0）：较低，主要追求击杀而非助攻
     * - 射手（5.0~7.0）：最低，独立输出
     *
     * @param role 角色定位
     * @return 场均助攻数
     */
    private BigDecimal generateAvgAssists(String role) {
        return switch (role) {
            case "Support" -> BigDecimal.valueOf(10.0 + Math.random() * 4);
            case "Tank" -> BigDecimal.valueOf(8.0 + Math.random() * 3);
            case "Mage" -> BigDecimal.valueOf(7.0 + Math.random() * 3);
            case "Fighter" -> BigDecimal.valueOf(6.0 + Math.random() * 3);
            case "Assassin" -> BigDecimal.valueOf(5.0 + Math.random() * 3);
            case "Marksman" -> BigDecimal.valueOf(5.0 + Math.random() * 2);
            default -> BigDecimal.valueOf(6.0);
        };
    }
}
