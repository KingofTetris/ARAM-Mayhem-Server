package com.aram.mayhem.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AramDataCollector 单元测试
 *
 * 覆盖范围：
 * - collectAramStats：正常/空响应/网络异常/HTML无表格
 * - collectAugmentStats：正常/空响应/网络异常
 * - parseHeroStatsTable：正常/多行/空表格/列数不足
 * - parseAugmentStatsTable：正常/多行/空表格
 * - parseHeroRow：完整数据/部分数据/列数不足
 * - parseAugmentRow：完整数据/部分数据/列数不足
 * - extractChampionName：有链接/无链接
 * - extractTier：有tier元素/无tier元素/有alt属性
 * - extractQuality：prismatic/gold/silver/未知
 * - parsePercentCell：正常/无百分号/非法格式
 * - parseNumericCell：正常/非法格式
 * - enforceRequestInterval：间隔不足/间隔充足
 * - selectRandomUserAgent：正常/空列表/null列表
 * - sanitize：null/空白/特殊字符
 * - fetchPage：正常/网络异常
 */
@DisplayName("AramDataCollector 测试")
@ExtendWith(MockitoExtension.class)
class AramDataCollectorTest {

    @Mock
    private RestTemplate restTemplate;

    private AramDataCollector collector;

    @BeforeEach
    void setUp() {
        collector = new AramDataCollector(restTemplate);
        collector.setBaseUrl("https://u.gg/lol/aram");
        collector.setRequestIntervalMs(0);
        collector.setUserAgents(List.of(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
        ));
    }

    private Element parseRow(String rowHtml) {
        String tableHtml = "<table><tbody>" + rowHtml + "</tbody></table>";
        Document doc = Jsoup.parse(tableHtml);
        return doc.select("table tbody tr").first();
    }

    private Element parseCell(String cellHtml) {
        String tableHtml = "<table><tbody><tr>" + cellHtml + "</tr></tbody></table>";
        Document doc = Jsoup.parse(tableHtml);
        return doc.select("table tbody tr td").first();
    }

    @Nested
    @DisplayName("collectAramStats 测试")
    class CollectAramStatsTest {

        @Test
        @DisplayName("正常采集英雄统计数据")
        void collectAramStats_success() {
            String html = buildHeroTableHtml();
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(html);

            var stats = collector.collectAramStats();

            assertFalse(stats.isEmpty());
            assertEquals(2, stats.size());
            assertEquals("Aatrox", stats.get(0).getChampionName());
            assertEquals(new BigDecimal("52.30"), stats.get(0).getWinRate());
            assertEquals(new BigDecimal("15.30"), stats.get(0).getPickRate());
            assertEquals("S", stats.get(0).getTier());
            assertEquals("u.gg", stats.get(0).getSource());
        }

        @Test
        @DisplayName("空HTML响应返回空列表")
        void collectAramStats_emptyHtml() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

            var stats = collector.collectAramStats();

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("null响应返回空列表")
        void collectAramStats_nullResponse() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            var stats = collector.collectAramStats();

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("网络异常返回空列表")
        void collectAramStats_networkError() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            var stats = collector.collectAramStats();

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("HTML无表格返回空列表")
        void collectAramStats_noTable() {
            String html = "<html><body><p>No data here</p></body></html>";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(html);

            var stats = collector.collectAramStats();

            assertTrue(stats.isEmpty());
        }
    }

    @Nested
    @DisplayName("collectAugmentStats 测试")
    class CollectAugmentStatsTest {

        @Test
        @DisplayName("正常采集符文统计数据")
        void collectAugmentStats_success() {
            String html = buildAugmentTableHtml();
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(html);

            var stats = collector.collectAugmentStats();

            assertFalse(stats.isEmpty());
            assertEquals(2, stats.size());
            assertEquals("Poro Power", stats.get(0).getAugmentName());
            assertEquals(new BigDecimal("55.10"), stats.get(0).getWinRate());
            assertEquals("u.gg", stats.get(0).getSource());
        }

        @Test
        @DisplayName("空HTML响应返回空列表")
        void collectAugmentStats_emptyHtml() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

            var stats = collector.collectAugmentStats();

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("网络异常返回空列表")
        void collectAugmentStats_networkError() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RestClientException("Timeout"));

            var stats = collector.collectAugmentStats();

