package com.aram.mayhem.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Riot Data Dragon API 客户端
 *
 * 功能：调用 Riot Data Dragon REST API 获取英雄基础数据
 * 数据源：https://ddragon.leagueoflegends.com
 * API 流程：
 *   1. GET /api/versions.json → 获取最新版本号
 *   2. GET /cdn/{version}/data/{locale}/champion.json → 获取英雄列表
 *   3. GET /cdn/{version}/data/{locale}/champion/{key}.json → 获取英雄详情
 * 异常策略：网络超时/解析失败 → 返回空结果 + 日志告警，不中断同步流程
 * 关联：service/DataAggregatorService.java
 */
@Slf4j
@Component
public class RiotDataDragonClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${datadragon.base-url:https://ddragon.leagueoflegends.com}")
    private String baseUrl;

    @Value("${datadragon.locale:zh_CN}")
    private String locale;

    public RiotDataDragonClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取 Data Dragon 最新版本号
     *
     * @return 最新版本号字符串（如 "14.10.1"），失败时返回 null
     */
    public String fetchLatestVersion() {
        String url = baseUrl + "/api/versions.json";
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("DataDragon: versions response is null");
                return null;
            }
            JsonNode array = objectMapper.readTree(response);
            if (array.isArray() && !array.isEmpty()) {
                String version = array.get(0).asText();
                log.info("DataDragon: latest version = {}", version);
                return version;
            }
            log.warn("DataDragon: versions array is empty");
            return null;
        } catch (RestClientException e) {
            log.error("DataDragon: failed to fetch versions from {}", url, e);
            return null;
        } catch (Exception e) {
            log.error("DataDragon: failed to parse versions response", e);
            return null;
        }
    }

    /**
     * 获取英雄列表（全量）
     *
     * @param version Data Dragon 版本号
     * @return 英雄列表，key=championKey（如 "Aatrox"），value=英雄基础信息 JSON
     *         失败时返回空 Map
     */
    public Map<String, JsonNode> fetchChampionList(String version) {
        String url = String.format("%s/cdn/%s/data/%s/champion.json", baseUrl, version, locale);
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("DataDragon: champion list response is null for version={}", version);
                return Collections.emptyMap();
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            if (!data.isObject()) {
                log.warn("DataDragon: champion list data node is not an object");
                return Collections.emptyMap();
            }

            Map<String, JsonNode> champions = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                champions.put(entry.getKey(), entry.getValue());
            }
            log.info("DataDragon: fetched {} champions for version={}", champions.size(), version);
            return champions;
        } catch (RestClientException e) {
            log.error("DataDragon: failed to fetch champion list from {}", url, e);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("DataDragon: failed to parse champion list response", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取英雄详情（含技能、被动、属性）
     *
     * @param version    Data Dragon 版本号
     * @param championKey 英雄 Key（如 "Aatrox"）
     * @return 英雄详情 JSON 节点，失败时返回 null
     */
    public JsonNode fetchChampionDetail(String version, String championKey) {
        String url = String.format("%s/cdn/%s/data/%s/champion/%s.json", baseUrl, version, locale, championKey);
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.warn("DataDragon: champion detail response is null for key={}", championKey);
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode championData = root.path("data").path(championKey);
            if (championData.isMissingNode()) {
                log.warn("DataDragon: champion data missing for key={}", championKey);
                return null;
            }
            log.debug("DataDragon: fetched detail for champion={}", championKey);
            return championData;
        } catch (RestClientException e) {
            log.error("DataDragon: failed to fetch champion detail for key={} from {}", championKey, url, e);
            return null;
        } catch (Exception e) {
            log.error("DataDragon: failed to parse champion detail for key={}", championKey, e);
            return null;
        }
    }

    /**
     * 获取英雄图片 URL 列表（头像、技能图标、被动图标）
     *
     * @param version     Data Dragon 版本号
     * @param championKey 英雄 Key
     * @return 图片 URL 映射（key=图片类型，value=完整 URL），失败时返回空 Map
     */
    public Map<String, String> fetchChampionImages(String version, String championKey) {
        Map<String, String> images = new LinkedHashMap<>();

        String spriteBaseUrl = String.format("%s/cdn/%s/img", baseUrl, version);

        images.put("avatar", String.format("%s/champion/%s.png", spriteBaseUrl, championKey));

        JsonNode detail = fetchChampionDetail(version, championKey);
        if (detail == null) {
            log.warn("DataDragon: cannot fetch images for champion={}, detail is null", championKey);
            return images;
        }

        JsonNode passive = detail.path("passive");
        if (!passive.isMissingNode() && passive.has("image")) {
            String passiveImage = passive.path("image").path("full").asText("");
            if (!passiveImage.isEmpty()) {
                images.put("passive", String.format("%s/passive/%s", spriteBaseUrl, passiveImage));
            }
        }

        JsonNode spells = detail.path("spells");
        if (spells.isArray()) {
            String[] spellKeys = {"Q", "W", "E", "R"};
            for (int i = 0; i < Math.min(spells.size(), spellKeys.length); i++) {
                String spellImage = spells.get(i).path("image").path("full").asText("");
                if (!spellImage.isEmpty()) {
                    images.put("spell_" + spellKeys[i], String.format("%s/spell/%s", spriteBaseUrl, spellImage));
                }
            }
        }

        log.debug("DataDragon: fetched {} image URLs for champion={}", images.size(), championKey);
        return images;
    }

    /**
     * 获取英雄基础属性（攻击、防御、魔法、难度）
     *
     * @param championData 英雄详情 JSON 节点（来自 fetchChampionDetail）
     * @return 属性映射（key=属性名，value=属性值），失败时返回空 Map
     */
    public Map<String, Integer> extractStats(JsonNode championData) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        if (championData == null) {
            return stats;
        }

        JsonNode info = championData.path("info");
        if (info.isObject()) {
            putIfPresent(stats, info, "attack", "attack");
            putIfPresent(stats, info, "defense", "defense");
            putIfPresent(stats, info, "magic", "magic");
            putIfPresent(stats, info, "difficulty", "difficulty");
        }

        return stats;
    }

    private void putIfPresent(Map<String, Integer> map, JsonNode node, String jsonKey, String mapKey) {
        if (node.has(jsonKey)) {
            map.put(mapKey, node.get(jsonKey).asInt());
        }
    }
}
