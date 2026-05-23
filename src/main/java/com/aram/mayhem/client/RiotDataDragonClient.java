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
 * Riot DataDragon API 客户端 —— 从 Riot 官方数据源获取英雄基础数据的"采购员"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是整个数据管线的"第一站"——它负责从 Riot Games 官方提供的 DataDragon API
 * 获取英雄的基础数据（名字、称号、技能描述、图片等）。
 *
 * 打个比方：
 * - 如果数据管线是一条"从农场到餐桌"的供应链，那么本类就是"去农场采购原材料"的采购员
 * - AramDataCollector 是"去批发市场采购统计数据"的另一个采购员
 * - DataAggregatorService 是"把两批原材料合并加工"的厨师
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、DataDragon API 是什么？
 * ═══════════════════════════════════════════════════════════════════
 *
 * DataDragon 是 Riot Games 官方提供的免费数据接口，包含：
 * - 英雄基础信息（名称、称号、角色定位）
 * - 英雄技能详情（被动、Q/W/E/R 技能描述和图标）
 * - 英雄图片资源（头像、技能图标、被动图标的 URL）
 * - 版本信息（游戏客户端当前使用的版本号）
 *
 * 官方地址：https://ddragon.leagueoflegends.com
 * 无需 API Key，直接 HTTP GET 即可获取数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、API 调用流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * 调用顺序如下（必须按顺序执行）：
 *
 *   第1步：fetchLatestVersion()
 *          → GET /api/versions.json
 *          → 返回版本号列表，取第一个即为最新版本（如 "14.10.1"）
 *          → 为什么需要版本号？因为后续所有 API 都需要指定版本号
 *
 *   第2步：fetchChampionList(version)
 *          → GET /cdn/{version}/data/{locale}/champion.json
 *          → 返回所有英雄的基础信息（名称、Key、角色定位等）
 *          → 返回格式：Map<String, JsonNode>，key 是英雄的英文名（如 "Aatrox"）
 *
 *   第3步：fetchChampionDetail(version, championKey)
 *          → GET /cdn/{version}/data/{locale}/champion/{key}.json
 *          → 返回单个英雄的详细信息（技能、被动、属性等）
 *          → 需要逐个英雄调用，所以通常在第2步获取列表后循环调用
 *
 *   第4步：fetchChampionImages(version, championKey)
 *          → 内部调用 fetchChampionDetail，然后提取图片 URL
 *          → 返回各种图片的完整 URL（头像、被动图标、Q/W/E/R 技能图标）
 *
 *   第5步：extractStats(championData)
 *          → 从英雄详情 JSON 中提取基础属性（攻击/防御/魔法/难度）
 *          → 这些属性是 1~10 的整数评分，用于展示英雄特点
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、异常处理策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类采用"容错优先"策略——任何异常都不中断同步流程：
 * - 网络超时 / 连接失败 → 返回 null 或空 Map + 记录 ERROR 日志
 * - JSON 解析失败 → 返回 null 或空 Map + 记录 ERROR 日志
 * - 响应为空 → 返回 null 或空 Map + 记录 WARN 日志
 *
 * 为什么这样设计？
 * - 数据同步是后台定时任务，不应该因为单个英雄的数据获取失败就中断整个同步
 * - 部分英雄数据缺失时，可以继续同步其他英雄，缺失的下次再补
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、依赖说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 依赖对象              | 作用                           | 打个比方
 * ----------------------|-------------------------------|------------------
 * RestTemplate          | 发送 HTTP 请求                 | 快递员（去取货）
 * ObjectMapper          | 解析 JSON 响应为 Java 对象     | 翻译官（把外文翻译成中文）
 *
 * 配置项（application.yml）：
 * - datadragon.base-url  → DataDragon 基础 URL（默认 https://ddragon.leagueoflegends.com）
 * - datadragon.locale    → 语言区域（默认 zh_CN，即简体中文）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关联类
 * ═══════════════════════════════════════════════════════════════════
 *
 * - DataAggregatorService → 调用本类获取数据后进行聚合处理
 * - DataSyncScheduler     → 定时触发数据同步，间接调用本类
 * - AramDataCollector     → 另一个数据采集器，从 U.GG 采集统计数据
 */
