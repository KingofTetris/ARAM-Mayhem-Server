package com.aram.mayhem.initializer;

import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.HeroMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final HeroMapper heroMapper;

    public DataInitializer(HeroMapper heroMapper) {
        this.heroMapper = heroMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (heroMapper.selectCount(null) > 0) {
            return;
        }

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
                createHero(153, "Wukong", "孙悟空", "齐天大圣", "Fighter", "A", 50.5, 4.3, "中"),
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

        heroes.forEach(heroMapper::insert);
    }

    private Hero createHero(int riotId, String nameEn, String nameZh, String title,
                            String role, String tier, double winRate,
                            double pickRate, String confidenceLevel) {
        Hero hero = new Hero();
        hero.setRiotId(riotId);
        hero.setNameEn(nameEn);
        hero.setNameZh(nameZh);
        hero.setTitle(title);
        hero.setRole(role);
        hero.setImageUrl("/images/heroes/" + nameEn + ".png");
        hero.setTier(tier);
        hero.setWinRate(BigDecimal.valueOf(winRate));
        hero.setPickRate(BigDecimal.valueOf(pickRate));
        hero.setConfidenceLevel(confidenceLevel);
        hero.setVersion("14.10");
        hero.setUpdatedAt(LocalDateTime.now());
        return hero;
    }
}
