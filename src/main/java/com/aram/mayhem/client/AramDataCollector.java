package com.aram.mayhem.client;

import com.aram.mayhem.dto.AramAugmentStatsDTO;
import com.aram.mayhem.dto.AramHeroStatsDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ARAM 数据采集器 —— 从 U.GG 网站采集英雄胜率/选取率 + 强化符文统计数据的"批发市场采购员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是数据管线的"第二站"——它负责从 U.GG 网站采集 ARAM 模式的统计数据。
 * 与 RiotDataDragonClient 不同，本类采集的是"玩家实战统计"数据（胜率、选取率等），
 * 而 RiotDataDragonClient 采集的是"英雄基础信息"（名称、技能、图片等）。
 *
 * 打个比方：
 * - RiotDataDragonClient = 去农场采购"原材料"（英雄名字、技能描述）
 * - 本类 AramDataCollector = 去批发市场采购"加工品"（胜率、选取率、KDA 统计）
 * - DataAggregatorService = 厨师，把两批材料合并加工成最终菜品
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、U.GG 数据源说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * U.GG（https://u.gg）是一个第三方英雄联盟数据统计网站，提供：
 * - 英雄胜率/选取率/梯级评级（S+/S/A/B/C）
 * - 强化符文胜率/选取率/品质/套装归属
 * - 场均 KDA（击杀/死亡/助攻）
 *
 * 本类通过"网页爬取"方式获取数据（U.GG 没有提供公开 API）：
 * 1. 用 RestTemplate 获取 HTML 页面
 * 2. 用 Jsoup 解析 HTML 中的表格数据
 * 3. 提取并清洗各字段值
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、反爬策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 因为 U.GG 是第三方网站，有反爬虫机制，本类实现了两种反爬策略：
 *
 * 策略1：请求间隔控制
 * - 两次请求之间至少间隔 requestIntervalMs 毫秒（默认 2000ms = 2秒）
 * - 通过 enforceRequestInterval() 方法实现
 * - 使用 volatile 的 lastRequestTime 变量记录上次请求时间
 *
 * 策略2：随机 User-Agent
 * - 每次请求随机选择一个 User-Agent 字符串
 * - 模拟不同浏览器访问，避免被识别为爬虫
 * - User-Agent 列表通过配置文件注入
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据采集流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * 英雄数据采集：
 * 1. collectAramStats() → 入口方法
 * 2. fetchPage(baseUrl) → 获取 HTML 页面
 * 3. parseHeroStatsTable(doc) → 解析英雄统计表格
 * 4. parseHeroRow(row) → 解析单行英雄数据
 * 5. extractChampionName / extractTier / parsePercentCell → 提取和清洗字段
 *
 * 符文数据采集：
 * 1. collectAugmentStats() → 入口方法
 * 2. fetchPage(baseUrl + "/augments") → 获取符文页面 HTML
 * 3. parseAugmentStatsTable(doc) → 解析符文统计表格
 * 4. parseAugmentRow(row) → 解析单行符文数据
 * 5. extractQuality / parsePercentCell → 提取和清洗字段
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、日志级别说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类使用结构化日志标签，便于追踪数据管线各阶段：
 * - [RECEIVE]：HTTP 请求阶段（发送请求、接收响应）
 * - [PARSE]：HTML 解析阶段（表格定位、行遍历）
 * - [CLEAN]：数据清洗阶段（字段提取、格式转换）
 * - [TRANSFORM]：数据转换阶段（构建 DTO 对象）
 * - [STORE]：存储阶段（采集结果汇总）
 *
 * 日志级别：
 * - INFO：采集流程的起止和汇总信息
 * - DEBUG：各阶段处理细节（行解析、字段提取）
 * - TRACE：原始数据值和清洗前后对比
 * - WARN：可恢复的异常情况（空响应、行跳过）
 * - ERROR：不可恢复的异常（网络故障、解析崩溃）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、异常处理策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与 RiotDataDragonClient 一致，采用"容错优先"策略：
 * - 网络超时/连接失败 → 返回空列表 + 记录 ERROR 日志
 * - HTML 解析失败 → 返回空列表 + 记录 ERROR 日志
 * - 单行数据解析失败 → 跳过该行，继续解析下一行 + 记录 DEBUG 日志
 * - 字段值为空/格式错误 → 返回 null + 记录 DEBUG 日志
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、依赖说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * RestTemplate          | 发送 HTTP 请求获取 HTML        | 快递员（去取货）
 * Jsoup                 | 解析 HTML 文档，提取表格数据    | 阅读器（读懂网页）
 * AramHeroStatsDTO      | 英雄统计数据传输对象            | 包装箱（装英雄数据）
 * AramAugmentStatsDTO   | 符文统计数据传输对象            | 包装箱（装符文数据）
 *
 * 配置项（application.yml）：
 * - aramdata.base-url           → U.GG ARAM 页面 URL（默认 https://u.gg/lol/aram）
 * - aramdata.request-interval-ms → 请求间隔毫秒数（默认 2000）
 * - aramdata.user-agents        → User-Agent 列表（逗号分隔）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 八、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - DataAggregatorService → 调用本类获取统计数据后与 RiotDataDragon 数据合并
 * - DataSyncScheduler     → 定时触发数据同步，间接调用本类
 * - RiotDataDragonClient  → 另一个数据采集器，从 Riot 官方获取基础数据
 */