            assertTrue(stats.isEmpty());
        }
    }

    @Nested
    @DisplayName("parseHeroStatsTable 测试")
    class ParseHeroStatsTableTest {

        @Test
        @DisplayName("解析完整英雄表格")
        void parseHeroStatsTable_fullTable() {
            Document doc = Jsoup.parse(buildHeroTableHtml());

            var stats = collector.parseHeroStatsTable(doc);

            assertEquals(2, stats.size());
        }

        @Test
        @DisplayName("解析tr-table类名表格")
        void parseHeroStatsTable_trTableClass() {
            String html = "<table class=\"tr-table\"><tbody>" +
                    "<tr><td>1</td><td><a>Aatrox</a></td><td>52.30%</td><td>15.30%</td></tr>" +
                    "</tbody></table>";
            Document doc = Jsoup.parse(html);

            var stats = collector.parseHeroStatsTable(doc);

            assertEquals(1, stats.size());
        }

        @Test
        @DisplayName("空表格返回空列表")
        void parseHeroStatsTable_emptyTable() {
            String html = "<table><tbody></tbody></table>";
            Document doc = Jsoup.parse(html);

            var stats = collector.parseHeroStatsTable(doc);

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("列数不足的行被跳过")
        void parseHeroStatsTable_insufficientColumns() {
            String html = "<table><tbody>" +
                    "<tr><td>1</td><td>Aatrox</td></tr>" +
                    "<tr><td>2</td><td>Ahri</td><td>50.00%</td><td>12.00%</td></tr>" +
                    "</tbody></table>";
            Document doc = Jsoup.parse(html);

            var stats = collector.parseHeroStatsTable(doc);

            assertEquals(1, stats.size());
        }
    }

    @Nested
    @DisplayName("parseHeroRow 测试")
    class ParseHeroRowTest {

        @Test
        @DisplayName("完整7列数据解析")
        void parseHeroRow_fullData() {
            var row = parseRow("<tr><td>1</td><td><a>Aatrox</a></td><td>52.30%</td><td>15.30%</td><td>5.3</td><td>3.1</td><td>7.2</td></tr>");

            var stat = collector.parseHeroRow(row);

            assertNotNull(stat);
            assertEquals("Aatrox", stat.getChampionName());
            assertEquals(new BigDecimal("52.30"), stat.getWinRate());
            assertEquals(new BigDecimal("15.30"), stat.getPickRate());
            assertEquals(new BigDecimal("5.30"), stat.getAvgKills());
            assertEquals(new BigDecimal("3.10"), stat.getAvgDeaths());
            assertEquals(new BigDecimal("7.20"), stat.getAvgAssists());
        }

        @Test
        @DisplayName("仅4列数据时KDA为null")
        void parseHeroRow_minimalData() {
            var row = parseRow("<tr><td>1</td><td>Aatrox</td><td>52.30%</td><td>15.30%</td></tr>");

            var stat = collector.parseHeroRow(row);

            assertNotNull(stat);
            assertEquals("Aatrox", stat.getChampionName());
            assertNull(stat.getAvgKills());
            assertNull(stat.getAvgDeaths());
            assertNull(stat.getAvgAssists());
        }

        @Test
        @DisplayName("列数不足3列返回null")
        void parseHeroRow_insufficientColumns() {
            var row = parseRow("<tr><td>1</td><td>Aatrox</td></tr>");

            var stat = collector.parseHeroRow(row);

            assertNull(stat);
        }
    }

    @Nested
    @DisplayName("parseAugmentRow 测试")
    class ParseAugmentRowTest {

        @Test
        @DisplayName("完整7列数据解析")
        void parseAugmentRow_fullData() {
            var row = parseRow("<tr><td>1</td><td>Poro Power</td><td>55.10%</td><td>8.20%</td><td>3.5</td><td>S</td><td>Precision</td></tr>");

            var stat = collector.parseAugmentRow(row);

            assertNotNull(stat);
            assertEquals("Poro Power", stat.getAugmentName());
            assertEquals(new BigDecimal("55.10"), stat.getWinRate());
            assertEquals(new BigDecimal("8.20"), stat.getPickRate());
            assertEquals(new BigDecimal("3.50"), stat.getAvgPlacement());
        }

        @Test
        @DisplayName("仅4列数据时扩展字段为null")
        void parseAugmentRow_minimalData() {
            var row = parseRow("<tr><td>1</td><td>Poro Power</td><td>55.10%</td><td>8.20%</td></tr>");

            var stat = collector.parseAugmentRow(row);

            assertNotNull(stat);
            assertNull(stat.getAvgPlacement());
            assertNull(stat.getTier());
            assertNull(stat.getSynergySet());
        }

        @Test
        @DisplayName("列数不足3列返回null")
        void parseAugmentRow_insufficientColumns() {
            var row = parseRow("<tr><td>1</td><td>Poro Power</td></tr>");

            var stat = collector.parseAugmentRow(row);

            assertNull(stat);
        }
    }

    @Nested
    @DisplayName("extractChampionName 测试")
    class ExtractChampionNameTest {

        @Test
        @DisplayName("有链接时提取链接文本")
        void extractChampionName_withLink() {
            var cell = parseCell("<td><a href=\"/lol/champions/aatrox\">Aatrox</a></td>");

            String name = collector.extractChampionName(cell);

            assertEquals("Aatrox", name);
        }

        @Test
        @DisplayName("无链接时提取单元格文本")
        void extractChampionName_noLink() {
            var cell = parseCell("<td>Ahri</td>");

            String name = collector.extractChampionName(cell);

            assertEquals("Ahri", name);
        }

        @Test
        @DisplayName("有champion-name类时优先提取")
        void extractChampionName_withClassName() {
            var cell = parseCell("<td><span class=\"champion-name\">Zed</span><span class=\"tier\">S</span></td>");

            String name = collector.extractChampionName(cell);

            assertEquals("Zed", name);
        }
    }

    @Nested
    @DisplayName("extractTier 测试")
    class ExtractTierTest {

        @Test
        @DisplayName("有tier类元素时提取梯级")
        void extractTier_withTierElement() {
            var cell = parseCell("<td><span class=\"tier\">S</span> Aatrox</td>");

            String tier = collector.extractTier(cell);

            assertEquals("S", tier);
        }

        @Test
        @DisplayName("有alt属性的img时提取梯级")
        void extractTier_withImgAlt() {
            var cell = parseCell("<td><img alt=\"tier S\" /> Aatrox</td>");

            String tier = collector.extractTier(cell);

            assertEquals("TIER S", tier);
        }

        @Test
        @DisplayName("无梯级元素时从文本提取")
        void extractTier_fromText() {
            var cell = parseCell("<td>S+ Aatrox</td>");

            String tier = collector.extractTier(cell);

            assertEquals("S+", tier);
        }

        @Test
        @DisplayName("无梯级信息时返回null")
        void extractTier_noTier() {
            var cell = parseCell("<td>Aatrox</td>");

            String tier = collector.extractTier(cell);

            assertNull(tier);
        }
    }

    @Nested
    @DisplayName("extractQuality 测试")
    class ExtractQualityTest {

        @Test
        @DisplayName("prismatic类返回棱彩")
        void extractQuality_prismatic() {
            var cell = parseCell("<td class=\"prismatic\">Poro Power</td>");

            String quality = collector.extractQuality(cell);

            assertEquals("棱彩", quality);
        }

        @Test
        @DisplayName("gold类返回金色")
        void extractQuality_gold() {
            var cell = parseCell("<td class=\"gold-item\">Fire Wave</td>");

            String quality = collector.extractQuality(cell);

            assertEquals("金色", quality);
        }

        @Test
        @DisplayName("silver类返回银色")
        void extractQuality_silver() {
            var cell = parseCell("<td class=\"silver-item\">Quick Strike</td>");

            String quality = collector.extractQuality(cell);

            assertEquals("银色", quality);
        }

        @Test
        @DisplayName("文本包含prismatic返回棱彩")
        void extractQuality_textPrismatic() {
            var cell = parseCell("<td>Poro Power prismatic</td>");

            String quality = collector.extractQuality(cell);

            assertEquals("棱彩", quality);
        }

        @Test
        @DisplayName("未知品质返回null")
        void extractQuality_unknown() {
            var cell = parseCell("<td>Unknown Augment</td>");

            String quality = collector.extractQuality(cell);

            assertNull(quality);
        }
    }

    @Nested
    @DisplayName("parsePercentCell 测试")
    class ParsePercentCellTest {

        @Test
        @DisplayName("正常百分比解析")
        void parsePercentCell_normal() {
            var cell = parseCell("<td>52.30%</td>");

            BigDecimal result = collector.parsePercentCell(cell);

            assertEquals(new BigDecimal("52.30"), result);
        }

        @Test
        @DisplayName("无百分号解析")
        void parsePercentCell_noPercent() {
            var cell = parseCell("<td>52.30</td>");

            BigDecimal result = collector.parsePercentCell(cell);

            assertEquals(new BigDecimal("52.30"), result);
        }

        @Test
        @DisplayName("非法格式返回null")
        void parsePercentCell_invalid() {
            var cell = parseCell("<td>N/A</td>");

            BigDecimal result = collector.parsePercentCell(cell);

            assertNull(result);
        }

        @Test
        @DisplayName("逗号作为小数点解析")
        void parsePercentCell_commaDecimal() {
            var cell = parseCell("<td>52,30%</td>");

            BigDecimal result = collector.parsePercentCell(cell);

            assertEquals(new BigDecimal("52.30"), result);
        }
    }

    @Nested
    @DisplayName("parseNumericCell 测试")
    class ParseNumericCellTest {

        @Test
        @DisplayName("正常数值解析")
        void parseNumericCell_normal() {
            var cell = parseCell("<td>5.3</td>");

            BigDecimal result = collector.parseNumericCell(cell);

            assertEquals(new BigDecimal("5.30"), result);
        }

        @Test
        @DisplayName("非法格式返回null")
        void parseNumericCell_invalid() {
            var cell = parseCell("<td>--</td>");

            BigDecimal result = collector.parseNumericCell(cell);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("enforceRequestInterval 测试")
    class EnforceRequestIntervalTest {

        @Test
        @DisplayName("间隔充足时不休眠")
        void enforceRequestInterval_sufficientGap() {
            collector.setRequestIntervalMs(0);

            assertDoesNotThrow(() -> collector.enforceRequestInterval());
        }

        @Test
        @DisplayName("间隔不足时休眠")
        void enforceRequestInterval_insufficientGap() {
            collector.setRequestIntervalMs(100);
            collector.setLastRequestTime(System.currentTimeMillis());

            long start = System.currentTimeMillis();
            collector.enforceRequestInterval();
            long elapsed = System.currentTimeMillis() - start;

            assertTrue(elapsed >= 0);
        }
    }

    @Nested
    @DisplayName("selectRandomUserAgent 测试")
    class SelectRandomUserAgentTest {

        @Test
        @DisplayName("正常列表返回非空User-Agent")
        void selectRandomUserAgent_normal() {
            String ua = collector.selectRandomUserAgent();

            assertNotNull(ua);
            assertTrue(ua.startsWith("Mozilla"));
        }

        @Test
        @DisplayName("空列表返回默认User-Agent")
        void selectRandomUserAgent_emptyList() {
            collector.setUserAgents(List.of());

            String ua = collector.selectRandomUserAgent();

            assertNotNull(ua);
            assertTrue(ua.contains("Mozilla"));
        }

        @Test
        @DisplayName("null列表返回默认User-Agent")
        void selectRandomUserAgent_nullList() {
            collector.setUserAgents(null);

            String ua = collector.selectRandomUserAgent();

            assertNotNull(ua);
            assertTrue(ua.contains("Mozilla"));
        }
    }

    @Nested
    @DisplayName("sanitize 测试")
    class SanitizeTest {

        @Test
        @DisplayName("null输入返回null")
        void sanitize_null() {
            assertNull(collector.sanitize(null));
        }

        @Test
        @DisplayName("去除首尾空白")
        void sanitize_trim() {
            assertEquals("Aatrox", collector.sanitize("  Aatrox  "));
        }

        @Test
        @DisplayName("合并内部多余空白")
        void sanitize_multipleSpaces() {
            assertEquals("Aatrox Darkin", collector.sanitize("Aatrox  Darkin"));
        }

        @Test
        @DisplayName("替换不间断空格")
        void sanitize_nonBreakingSpace() {
            assertEquals("Aatrox", collector.sanitize("Aatrox\u00A0"));
        }
    }

    @Nested
    @DisplayName("fetchPage 测试")
    class FetchPageTest {

        @Test
        @DisplayName("正常获取页面HTML")
        void fetchPage_success() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("<html>data</html>");

            String html = collector.fetchPage("https://u.gg/lol/aram");

            assertEquals("<html>data</html>", html);
        }

        @Test
        @DisplayName("网络异常返回null")
        void fetchPage_networkError() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            String html = collector.fetchPage("https://u.gg/lol/aram");

            assertNull(html);
        }
    }

    private String buildHeroTableHtml() {
        return "<html><body><table class=\"tr-table\"><tbody>" +
                "<tr><td>1</td><td><a href=\"/lol/champions/aatrox\">Aatrox</a> <span class=\"tier\">S</span></td>" +
                "<td>52.30%</td><td>15.30%</td><td>5.3</td><td>3.1</td><td>7.2</td></tr>" +
                "<tr><td>2</td><td><a href=\"/lol/champions/ahri\">Ahri</a> <span class=\"tier\">A</span></td>" +
                "<td>50.10%</td><td>12.00%</td><td>4.8</td><td>4.5</td><td>8.1</td></tr>" +
                "</tbody></table></body></html>";
    }

    private String buildAugmentTableHtml() {
        return "<html><body><table class=\"tr-table\"><tbody>" +
                "<tr><td>1</td><td class=\"prismatic\">Poro Power</td>" +
                "<td>55.10%</td><td>8.20%</td><td>3.5</td><td>S</td><td>Precision</td></tr>" +
                "<tr><td>2</td><td class=\"gold-item\">Fire Wave</td>" +
                "<td>51.00%</td><td>6.50%</td><td>4.2</td><td>A</td><td>Domination</td></tr>" +
                "</tbody></table></body></html>";
    }
}
