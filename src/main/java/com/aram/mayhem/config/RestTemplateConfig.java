package com.aram.mayhem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate HTTP 客户端配置类
 *
 * RestTemplate 是 Spring 提供的 HTTP 客户端工具，用于在 Java 代码中发送 HTTP 请求。
 * 就像浏览器可以访问网页一样，RestTemplate 可以让我们的后端去访问其他服务器的接口。
 *
 * 本项目使用场景：
 * - RiotDataDragonClient：调用 Riot 官方的 DataDragon API，获取英雄和符文的基础数据
 * - AramDataCollector：调用 U.GG 等第三方数据源，获取 ARAM 模式的胜率统计
 *
 * 为什么需要自定义配置？
 * Spring Boot 不再自动配置 RestTemplate（从 Spring Boot 1.4 开始），
 * 我们需要手动创建 Bean，并设置合理的超时时间，避免外部 API 响应慢时拖垮我们的服务。
 *
 * 超时配置说明：
 * - 连接超时（connectTimeout）：建立 TCP 连接的最大等待时间
 *   - 如果对方服务器宕机或网络不通，30 秒后就会报错，而不是一直等下去
 * - 读取超时（readTimeout）：等待服务器返回数据的最大时间
 *   - 如果连接已建立但对方处理太慢，30 秒后也会超时
 * - 数据同步是后台批处理任务，允许较长的超时时间（30 秒）
 * - 如果是面向用户的接口，超时时间应该设置得更短（如 5-10 秒）
 *
 * 关联类：
 * - RiotDataDragonClient：调用 Riot API 获取英雄/符文数据
 * - AramDataCollector：调用第三方 API 获取 ARAM 统计数据
 */
@Configuration // 声明这是一个 Spring 配置类
public class RestTemplateConfig {

    // 从 application.yml 读取连接超时时间，默认 30 秒
    // @Value 注解会从配置文件中找 datadragon.connect-timeout 的值，找不到就用默认值 30
    @Value("${datadragon.connect-timeout:30}")
    private int connectTimeout;

    // 从 application.yml 读取读取超时时间，默认 30 秒
    @Value("${datadragon.read-timeout:30}")
    private int readTimeout;

    /**
     * 创建 RestTemplate Bean —— HTTP 客户端工具
     *
     * RestTemplate 是线程安全的，整个应用只需要一个实例（单例 Bean）。
     * 所有需要发送 HTTP 请求的地方，都可以注入这个 Bean 来使用。
     *
     * @return 配置好超时时间的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        // 创建请求工厂 —— 用于配置 HTTP 请求的底层参数
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时：单位是毫秒，所以乘以 1000 把秒转换为毫秒
        // 30 * 1000 = 30000 毫秒 = 30 秒
        factory.setConnectTimeout(connectTimeout * 1000);
        // 设置读取超时：同样转换为毫秒
        factory.setReadTimeout(readTimeout * 1000);
        // 用配置好的工厂创建 RestTemplate
        return new RestTemplate(factory);
    }
}