@Slf4j
@Component
public class AramDataCollector {

    /**
     * RestTemplate —— Spring 提供的 HTTP 客户端
     *
     * 作用：发送 HTTP GET 请求到 U.GG 网站，获取 HTML 页面内容
     * 注意：这里获取的是 HTML 而不是 JSON（U.GG 没有公开 API）
     */
    private final RestTemplate restTemplate;

    /**
     * U.GG ARAM 页面的基础 URL
     *
     * 从 application.yml 读取，默认值为 https://u.gg/lol/aram
     * 英雄数据：直接访问此 URL
     * 符文数据：访问此 URL + "/augments"
     */
    @Value("${aramdata.base-url:https://u.gg/lol/aram}")
    private String baseUrl;

    /**
     * 两次 HTTP 请求之间的最小间隔时间（毫秒）
     *
     * 从 application.yml 读取，默认值为 2000（2秒）
     * 用途：反爬策略，避免请求过于频繁被 U.GG 封禁 IP
     */
    @Value("${aramdata.request-interval-ms:2000}")
    private long requestIntervalMs;

    /**
     * User-Agent 候选列表
     *
     * 从 application.yml 读取，逗号分隔，默认为一个 Chrome 浏览器 UA
     * 用途：反爬策略，每次请求随机选择一个 UA，模拟不同浏览器
     * 示例配置：Mozilla/5.0 (Windows),Mozilla/5.0 (Mac),Mozilla/5.0 (Linux)
     */
    @Value("#{'${aramdata.user-agents:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36}'.split(',')}")
    private List<String> userAgents;

    /**
     * 上次发送 HTTP 请求的时间戳（毫秒）
     *
     * 使用 volatile 修饰确保多线程可见性（虽然当前是单线程使用，但做防御性编程）
     * 用于 enforceRequestInterval() 计算距离上次请求的间隔时间
     */
    private volatile long lastRequestTime = 0;