@Slf4j
@Component
public class RiotDataDragonClient {

    /**
     * RestTemplate —— Spring 提供的 HTTP 客户端工具
     *
     * 作用：发送 HTTP GET 请求到 DataDragon API，获取 JSON 响应
     * 为什么用 RestTemplate 而不是 WebClient？
     * - DataDragon API 是简单的请求-响应模式，不需要异步/响应式
     * - RestTemplate 使用简单，适合这种场景
     * - 本项目已在配置类中注册了 RestTemplate Bean（带超时设置）
     */
    private final RestTemplate restTemplate;

    /**
     * ObjectMapper —— Jackson 提供的 JSON 解析工具
     *
     * 作用：把 DataDragon API 返回的 JSON 字符串解析为 JsonNode 树结构
     * 为什么用 JsonNode 而不是直接映射为 Java 类？
     * - DataDragon API 的 JSON 结构比较复杂且可能变化
     * - 使用 JsonNode 可以灵活地按路径读取字段，不需要为每个 API 定义专门的 DTO
     * - 只需要提取部分字段，不需要完整映射
     */
    private final ObjectMapper objectMapper;

    /**
     * DataDragon 基础 URL
     *
     * 从 application.yml 读取，默认值为 https://ddragon.leagueoflegends.com
     * 可覆盖的场景：单元测试时指向本地 Mock 服务器
     */
    @Value("${datadragon.base-url:https://ddragon.leagueoflegends.com}")
    private String baseUrl;

    /**
     * 语言区域设置
     *
     * 从 application.yml 读取，默认值为 zh_CN（简体中文）
     * 可选值：en_US（英文）、ja_JP（日文）、ko_KR（韩文）等
     * 影响：API 返回的英雄名称、技能描述等文本的语言
     */
    @Value("${datadragon.locale:zh_CN}")
    private String locale;

    /**
     * 构造函数 —— Spring 自动注入依赖
     *
     * @param restTemplate HTTP 客户端（由配置类注册的 Bean）
     * @param objectMapper JSON 解析器（Spring Boot 自动配置的 Bean）
     */
    public RiotDataDragonClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取 DataDragon 最新版本号
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 调用 DataDragon 的版本列表 API，获取当前最新的游戏版本号。
     * 版本号是后续所有 API 调用的前提——必须知道版本号才能获取对应版本的数据。
     *
     * ══════════════════════════════════════════════════════════════
     * API 详情
     * ══════════════════════════════════════════════════════════════
     *
     * 请求：GET https://ddragon.leagueoflegends.com/api/versions.json
     * 响应：JSON 数组，按时间倒序排列，第一个元素即为最新版本
     *       示例：["14.10.1", "14.9.1", "14.8.1", ...]
     *
     * ══════════════════════════════════════════════════════════════
     * 处理流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 发送 HTTP GET 请求到 /api/versions.json
     * 2. 检查响应是否为 null（网络异常时可能返回 null）
     * 3. 用 ObjectMapper 解析 JSON 为 JsonNode 数组
     * 4. 取数组第一个元素作为最新版本号
     * 5. 记录 INFO 日志（成功）或 WARN 日志（异常情况）
     *
     * @return 最新版本号字符串（如 "14.10.1"），失败时返回 null
     */
    public String fetchLatestVersion() {
        String url = baseUrl + "/api/versions.json";
        try {
            // 第1步：发送 HTTP GET 请求，获取 JSON 字符串
            String response = restTemplate.getForObject(url, String.class);

            // 第2步：检查响应是否为 null（可能网络异常导致）
            if (response == null) {
                log.warn("DataDragon: versions response is null");
                return null;
            }

            // 第3步：解析 JSON 字符串为 JsonNode 树结构
            JsonNode array = objectMapper.readTree(response);

            // 第4步：检查是否为数组且非空，取第一个元素
            if (array.isArray() && !array.isEmpty()) {
                String version = array.get(0).asText();
                log.info("DataDragon: latest version = {}", version);
                return version;
            }

            // 数组为空的情况（理论上不会发生，但做防御性编程）
            log.warn("DataDragon: versions array is empty");
            return null;
        } catch (RestClientException e) {
            // 网络异常：连接超时、DNS 解析失败、服务器拒绝等
            log.error("DataDragon: failed to fetch versions from {}", url, e);
            return null;
        } catch (Exception e) {
            // JSON 解析异常：返回的不是合法 JSON
            log.error("DataDragon: failed to parse versions response", e);
            return null;
        }
    }

