package com.aram.mayhem.initializer;

import com.aram.mayhem.entity.Bulletin;
import com.aram.mayhem.mapper.BulletinMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告数据初始化器 —— 应用启动时的"公告种子数据播种机"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与 DataInitializer 和 AugmentDataInitializer 类似，本类实现了 CommandLineRunner 接口，
 * 在应用启动完成后自动执行，检查 tb_bulletin 表是否为空，如果为空则插入种子公告数据。
 *
 * 打个比方：
 * - DataInitializer 是"英雄播种机"
 * - AugmentDataInitializer 是"符文播种机"
 * - 本类是"公告播种机"——播下初始公告，让用户首次打开 App 时能看到内容
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、公告类型
 * ═══════════════════════════════════════════════════════════════════
 *
 * 种子公告包含3种类型：
 * - version：版本更新公告（如 "v1.0.0 正式上线"）
 * - event：活动公告（如 "ARAM 模式周免英雄活动"）
 * - notice：通知公告（如 "数据同步完成通知"）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、公告内容格式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告内容使用 HTML 格式，支持：
 * - 段落：<p>...</p>
 * - 列表：<ul><li>...</li></ul>
 * - Emoji：✨🎯 等 Unicode 字符
 *
 * 前端使用 WebView 或 HTML 解析器渲染公告内容。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、置顶机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * isPinned 字段控制公告是否置顶：
 * - 1：置顶（显示在公告列表最前面）
 * - 0：不置顶（按发布时间排序）
 *
 * 种子数据中只有"v1.0.0 正式上线"被置顶，其他公告不置顶。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Bulletin → 公告实体类，对应 tb_bulletin 表
 * - BulletinMapper → MyBatis-Plus Mapper，提供 CRUD 操作
 * - DataInitializer → 英雄数据初始化器
 * - AugmentDataInitializer → 符文数据初始化器
 */
@Component
public class BulletinDataInitializer implements CommandLineRunner {

    /**
     * BulletinMapper —— MyBatis-Plus 提供的公告表 CRUD 操作接口
     *
     * 主要使用的方法：
     * - selectCount(null)：查询表中总记录数
     * - insert(bulletin)：插入一条公告记录
     */
    private final BulletinMapper bulletinMapper;

    /**
     * 构造函数 —— Spring 自动注入 BulletinMapper
     *
     * @param bulletinMapper 公告表 Mapper
     */
    public BulletinDataInitializer(BulletinMapper bulletinMapper) {
        this.bulletinMapper = bulletinMapper;
    }

    /**
     * 应用启动后自动执行 —— 检查并初始化公告种子数据
     *
     * ══════════════════════════════════════════════════════════════
     * 执行流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 检查 tb_bulletin 表是否已有数据
     * 2. 如果已有数据 → 直接返回（幂等性保证）
     * 3. 如果没有数据 → 创建4条种子公告：
     *    a. v1.0.0 正式上线（置顶）
     *    b. ARAM 模式周免英雄活动
     *    c. 数据同步完成通知
     *    d. v1.1.0 版本预告
     * 4. 逐条插入到数据库
     *
     * @param args Spring Boot 启动参数（本类不使用）
     */
    @Override
    @Transactional
    public void run(String... args) {
        // 幂等性检查：如果表中已有数据，跳过初始化
        if (bulletinMapper.selectCount(null) > 0) {
            return;
        }

        // 创建4条种子公告
        List<Bulletin> bulletins = List.of(
                createBulletin("version", "v1.0.0 正式上线",
                        "<p>ARAM Mayhem Assistant 正式上线啦！</p>" +
                        "<p>本次更新内容：</p>" +
                        "<ul><li>✨ 英雄数据全面接入</li>" +
                        "<li>✨ 海克斯符文推荐系统</li>" +
                        "<li>✨ 社区玩法分享功能</li>" +
                        "<li>✨ 实时公告推送</li></ul>",
                        "/images/bulletins/v1.0.0.png", 1),

                createBulletin("event", "ARAM 模式周免英雄活动",
                        "<p>本周 ARAM 模式周免英雄已更新！</p>" +
                        "<p>推荐尝试：亚托克斯、拉克丝、杰斯等强力英雄。</p>" +
                        "<p>快打开 App 查看最新英雄强度排行吧！</p>",
                        "/images/bulletins/weekly-rotation.png", 0),

                createBulletin("notice", "数据同步完成通知",
                        "<p>英雄数据与海克斯数据已完成最新版本同步。</p>" +
                        "<p>数据来源：Riot DataDragon + U.GG 统计</p>" +
                        "<p>更新时间：2024-05-22</p>",
                        "/images/bulletins/data-sync.png", 0),

                createBulletin("version", "v1.1.0 版本预告",
                        "<p>即将推出的新功能：</p>" +
                        "<ul><li>🎯 英雄克制关系图谱</li>" +
                        "<li>🎯 装备推荐系统</li>" +
                        "<li>🎯 战绩查询功能</li></ul>" +
                        "<p>敬请期待！</p>",
                        "/images/bulletins/v1.1.0-preview.png", 0)
        );

        // 逐条插入到 tb_bulletin 表
        bulletins.forEach(bulletinMapper::insert);
    }

    /**
     * 创建单个公告实体 —— 种子数据的工厂方法
     *
     * ══════════════════════════════════════════════════════════════
     * 参数说明
     * ══════════════════════════════════════════════════════════════
     *
     * @param type      公告类型（version/event/notice）
     * @param title     公告标题（如 "v1.0.0 正式上线"）
     * @param content   公告内容（HTML 格式，前端直接渲染）
     * @param imageUrl  公告封面图片 URL（如 "/images/bulletins/v1.0.0.png"）
     * @param isPinned  是否置顶（1=置顶，0=不置顶）
     * @return 完整的 Bulletin 实体对象
     */
    private Bulletin createBulletin(String type, String title, String content,
                                     String imageUrl, int isPinned) {
        Bulletin bulletin = new Bulletin();
        bulletin.setType(type);                                   // 公告类型
        bulletin.setTitle(title);                                 // 标题
        bulletin.setContent(content);                             // HTML 内容
        bulletin.setImageUrl(imageUrl);                           // 封面图片 URL
        bulletin.setIsPinned(isPinned);                           // 是否置顶
        bulletin.setPublishedAt(LocalDateTime.now());             // 发布时间
        return bulletin;
    }
}