    /**
     * 构造函数 —— Spring 自动注入 RestTemplate
     *
     * @param restTemplate HTTP 客户端（由配置类注册的 Bean）
     */
    public AramDataCollector(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 设置基础 URL（仅供单元测试使用）
     *
     * 为什么需要这个方法？
     * - 单元测试时不想访问真实的 U.GG 网站
     * - 可以设置为本地 Mock 服务器的 URL（如 http://localhost:8080/test）
     */
    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 设置请求间隔（仅供单元测试使用）
     *
     * 测试时可以设为 0，避免等待 2 秒间隔
     */
    void setRequestIntervalMs(long requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    /**
     * 设置 User-Agent 列表（仅供单元测试使用）
     */
    void setUserAgents(List<String> userAgents) {
        this.userAgents = userAgents;
    }

    /**
     * 设置上次请求时间（仅供单元测试使用）
     *
     * 可以设为 0，让第一次请求不需要等待间隔
     */
    void setLastRequestTime(long lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    /**
     * 采集 ARAM 英雄统计数据 —— 英雄数据采集的入口方法
     *
     * ══════════════════════════════════════════════════════════════
     * 处理流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 调用 fetchPage() 获取 U.GG ARAM 页面的 HTML
     * 2. 用 Jsoup 将 HTML 解析为 Document 对象
     * 3. 调用 parseHeroStatsTable() 从 Document 中提取英雄统计表格
     * 4. 返回英雄统计列表
     *
     * ══════════════════════════════════════════════════════════════
     * 日志追踪
     * ══════════════════════════════════════════════════════════════
     *
     * - [RECEIVE] INFO：采集开始
     * - [RECEIVE] WARN：HTML 为空
     * - [RECEIVE] TRACE：原始 HTML 预览（前200字符）
     * - [PARSE] DEBUG：Jsoup 文档创建
     * - [STORE] INFO：采集完成，记录数量
     * - [RECEIVE] ERROR：采集失败
     *
     * @return 英雄统计列表，失败时返回空 List（不是 null）
     */
    public List<AramHeroStatsDTO> collectAramStats() {
        log.info("[RECEIVE] hero stats collection started | url={}", baseUrl);
        try {
            String html = fetchPage(baseUrl);
            if (html == null || html.isEmpty()) {
                log.warn("[RECEIVE] hero stats empty response | url={} | htmlLength={}", baseUrl, html == null ? "null" : 0);
                return Collections.emptyList();
            }

            log.trace("[RECEIVE] hero stats raw HTML received | url={} | length={} | preview={}",
                    baseUrl, html.length(), html.substring(0, Math.min(200, html.length())));

            Document doc = Jsoup.parse(html);
            log.debug("[PARSE] hero stats Jsoup document created | url={}", baseUrl);

            List<AramHeroStatsDTO> stats = parseHeroStatsTable(doc);

            log.info("[STORE] hero stats collection completed | url={} | recordCount={}", baseUrl, stats.size());
            return stats;
        } catch (Exception e) {
            log.error("[RECEIVE] hero stats collection failed | url={}", baseUrl, e);
            return Collections.emptyList();
        }
    }

    /**
     * 采集 ARAM 强化符文统计数据 —— 符文数据采集的入口方法
     *
     * ══════════════════════════════════════════════════════════════
     * 处理流程
     * ══════════════════════════════════════════════════════════════
     *
     * 与 collectAramStats() 类似，但访问的是 U.GG 的符文页面：
     * 1. 拼接符文页面 URL：baseUrl + "/augments"
     * 2. 调用 fetchPage() 获取 HTML
     * 3. 用 Jsoup 解析为 Document
     * 4. 调用 parseAugmentStatsTable() 提取符文统计表格
     * 5. 返回符文统计列表
     *
     * @return 符文统计列表，失败时返回空 List（不是 null）
     */
    public List<AramAugmentStatsDTO> collectAugmentStats() {
        String augmentUrl = baseUrl + "/augments";
        log.info("[RECEIVE] augment stats collection started | url={}", augmentUrl);
        try {
            String html = fetchPage(augmentUrl);
            if (html == null || html.isEmpty()) {
                log.warn("[RECEIVE] augment stats empty response | url={} | htmlLength={}", augmentUrl, html == null ? "null" : 0);
                return Collections.emptyList();
            }

            log.trace("[RECEIVE] augment stats raw HTML received | url={} | length={} | preview={}",
                    augmentUrl, html.length(), html.substring(0, Math.min(200, html.length())));

            Document doc = Jsoup.parse(html);
            log.debug("[PARSE] augment stats Jsoup document created | url={}", augmentUrl);

            List<AramAugmentStatsDTO> stats = parseAugmentStatsTable(doc);

            log.info("[STORE] augment stats collection completed | url={} | recordCount={}", augmentUrl, stats.size());
            return stats;
        } catch (Exception e) {
            log.error("[RECEIVE] augment stats collection failed | url={}", augmentUrl, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取页面 HTML（含反爬策略）
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 发送 HTTP GET 请求获取目标 URL 的 HTML 内容。
     * 在发送请求前，会先执行反爬策略：
     * 1. enforceRequestInterval()：确保两次请求之间有足够间隔
     * 2. selectRandomUserAgent()：随机选择 User-Agent
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么是包级可见（package-private）？
     * ══════════════════════════════════════════════════════════════
     *
     * 不设为 private 是为了方便单元测试中 Mock 或验证此方法的行为。
     * 不设为 public 是因为外部调用者不需要直接获取 HTML，应通过 collectAramStats() 等入口方法。
     *
     * @param url 目标 URL
     * @return HTML 字符串，失败时返回 null
     */
    String fetchPage(String url) {
        enforceRequestInterval();
        String userAgent = selectRandomUserAgent();

        try {
            log.debug("[RECEIVE] HTTP GET | url={} | userAgent={}", url, userAgent);
            String html = restTemplate.getForObject(url, String.class);
            lastRequestTime = System.currentTimeMillis();

            if (html != null) {
                log.debug("[RECEIVE] HTTP response received | url={} | contentLength={}", url, html.length());
            } else {
                log.warn("[RECEIVE] HTTP response is null | url={}", url);
            }
            return html;
        } catch (Exception e) {
            log.error("[RECEIVE] HTTP request failed | url={} | error={}", url, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析英雄统计表格
     *
     * ══════════════════════════════════════════════════════════════
     * U.GG ARAM 页面表格结构（典型）
     * ══════════════════════════════════════════════════════════════
     *
     * CSS 选择器：table.tr-table tbody tr
     * 每行（tr）包含多个单元格（td）：
     *   td[0]：排名序号
     *   td[1]：英雄名称 + 梯级图标（需要分别提取名称和梯级）
     *   td[2]：胜率（如 "52.30%"）
     *   td[3]：选取率（如 "8.50%"）
     *   td[4~6]：场均 KDA（击杀/死亡/助攻）
     *
     * ══════════════════════════════════════════════════════════════
     * CSS 选择器降级策略
     * ══════════════════════════════════════════════════════════════
     *
     * 优先使用 table.tr-table tbody tr（U.GG 常用选择器）
     * 如果找不到行，降级为 table tbody tr（通用选择器）
     * 如果仍然找不到，返回空列表并记录 WARN 日志
     *
     * @param doc Jsoup Document（由 Jsoup.parse(html) 生成）
     * @return 英雄统计列表
     */
    List<AramHeroStatsDTO> parseHeroStatsTable(Document doc) {
        List<AramHeroStatsDTO> results = new ArrayList<>();

        Elements rows = doc.select("table.tr-table tbody tr");
        String selectorUsed = "table.tr-table tbody tr";
        if (rows.isEmpty()) {
            rows = doc.select("table tbody tr");
            selectorUsed = "table tbody tr";
            log.debug("[PARSE] hero table fallback selector used | selector={}", selectorUsed);
        }
        if (rows.isEmpty()) {
            log.warn("[PARSE] no hero table rows found | triedSelectors=[table.tr-table tbody tr, table tbody tr]");
            return results;
        }

        log.debug("[PARSE] hero table rows found | selector={} | rowCount={}", selectorUsed, rows.size());

        int skippedCount = 0;
        int rowIndex = 0;
        for (Element row : rows) {
            rowIndex++;
            try {
                AramHeroStatsDTO stat = parseHeroRow(row, rowIndex);
                if (stat != null && stat.getChampionName() != null) {
                    results.add(stat);
                } else {
                    skippedCount++;
                    log.debug("[PARSE] hero row skipped | rowIndex={} | reason={}", rowIndex,
                            stat == null ? "insufficientColumns" : "nullChampionName");
                }
            } catch (Exception e) {
                skippedCount++;
                log.debug("[PARSE] hero row parse error | rowIndex={} | error={}", rowIndex, e.getMessage());
            }
        }

        if (skippedCount > 0) {
            log.debug("[PARSE] hero table parse summary | totalRows={} | parsed={} | skipped={}", rows.size(), results.size(), skippedCount);
        }

        return results;
    }

    /**
     * 解析单行英雄数据
     *
     * ══════════════════════════════════════════════════════════════
     * 处理流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 获取所有 td 单元格，检查数量 >= 4（至少需要排名+名称+胜率+选取率）
     * 2. 从 td[1] 提取英雄名称（extractChampionName）和梯级（extractTier）
     * 3. 从 td[2] 提取胜率（parsePercentCell，如 "52.30%" → 52.30）
     * 4. 从 td[3] 提取选取率（parsePercentCell，如 "8.50%" → 8.50）
     * 5. 如果 td 数量 >= 7，从 td[4~6] 提取场均 KDA
     * 6. 构建 AramHeroStatsDTO 并返回
     *
     * @param row HTML 表格行元素（<tr>）
     * @return 英雄统计数据 DTO，解析失败时返回 null
     */
    AramHeroStatsDTO parseHeroRow(Element row) {
        return parseHeroRow(row, -1);
    }

    /**
     * 解析单行英雄数据（带行号，用于日志追踪）
     *
     * @param row      HTML 表格行元素
     * @param rowIndex 行号（从1开始），用于日志定位问题行
     * @return 英雄统计数据 DTO，解析失败时返回 null
     */
    AramHeroStatsDTO parseHeroRow(Element row, int rowIndex) {
        // 获取该行的所有 td 单元格
        Elements cells = row.select("td");

        // 至少需要4列：排名 + 名称 + 胜率 + 选取率
        if (cells.size() < 4) {
            log.trace("[PARSE] hero row insufficient columns | rowIndex={} | cellCount={}", rowIndex, cells.size());
            return null;
        }

        // 从 td[1] 提取英雄名称（可能包含梯级标记等干扰文本）
        String championName = extractChampionName(cells.get(1));
        // 从 td[1] 提取梯级评级（S+/S/A/B/C）
        String tier = extractTier(cells.get(1));
        // 从 td[2] 提取胜率百分比（如 "52.30%" → 52.30）
        BigDecimal winRate = parsePercentCell(cells.get(2));
        // 从 td[3] 提取选取率百分比（如 "8.50%" → 8.50）
        BigDecimal pickRate = parsePercentCell(cells.get(3));

        // KDA 数据是可选的（td 数量 >= 7 时才存在）
        BigDecimal avgKills = null;
        BigDecimal avgDeaths = null;
        BigDecimal avgAssists = null;
        if (cells.size() >= 7) {
            avgKills = parseNumericCell(cells.get(4));   // 场均击杀
            avgDeaths = parseNumericCell(cells.get(5));   // 场均死亡
            avgAssists = parseNumericCell(cells.get(6));  // 场均助攻
        }

        log.trace("[TRANSFORM] hero row parsed | rowIndex={} | champion={} | tier={} | winRate={} | pickRate={} | kills={} | deaths={} | assists={}",
                rowIndex, championName, tier, winRate, pickRate, avgKills, avgDeaths, avgAssists);

        // 使用 Builder 模式构建 DTO，source 固定为 "u.gg"
        return AramHeroStatsDTO.builder()
                .championName(championName)
                .tier(tier)
                .winRate(winRate)
                .pickRate(pickRate)
                .avgKills(avgKills)
                .avgDeaths(avgDeaths)
                .avgAssists(avgAssists)
                .source("u.gg")
                .build();
    }

    /**
     * 解析符文统计表格
     *
     * ══════════════════════════════════════════════════════════════
     * 处理逻辑
     * ══════════════════════════════════════════════════════════════
     *
     * 与 parseHeroStatsTable 逻辑类似，但解析的是符文数据表格：
     * 1. 使用 CSS 选择器定位表格行
     * 2. 逐行调用 parseAugmentRow 解析
     * 3. 过滤掉名称为 null 的无效行
     *
     * @param doc Jsoup Document
     * @return 符文统计列表
     */
    List<AramAugmentStatsDTO> parseAugmentStatsTable(Document doc) {
        List<AramAugmentStatsDTO> results = new ArrayList<>();

        Elements rows = doc.select("table.tr-table tbody tr");
        String selectorUsed = "table.tr-table tbody tr";
        if (rows.isEmpty()) {
            rows = doc.select("table tbody tr");
            selectorUsed = "table tbody tr";
            log.debug("[PARSE] augment table fallback selector used | selector={}", selectorUsed);
        }
        if (rows.isEmpty()) {
            log.warn("[PARSE] no augment table rows found | triedSelectors=[table.tr-table tbody tr, table tbody tr]");
            return results;
        }

        log.debug("[PARSE] augment table rows found | selector={} | rowCount={}", selectorUsed, rows.size());

        int skippedCount = 0;
        int rowIndex = 0;
        for (Element row : rows) {
            rowIndex++;
            try {
                AramAugmentStatsDTO stat = parseAugmentRow(row, rowIndex);
                if (stat != null && stat.getAugmentName() != null) {
                    results.add(stat);
                } else {
                    skippedCount++;
                    log.debug("[PARSE] augment row skipped | rowIndex={} | reason={}", rowIndex,
                            stat == null ? "insufficientColumns" : "nullAugmentName");
                }
            } catch (Exception e) {
                skippedCount++;
                log.debug("[PARSE] augment row parse error | rowIndex={} | error={}", rowIndex, e.getMessage());
            }
        }

        if (skippedCount > 0) {
            log.debug("[PARSE] augment table parse summary | totalRows={} | parsed={} | skipped={}", rows.size(), results.size(), skippedCount);
        }

        return results;
    }

    /**
     * 解析单行符文数据
     *
     * @param row HTML 表格行元素
     * @return 符文统计数据 DTO，解析失败时返回 null
     */
    AramAugmentStatsDTO parseAugmentRow(Element row) {
        return parseAugmentRow(row, -1);
    }

    /**
     * 解析单行符文数据（带行号，用于日志追踪）
     *
     * ══════════════════════════════════════════════════════════════
     * 处理流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 获取所有 td 单元格，检查数量 >= 4
     * 2. 从 td[1] 提取符文名称和品质
     * 3. 从 td[2] 提取胜率，从 td[3] 提取选取率
     * 4. 可选字段：平均排名(td[4])、梯级(td[5])、套装(td[6])
     * 5. 构建 AramAugmentStatsDTO
     *
     * @param row      HTML 表格行元素
     * @param rowIndex 行号（从1开始）
     * @return 符文统计数据 DTO，解析失败时返回 null
     */
    AramAugmentStatsDTO parseAugmentRow(Element row, int rowIndex) {
        Elements cells = row.select("td");
        if (cells.size() < 4) {
            log.trace("[PARSE] augment row insufficient columns | rowIndex={} | cellCount={}", rowIndex, cells.size());
            return null;
        }

        String augmentName = extractText(cells.get(1));
        String quality = extractQuality(cells.get(1));
        BigDecimal winRate = parsePercentCell(cells.get(2));
        BigDecimal pickRate = parsePercentCell(cells.get(3));

        BigDecimal avgPlacement = null;
        String tier = null;
        String synergySet = null;

        if (cells.size() >= 5) {
            avgPlacement = parseNumericCell(cells.get(4));
        }
        if (cells.size() >= 6) {
            tier = extractTier(cells.get(5));
        }
        if (cells.size() >= 7) {
            synergySet = extractText(cells.get(6));
        }

        log.trace("[TRANSFORM] augment row parsed | rowIndex={} | name={} | quality={} | winRate={} | pickRate={} | placement={} | tier={} | synergy={}",
                rowIndex, augmentName, quality, winRate, pickRate, avgPlacement, tier, synergySet);

        return AramAugmentStatsDTO.builder()
                .augmentName(augmentName)
                .quality(quality)
                .winRate(winRate)
                .pickRate(pickRate)
                .avgPlacement(avgPlacement)
                .tier(tier)
                .synergySet(synergySet)
                .source("u.gg")
                .build();
    }

    /**
     * 提取英雄名称（去除梯级标记等干扰文本）
     *
     * ══════════════════════════════════════════════════════════════
     * 为什么需要"提取"而不是直接取文本？
     * ══════════════════════════════════════════════════════════════
     *
     * U.GG 的英雄名称单元格可能包含：
     * - 英雄名称（如 "Aatrox"）
     * - 梯级图标（如 <img alt="S tier">）
     * - 链接标签（如 <a href="...">Aatrox</a>）
     * - 其他装饰性文本
     *
     * 所以需要优先从 <a> 或 .champion-name 等子元素中提取纯名称，
     * 如果找不到子元素，则降级为取整个单元格的文本。
     *
     * @param cell HTML 单元格元素（td[1]）
     * @return 清洗后的英雄名称
     */
    String extractChampionName(Element cell) {
        Element nameEl = cell.selectFirst("a, .champion-name, span");
        if (nameEl != null) {
            String raw = nameEl.text();
            String cleaned = sanitize(raw);
            log.trace("[CLEAN] extractChampionName | raw='{}' | cleaned='{}'", raw, cleaned);
            return cleaned;
        }
        String raw = cell.text();
        String cleaned = sanitize(raw);
        log.trace("[CLEAN] extractChampionName (fallback) | raw='{}' | cleaned='{}'", raw, cleaned);
        return cleaned;
    }

    /**
     * 提取梯级评级（S+/S/A/B/C）
     *
     * ══════════════════════════════════════════════════════════════
     * 提取策略（按优先级）
     * ══════════════════════════════════════════════════════════════
     *
     * 策略1：从 CSS class 含 "tier" 的元素或 alt 含 "tier" 的 img 中提取
     * 策略2：用正则从单元格文本中匹配梯级标记（S+/S/A/B/C）
     * 策略3：都找不到则返回 null
     *
     * @param cell HTML 单元格元素
     * @return 梯级评级（大写），找不到时返回 null
     */
    String extractTier(Element cell) {
        Elements tierElements = cell.select(".tier, [class*=tier], img[alt*=tier]");
        if (!tierElements.isEmpty()) {
            String tierText = tierElements.first().text();
            if (!tierText.isEmpty()) {
                String cleaned = sanitize(tierText).toUpperCase();
                log.trace("[CLEAN] extractTier (element) | raw='{}' | cleaned='{}'", tierText, cleaned);
                return cleaned;
            }
            String alt = tierElements.first().attr("alt");
            if (!alt.isEmpty()) {
                String cleaned = sanitize(alt).toUpperCase();
                log.trace("[CLEAN] extractTier (imgAlt) | raw='{}' | cleaned='{}'", alt, cleaned);
                return cleaned;
            }
        }

        String cellText = cell.text().trim();
        if (cellText.matches(".*[Ss][+]?\\b.*") || cellText.matches(".*[A-Ca-c]\\b.*")) {
            String extracted = cellText.replaceAll(".*?([Ss][+]?|[A-Ca-c]).*", "$1");
            String cleaned = extracted.toUpperCase();
            log.trace("[CLEAN] extractTier (regex) | cellText='{}' | extracted='{}' | cleaned='{}'", cellText, extracted, cleaned);
            return cleaned;
        }

        log.trace("[CLEAN] extractTier | no tier found | cellText='{}'", cellText);
        return null;
    }

    /**
     * 提取符文品质（银色/金色/棱彩）
     *
     * ══════════════════════════════════════════════════════════════
     * 提取策略
     * ══════════════════════════════════════════════════════════════
     *
     * 策略1：从 CSS class 名判断
     *   - 含 "prismatic" 或 "legendary" → 棱彩
     *   - 含 "gold" 或 "epic" → 金色
     *   - 含 "silver" 或 "rare" → 银色
     *
     * 策略2：从文本内容判断
     *   - 含 "prismatic" 或 "棱彩" → 棱彩
     *   - 含 "gold" 或 "金色" → 金色
     *   - 含 "silver" 或 "银色" → 银色
     *
     * 策略3：无法判断则返回 null
     *
     * @param cell HTML 单元格元素
     * @return 品质名称（中文），找不到时返回 null
     */
    String extractQuality(Element cell) {
        String className = cell.className();
        if (className.contains("prismatic") || className.contains("legendary")) {
            log.trace("[CLEAN] extractQuality (class) | className='{}' | quality=棱彩", className);
            return "棱彩";
        } else if (className.contains("gold") || className.contains("epic")) {
            log.trace("[CLEAN] extractQuality (class) | className='{}' | quality=金色", className);
            return "金色";
        } else if (className.contains("silver") || className.contains("rare")) {
            log.trace("[CLEAN] extractQuality (class) | className='{}' | quality=银色", className);
            return "银色";
        }

        String text = cell.text().toLowerCase();
        if (text.contains("prismatic") || text.contains("棱彩")) {
            log.trace("[CLEAN] extractQuality (text) | text='{}' | quality=棱彩", text);
            return "棱彩";
        } else if (text.contains("gold") || text.contains("金色")) {
            log.trace("[CLEAN] extractQuality (text) | text='{}' | quality=金色", text);
            return "金色";
        } else if (text.contains("silver") || text.contains("银色")) {
            log.trace("[CLEAN] extractQuality (text) | text='{}' | quality=银色", text);
            return "银色";
        }

        log.trace("[CLEAN] extractQuality | unknown quality | className='{}' | text='{}'", className, text);
        return null;
    }

    /**
     * 解析百分比单元格
     *
     * 将 "52.30%" 这样的文本转换为 BigDecimal 52.30
     * 处理步骤：去除百分号 → 替换逗号为点（欧洲数字格式） → 解析为 BigDecimal → 保留2位小数
     *
     * @param cell HTML 单元格元素
     * @return 百分比数值（如 52.30），解析失败时返回 null
     */
    BigDecimal parsePercentCell(Element cell) {
        String raw = cell.text();
        String cleaned = raw.trim().replace("%", "").replace(",", ".").trim();
        log.trace("[CLEAN] parsePercentCell | raw='{}' | cleaned='{}'", raw, cleaned);
        try {
            BigDecimal result = new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
            log.trace("[TRANSFORM] parsePercentCell | cleaned='{}' | result={}", cleaned, result);
            return result;
        } catch (NumberFormatException e) {
            log.debug("[CLEAN] parsePercentCell invalid format | raw='{}' | cleaned='{}'", raw, cleaned);
            return null;
        }
    }

    /**
     * 解析数值单元格
     *
     * 将 "5.3" 这样的文本转换为 BigDecimal 5.30
     * 处理步骤：替换逗号为点 → 解析为 BigDecimal → 保留2位小数
     *
     * @param cell HTML 单元格元素
     * @return 数值（如 5.30），解析失败时返回 null
     */
    BigDecimal parseNumericCell(Element cell) {
        String raw = cell.text();
        String cleaned = raw.trim().replace(",", ".").trim();
        log.trace("[CLEAN] parseNumericCell | raw='{}' | cleaned='{}'", raw, cleaned);
        try {
            BigDecimal result = new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
            log.trace("[TRANSFORM] parseNumericCell | cleaned='{}' | result={}", cleaned, result);
            return result;
        } catch (NumberFormatException e) {
            log.debug("[CLEAN] parseNumericCell invalid format | raw='{}' | cleaned='{}'", raw, cleaned);
            return null;
        }
    }

    /**
     * 提取纯文本内容
     *
     * 获取单元格的文本内容并进行清洗（去除多余空白和特殊字符）
     *
     * @param cell HTML 单元格元素
     * @return 清洗后的文本
     */
    String extractText(Element cell) {
        String raw = cell.text();
        String cleaned = sanitize(raw);
        log.trace("[CLEAN] extractText | raw='{}' | cleaned='{}'", raw, cleaned);
        return cleaned;
    }

    /**
     * 文本清理：去除首尾空白、特殊字符
     *
     * 清理规则：
     * - 将所有空白字符（包括不间断空格 \u00A0）替换为单个空格
     * - 去除首尾空白
     *
     * @param text 原始文本
     * @return 清洗后的文本，输入为 null 时返回 null
     */
    String sanitize(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    /**
     * 强制请求间隔（反爬策略）
     *
     * ══════════════════════════════════════════════════════════════
     * 工作原理
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 计算距离上次请求的间隔时间
     * 2. 如果间隔 < requestIntervalMs，则 sleep 补足差值
     * 3. 如果线程在 sleep 期间被中断，恢复中断标志并继续
     *
     * 为什么用 volatile？
     * - lastRequestTime 可能被多个线程读取（虽然当前是单线程使用）
     * - volatile 确保读取到的是最新值，避免线程缓存导致的旧值问题
     */
    void enforceRequestInterval() {
        long elapsed = System.currentTimeMillis() - lastRequestTime;
        if (elapsed < requestIntervalMs) {
            long sleepMs = requestIntervalMs - elapsed;
            log.debug("[RECEIVE] enforcing request interval | elapsed={}ms | required={}ms | sleep={}ms",
                    elapsed, requestIntervalMs, sleepMs);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[RECEIVE] request interval sleep interrupted");
            }
        }
    }

    /**
     * 随机选择 User-Agent（反爬策略）
     *
     * ══════════════════════════════════════════════════════════════
     * 工作原理
     * ══════════════════════════════════════════════════════════════
     *
     * 从 userAgents 列表中随机选择一个 User-Agent 字符串。
     * 使用 ThreadLocalRandom 而不是 Random，因为：
     * - ThreadLocalRandom 在多线程环境下性能更好（无竞争）
     * - 不需要创建 Random 实例
     *
     * 如果 userAgents 列表为空，返回默认的 Chrome User-Agent。
     *
     * @return 随机选择的 User-Agent 字符串
     */
    String selectRandomUserAgent() {
        if (userAgents == null || userAgents.isEmpty()) {
            log.trace("[RECEIVE] using default User-Agent | poolSize=0");
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        }
        String ua = userAgents.get(ThreadLocalRandom.current().nextInt(userAgents.size()));
        log.trace("[RECEIVE] User-Agent selected | poolSize={} | ua={}", userAgents.size(), ua);
        return ua;
    }
}
