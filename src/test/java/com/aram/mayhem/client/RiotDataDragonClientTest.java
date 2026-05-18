package com.aram.mayhem.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RiotDataDragonClient 单元测试
 *
 * 覆盖范围：
 * - fetchLatestVersion：正常/空响应/网络异常/解析异常
 * - fetchChampionList：正常/空响应/网络异常/解析异常
 * - fetchChampionDetail：正常/缺失节点/网络异常/解析异常
 * - fetchChampionImages：正常/详情为null/部分字段缺失
 * - extractStats：正常/null输入
 */
@DisplayName("RiotDataDragonClient 测试")
@ExtendWith(MockitoExtension.class)
class RiotDataDragonClientTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RiotDataDragonClient client;

    @BeforeEach
    void setUp() {
        client = new RiotDataDragonClient(restTemplate, objectMapper);
    }

    @Nested
    @DisplayName("fetchLatestVersion 测试")
    class FetchLatestVersionTest {

        @Test
        @DisplayName("正常获取最新版本号")
        void fetchLatestVersion_success() {
            String versionsJson = "[\"14.10.1\",\"14.9.1\",\"14.8.1\"]";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(versionsJson);

            String version = client.fetchLatestVersion();

            assertEquals("14.10.1", version);
        }

        @Test
        @DisplayName("响应为null时返回null")
        void fetchLatestVersion_nullResponse() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            String version = client.fetchLatestVersion();

            assertNull(version);
        }

        @Test
        @DisplayName("网络异常时返回null")
        void fetchLatestVersion_networkError() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            String version = client.fetchLatestVersion();

            assertNull(version);
        }

        @Test
        @DisplayName("解析异常时返回null")
        void fetchLatestVersion_parseError() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("invalid json");

            String version = client.fetchLatestVersion();

            assertNull(version);
        }

        @Test
        @DisplayName("空数组时返回null")
        void fetchLatestVersion_emptyArray() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("[]");

            String version = client.fetchLatestVersion();

            assertNull(version);
        }
    }

    @Nested
    @DisplayName("fetchChampionList 测试")
    class FetchChampionListTest {

        @Test
        @DisplayName("正常获取英雄列表")
        void fetchChampionList_success() {
            String championJson = "{\"data\":{\"Aatrox\":{\"id\":\"Aatrox\",\"key\":\"266\",\"name\":\"Aatrox\"},\"Ahri\":{\"id\":\"Ahri\",\"key\":\"103\",\"name\":\"Ahri\"}}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(championJson);

            Map<String, JsonNode> champions = client.fetchChampionList("14.10.1");

            assertEquals(2, champions.size());
            assertTrue(champions.containsKey("Aatrox"));
            assertTrue(champions.containsKey("Ahri"));
            assertEquals("Aatrox", champions.get("Aatrox").path("id").asText());
        }

        @Test
        @DisplayName("响应为null时返回空Map")
        void fetchChampionList_nullResponse() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            Map<String, JsonNode> champions = client.fetchChampionList("14.10.1");

            assertTrue(champions.isEmpty());
        }

        @Test
        @DisplayName("网络异常时返回空Map")
        void fetchChampionList_networkError() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RestClientException("Timeout"));

            Map<String, JsonNode> champions = client.fetchChampionList("14.10.1");

            assertTrue(champions.isEmpty());
        }

        @Test
        @DisplayName("解析异常时返回空Map")
        void fetchChampionList_parseError() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("not json");

            Map<String, JsonNode> champions = client.fetchChampionList("14.10.1");

            assertTrue(champions.isEmpty());
        }
    }

    @Nested
    @DisplayName("fetchChampionDetail 测试")
    class FetchChampionDetailTest {

        @Test
        @DisplayName("正常获取英雄详情")
        void fetchChampionDetail_success() {
            String detailJson = "{\"data\":{\"Aatrox\":{\"id\":\"Aatrox\",\"name\":\"Aatrox\",\"passive\":{\"name\":\"Deathbringer Stance\"},\"spells\":[{\"id\":\"AatroxQ\"}]}}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(detailJson);

            JsonNode detail = client.fetchChampionDetail("14.10.1", "Aatrox");

            assertNotNull(detail);
            assertEquals("Aatrox", detail.path("id").asText());
        }

        @Test
        @DisplayName("英雄数据缺失时返回null")
        void fetchChampionDetail_missingData() {
            String detailJson = "{\"data\":{\"OtherChamp\":{\"id\":\"OtherChamp\"}}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(detailJson);

            JsonNode detail = client.fetchChampionDetail("14.10.1", "Aatrox");

            assertNull(detail);
        }

        @Test
        @DisplayName("网络异常时返回null")
        void fetchChampionDetail_networkError() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection reset"));

            JsonNode detail = client.fetchChampionDetail("14.10.1", "Aatrox");

            assertNull(detail);
        }

        @Test
        @DisplayName("响应为null时返回null")
        void fetchChampionDetail_nullResponse() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            JsonNode detail = client.fetchChampionDetail("14.10.1", "Aatrox");

            assertNull(detail);
        }
    }

    @Nested
    @DisplayName("fetchChampionImages 测试")
    class FetchChampionImagesTest {

        @Test
        @DisplayName("正常获取图片URL列表")
        void fetchChampionImages_success() {
            String detailJson = "{\"data\":{\"Aatrox\":{\"id\":\"Aatrox\",\"passive\":{\"image\":{\"full\":\"Aatrox_Passive.png\"}},\"spells\":[{\"image\":{\"full\":\"AatroxQ.png\"}},{\"image\":{\"full\":\"AatroxW.png\"}},{\"image\":{\"full\":\"AatroxE.png\"}},{\"image\":{\"full\":\"AatroxR.png\"}}]}}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(detailJson);

            Map<String, String> images = client.fetchChampionImages("14.10.1", "Aatrox");

            assertTrue(images.containsKey("avatar"));
            assertTrue(images.containsKey("passive"));
            assertTrue(images.containsKey("spell_Q"));
            assertTrue(images.containsKey("spell_W"));
            assertTrue(images.containsKey("spell_E"));
            assertTrue(images.containsKey("spell_R"));
            assertTrue(images.get("avatar").contains("Aatrox.png"));
        }

        @Test
        @DisplayName("详情为null时仅返回头像URL")
        void fetchChampionImages_nullDetail() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            Map<String, String> images = client.fetchChampionImages("14.10.1", "Aatrox");

            assertEquals(1, images.size());
            assertTrue(images.containsKey("avatar"));
        }

        @Test
        @DisplayName("部分技能图标缺失时仍返回可用URL")
        void fetchChampionImages_partialSpells() {
            String detailJson = "{\"data\":{\"Aatrox\":{\"id\":\"Aatrox\",\"passive\":{\"image\":{\"full\":\"Aatrox_Passive.png\"}},\"spells\":[{\"image\":{\"full\":\"AatroxQ.png\"}}]}}}";
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(detailJson);

            Map<String, String> images = client.fetchChampionImages("14.10.1", "Aatrox");

            assertTrue(images.containsKey("avatar"));
            assertTrue(images.containsKey("passive"));
            assertTrue(images.containsKey("spell_Q"));
            assertFalse(images.containsKey("spell_W"));
        }
    }

    @Nested
    @DisplayName("extractStats 测试")
    class ExtractStatsTest {

        @Test
        @DisplayName("正常提取英雄属性")
        void extractStats_success() throws Exception {
            String json = "{\"info\":{\"attack\":8,\"defense\":4,\"magic\":3,\"difficulty\":4}}";
            JsonNode championData = objectMapper.readTree(json);

            Map<String, Integer> stats = client.extractStats(championData);

            assertEquals(4, stats.size());
            assertEquals(8, stats.get("attack"));
            assertEquals(4, stats.get("defense"));
            assertEquals(3, stats.get("magic"));
            assertEquals(4, stats.get("difficulty"));
        }

        @Test
        @DisplayName("null输入时返回空Map")
        void extractStats_nullInput() {
            Map<String, Integer> stats = client.extractStats(null);

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("缺少info节点时返回空Map")
        void extractStats_missingInfo() throws Exception {
            String json = "{\"id\":\"Aatrox\"}";
            JsonNode championData = objectMapper.readTree(json);

            Map<String, Integer> stats = client.extractStats(championData);

            assertTrue(stats.isEmpty());
        }
    }
}
