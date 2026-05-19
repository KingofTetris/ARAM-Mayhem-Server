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
 * ARAM 数据采集器
 *
 * 功能：从 U.GG ARAM 页面采集英雄胜率/选取率/梯级 + 强化符文数据
 * 数据源：https://u.gg/lol/aram
 * 采集方式：RestTemplate 获取 HTML → Jsoup 解析表格数据
 * 反爬策略：请求间隔 ≥2s + 随机 User-Agent
 * 异常策略：网络超时/解析失败 → 返回空结果 + 日志告警，不中断同步流程
 * 关联：service/DataAggregatorService.java
 *
 * 日志级别说明：
 * - INFO：采集流程的起止和汇总信息
 * - DEBUG：各阶段处理细节（行解析、字段提取）
 * - TRACE：原始数据值和清洗前后对比
 * - WARN：可恢复的异常情况（空响应、行跳过）
 * - ERROR：不可恢复的异常（网络故障、解析崩溃）
 */
@Slf4j
@Component
public class AramDataCollector {

    private final RestTemplate restTemplate;

    @Value("${aramdata.base-url:https://u.gg/lol/aram}")
    private String baseUrl;

    @Value("${aramdata.request-interval-ms:2000}")
    private long requestIntervalMs;

    @Value("#{'${aramdata.user-agents:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36}'.split(',')}")
    private List<String> userAgents;

    private volatile long lastRequestTime = 0;

    public AramDataCollector(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    void setRequestIntervalMs(long requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    void setUserAgents(List<String> userAgents) {
        this.userAgents = userAgents;
    }

    void setLastRequestTime(long lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    /**
     * 采集 ARAM 英雄统计数据
     *
     * @return 英雄统计列表，失败时返回空 List
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
     * 采集 ARAM 强化符文统计数据
     *
     * @return 符文统计列表，失败时返回空 List
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
     * U.GG ARAM 页面表格结构（典型）：
     * table.tr-table > tbody > tr
     *   td[1]: 排名
     *   td[2]: 英雄名称 + 梯级图标
     *   td[3]: 胜率
     *   td[4]: 选取率
     *   td[5~7]: 场均 KDA
     *
     * @param doc Jsoup Document
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
     */
    AramHeroStatsDTO parseHeroRow(Element row) {
        return parseHeroRow(row, -1);
    }

    AramHeroStatsDTO parseHeroRow(Element row, int rowIndex) {
        Elements cells = row.select("td");
        if (cells.size() < 4) {
            log.trace("[PARSE] hero row insufficient columns | rowIndex={} | cellCount={}", rowIndex, cells.size());
            return null;
        }

        String championName = extractChampionName(cells.get(1));
        String tier = extractTier(cells.get(1));
        BigDecimal winRate = parsePercentCell(cells.get(2));
        BigDecimal pickRate = parsePercentCell(cells.get(3));

        BigDecimal avgKills = null;
        BigDecimal avgDeaths = null;
        BigDecimal avgAssists = null;
        if (cells.size() >= 7) {
            avgKills = parseNumericCell(cells.get(4));
            avgDeaths = parseNumericCell(cells.get(5));
            avgAssists = parseNumericCell(cells.get(6));
        }

        log.trace("[TRANSFORM] hero row parsed | rowIndex={} | champion={} | tier={} | winRate={} | pickRate={} | kills={} | deaths={} | assists={}",
                rowIndex, championName, tier, winRate, pickRate, avgKills, avgDeaths, avgAssists);

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
     */
    AramAugmentStatsDTO parseAugmentRow(Element row) {
        return parseAugmentRow(row, -1);
    }

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
     * 解析百分比单元格（如 "52.30%" → 52.30）
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
     * 解析数值单元格（如 "5.3" → 5.30）
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
     */
    String extractText(Element cell) {
        String raw = cell.text();
        String cleaned = sanitize(raw);
        log.trace("[CLEAN] extractText | raw='{}' | cleaned='{}'", raw, cleaned);
        return cleaned;
    }

    /**
     * 文本清理：去除首尾空白、特殊字符
     */
    String sanitize(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    /**
     * 强制请求间隔（反爬策略）
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