    /**
     * 获取英雄列表（全量）
     *
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 获取指定版本下所有英雄的基础信息列表。
     * 这里的"基础信息"包括：英雄 Key（如 "Aatrox"）、名称、称号、角色定位等。
     * 不包括技能详情（需要调用 fetchChampionDetail 获取）。
     *
     * ══════════════════════════════════════════════════════════════
     * API 详情
     * ══════════════════════════════════════════════════════════════
     *
     * 请求：GET /cdn/{version}/data/{locale}/champion.json
     * 示例：GET /cdn/14.10.1/data/zh_CN/champion.json
     * 响应：JSON 对象，data 字段包含所有英雄信息
     *       {
     *         "type": "champion",
     *         "version": "14.10.1",
     *         "data": {
     *           "Aatrox": { "id": "Aatrox", "key": "266", "name": "亚托克斯", ... },
     *           "Ahri":   { "id": "Ahri",   "key": "103", "name": "阿狸", ... },
     *           ...
     *         }
     *       }
     *
     * ══════════════════════════════════════════════════════════════
     * 返回值说明
     * ══════════════════════════════════════════════════════════════
     *
     * 返回 Map<String, JsonNode>：
     * - key：英雄的英文名（如 "Aatrox"），即 DataDragon 中的 champion key
     * - value：该英雄的完整 JSON 数据（包含 id、key、name、title、tags 等）
     *
     * 使用 LinkedHashMap 保持插入顺序（与 API 返回顺序一致）
     *
     * @param version DataDragon 版本号（由 fetchLatestVersion() 获取）
     * @return 英雄列表，key=championKey（如 "Aatrox"），value=英雄基础信息 JSON
     *         失败时返回空 Map（不是 null）
     */
    public Map<String, JsonNode> fetchChampionList(String version) {
        // 拼接请求 URL：/cdn/{version}/data/{locale}/champion.json
        String url = String.format("%s/cdn/%s/data/%s/champion.json", baseUrl, version, locale);
        try {
            // 第1步：发送 HTTP GET 请求
            String response = restTemplate.getForObject(url, String.class);

            // 第2步：检查响应是否为空
            if (response == null) {
                log.warn("DataDragon: champion list response is null for version={}", version);
                return Collections.emptyMap();
            }

            // 第3步：解析 JSON，定位到 data 字段
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            // 第4步：检查 data 是否为对象（不是数组）
            if (!data.isObject()) {
                log.warn("DataDragon: champion list data node is not an object");
                return Collections.emptyMap();
            }

            // 第5步：遍历 data 对象的所有字段，构建 Map
            Map<String, JsonNode> champions = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                // key = 英雄英文名（如 "Aatrox"），value = 英雄 JSON 数据
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
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 获取单个英雄的详细信息，包括：
     * - 被动技能（passive）：名称、描述、图标
     * - Q/W/E/R 技能（spells）：名称、描述、图标、冷却时间等
     * - 基础属性（info）：攻击/防御/魔法/难度评分
     * - 推荐装备、皮肤列表等
     *
     * ══════════════════════════════════════════════════════════════
     * API 详情
     * ══════════════════════════════════════════════════════════════
     *
     * 请求：GET /cdn/{version}/data/{locale}/champion/{championKey}.json
     * 示例：GET /cdn/14.10.1/data/zh_CN/champion/Aatrox.json
     * 响应：JSON 对象，data.{championKey} 包含英雄详情
     *       {
     *         "data": {
     *           "Aatrox": {
     *             "id": "Aatrox",
     *             "passive": { "name": "死亡之握", ... },
     *             "spells": [ { ... }, { ... }, { ... }, { ... } ],
     *             "info": { "attack": 8, "defense": 4, "magic": 3, "difficulty": 4 }
     *           }
     *         }
     *       }
     *
     * ══════════════════════════════════════════════════════════════
     * 注意事项
     * ══════════════════════════════════════════════════════════════
     *
     * - 每个英雄需要单独调用一次此 API，所以获取全部英雄详情需要循环调用
     * - API 有频率限制（虽然 DataDragon 没有明确限制，但建议控制请求频率）
     * - championKey 区分大小写（如 "Aatrox" 而不是 "aatrox"）
     *
     * @param version     DataDragon 版本号
     * @param championKey 英雄 Key（如 "Aatrox"），必须是精确的英文名
     * @return 英雄详情 JSON 节点（包含 passive、spells、info 等字段），失败时返回 null
     */
    public JsonNode fetchChampionDetail(String version, String championKey) {
        // 拼接请求 URL：/cdn/{version}/data/{locale}/champion/{key}.json
        String url = String.format("%s/cdn/%s/data/%s/champion/%s.json", baseUrl, version, locale, championKey);
        try {
            // 发送 HTTP GET 请求
            String response = restTemplate.getForObject(url, String.class);

            // 检查响应是否为空
            if (response == null) {
                log.warn("DataDragon: champion detail response is null for key={}", championKey);
                return null;
            }

            // 解析 JSON，定位到 data.{championKey}
            JsonNode root = objectMapper.readTree(response);
            JsonNode championData = root.path("data").path(championKey);

            // 检查英雄数据是否存在（championKey 错误时 isMissingNode() 返回 true）
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
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 构建英雄所有图片资源的完整 URL 列表，包括：
     * - avatar：英雄头像（正方形，用于列表展示）
     * - passive：被动技能图标
     * - spell_Q / spell_W / spell_E / spell_R：四个技能图标
     *
     * ══════════════════════════════════════════════════════════════
     * URL 构建规则
     * ══════════════════════════════════════════════════════════════
     *
     * DataDragon 的图片 URL 格式：
     * - 基础路径：{baseUrl}/cdn/{version}/img
     * - 英雄头像：{基础路径}/champion/{championKey}.png
     *   示例：https://ddragon.leagueoflegends.com/cdn/14.10.1/img/champion/Aatrox.png
     * - 被动图标：{基础路径}/passive/{passiveImageName}
     *   示例：https://ddragon.leagueoflegends.com/cdn/14.10.1/img/passive/Aatrox_Passive.png
     * - 技能图标：{基础路径}/spell/{spellImageName}
     *   示例：https://ddragon.leagueoflegends.com/cdn/14.10.1/img/spell/AatroxQ.png
     *
     * ══════════════════════════════════════════════════════════════
     * 处理流程
     * ══════════════════════════════════════════════════════════════
     *
     * 1. 先构建头像 URL（头像 URL 可以直接拼接，不需要查 API）
     * 2. 调用 fetchChampionDetail 获取英雄详情
     * 3. 从详情中提取被动技能图标文件名，拼接被动图标 URL
     * 4. 从详情中提取 Q/W/E/R 技能图标文件名，拼接技能图标 URL
     *
     * @param version     DataDragon 版本号
     * @param championKey 英雄 Key（如 "Aatrox"）
     * @return 图片 URL 映射（key=图片类型，value=完整 URL），失败时返回只含头像 URL 的 Map
     */
    public Map<String, String> fetchChampionImages(String version, String championKey) {
        Map<String, String> images = new LinkedHashMap<>();

        // 构建图片基础路径：{baseUrl}/cdn/{version}/img
        String spriteBaseUrl = String.format("%s/cdn/%s/img", baseUrl, version);

        // 第1步：构建英雄头像 URL（格式固定，不需要查 API）
        // 示例：https://ddragon.leagueoflegends.com/cdn/14.10.1/img/champion/Aatrox.png
        images.put("avatar", String.format("%s/champion/%s.png", spriteBaseUrl, championKey));

        // 第2步：获取英雄详情（从中提取技能和被动的图标文件名）
        JsonNode detail = fetchChampionDetail(version, championKey);
        if (detail == null) {
            // 如果获取详情失败，至少返回头像 URL
            log.warn("DataDragon: cannot fetch images for champion={}, detail is null", championKey);
            return images;
        }

        // 第3步：提取被动技能图标
        // JSON 路径：passive.image.full → 如 "Aatrox_Passive.png"
        JsonNode passive = detail.path("passive");
        if (!passive.isMissingNode() && passive.has("image")) {
            String passiveImage = passive.path("image").path("full").asText("");
            if (!passiveImage.isEmpty()) {
                // 拼接完整 URL：{baseUrl}/cdn/{version}/img/passive/{imageName}
                images.put("passive", String.format("%s/passive/%s", spriteBaseUrl, passiveImage));
            }
        }

        // 第4步：提取 Q/W/E/R 技能图标
        // JSON 路径：spells[0~3].image.full → 如 "AatroxQ.png", "AatroxW.png" 等
        JsonNode spells = detail.path("spells");
        if (spells.isArray()) {
            String[] spellKeys = {"Q", "W", "E", "R"};
            for (int i = 0; i < Math.min(spells.size(), spellKeys.length); i++) {
                String spellImage = spells.get(i).path("image").path("full").asText("");
                if (!spellImage.isEmpty()) {
                    // 拼接完整 URL：{baseUrl}/cdn/{version}/img/spell/{imageName}
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
     * ══════════════════════════════════════════════════════════════
     * 这个方法是干什么的？
     * ══════════════════════════════════════════════════════════════
     *
     * 从英雄详情 JSON 中提取 info 节点的四个属性评分：
     * - attack（攻击）：1~10，表示英雄的物理输出能力
     * - defense（防御）：1~10，表示英雄的承伤能力
     * - magic（魔法）：1~10，表示英雄的魔法输出能力
     * - difficulty（难度）：1~10，表示英雄的操作难度
     *
     * 这些评分是 Riot 官方给出的，用于帮助玩家快速了解英雄特点。
     *
     * @param championData 英雄详情 JSON 节点（来自 fetchChampionDetail 的返回值）
     * @return 属性映射（key=属性名，value=属性值1~10），失败时返回空 Map
     */
    public Map<String, Integer> extractStats(JsonNode championData) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        if (championData == null) {
            return stats;
        }

        // 从 JSON 的 info 节点提取四个属性
        JsonNode info = championData.path("info");
        if (info.isObject()) {
            putIfPresent(stats, info, "attack", "attack");
            putIfPresent(stats, info, "defense", "defense");
            putIfPresent(stats, info, "magic", "magic");
            putIfPresent(stats, info, "difficulty", "difficulty");
        }

        return stats;
    }

    /**
     * 辅助方法：如果 JSON 节点中存在指定字段，则将其值放入 Map
     *
     * @param map    目标 Map
     * @param node   JSON 节点
     * @param jsonKey JSON 中的字段名
     * @param mapKey  放入 Map 时使用的 key
     */
    private void putIfPresent(Map<String, Integer> map, JsonNode node, String jsonKey, String mapKey) {
        if (node.has(jsonKey)) {
            map.put(mapKey, node.get(jsonKey).asInt());
        }
    }
}
