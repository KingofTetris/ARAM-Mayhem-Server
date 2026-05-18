package com.aram.mayhem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate HTTP 客户端配置
 *
 * 功能：提供统一超时配置的 RestTemplate Bean
 * 用途：RiotDataDragonClient / AramDataCollector 调用外部 API
 * 超时：连接 30s / 读取 30s（数据同步为后台批处理，允许较长超时）
 * 关联：client/RiotDataDragonClient.java, client/AramDataCollector.java
 */
@Configuration
public class RestTemplateConfig {

    @Value("${datadragon.connect-timeout:30}")
    private int connectTimeout;

    @Value("${datadragon.read-timeout:30}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout * 1000);
        factory.setReadTimeout(readTimeout * 1000);
        return new RestTemplate(factory);
    }
}
