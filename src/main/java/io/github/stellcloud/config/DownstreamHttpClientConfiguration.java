package io.github.stellcloud.config;

import io.github.stellflux.http.client.StellfluxHttpClient;
import io.github.stellflux.http.client.StellfluxHttpClientFactory;
import io.github.stellflux.http.client.StellfluxHttpClientOptions;
import io.github.stellflux.http.client.StellfluxHttpClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 下游服务 HTTP 客户端配置。 */
@Configuration
public class DownstreamHttpClientConfiguration {

    private static final String STELLORBIT_SERVICE = "stellorbit-service";
    private static final String STELLNULA_SERVICE = "stellnula-service";
    private static final String STELLATLAS_SERVICE = "stellatlas-service";
    private static final String STELLFLOW_SERVICE = "stellflow-service";
    private static final String STELLMAP_SERVICE = "stellmap-service";

    /**
     * 创建服务治理 HTTP 客户端。
     *
     * @param factory Stellflux HTTP client factory
     * @param properties HTTP client properties
     * @return 服务治理 HTTP 客户端
     */
    @Bean
    @Qualifier("orbitHttpClient")
    public StellfluxHttpClient orbitHttpClient(
            StellfluxHttpClientFactory factory, StellfluxHttpClientProperties properties) {
        return createClient(factory, properties, STELLORBIT_SERVICE, "http://127.0.0.1:18081");
    }

    /**
     * 创建配置中心 HTTP 客户端。
     *
     * @param factory Stellflux HTTP client factory
     * @param properties HTTP client properties
     * @return 配置中心 HTTP 客户端
     */
    @Bean
    @Qualifier("nulaHttpClient")
    public StellfluxHttpClient nulaHttpClient(
            StellfluxHttpClientFactory factory, StellfluxHttpClientProperties properties) {
        return createClient(factory, properties, STELLNULA_SERVICE, "http://127.0.0.1:8060");
    }

    /**
     * 创建 CMDB HTTP 客户端。
     *
     * @param factory Stellflux HTTP client factory
     * @param properties HTTP client properties
     * @return CMDB HTTP 客户端
     */
    @Bean
    @Qualifier("atlasHttpClient")
    public StellfluxHttpClient atlasHttpClient(
            StellfluxHttpClientFactory factory, StellfluxHttpClientProperties properties) {
        return createClient(factory, properties, STELLATLAS_SERVICE, "http://127.0.0.1:8080");
    }

    /**
     * 创建消息队列 HTTP 客户端。
     *
     * @param factory Stellflux HTTP client factory
     * @param properties HTTP client properties
     * @return 消息队列 HTTP 客户端
     */
    @Bean
    @Qualifier("flowHttpClient")
    public StellfluxHttpClient flowHttpClient(
            StellfluxHttpClientFactory factory, StellfluxHttpClientProperties properties) {
        return createClient(factory, properties, STELLFLOW_SERVICE, "http://127.0.0.1:18083");
    }

    /**
     * 创建注册中心 HTTP 客户端。
     *
     * @param factory Stellflux HTTP client factory
     * @param properties HTTP client properties
     * @return 注册中心 HTTP 客户端
     */
    @Bean
    @Qualifier("mapHttpClient")
    public StellfluxHttpClient mapHttpClient(
            StellfluxHttpClientFactory factory, StellfluxHttpClientProperties properties) {
        return createClient(factory, properties, STELLMAP_SERVICE, "http://127.0.0.1:18084");
    }

    private StellfluxHttpClient createClient(
            StellfluxHttpClientFactory factory,
            StellfluxHttpClientProperties properties,
            String serviceId,
            String defaultBaseUrl) {
        StellfluxHttpClientProperties.ClientProperties clientProperties =
                properties.getClients().get(serviceId);
        if (clientProperties == null) {
            clientProperties = new StellfluxHttpClientProperties.ClientProperties();
            clientProperties.setBaseUrl(defaultBaseUrl);
        }
        StellfluxHttpClientOptions options = clientProperties.toOptions(serviceId);
        if (options.getBaseUrl() == null || options.getBaseUrl().isBlank()) {
            options.setBaseUrl(defaultBaseUrl);
        }
        return factory.create(options);
    }
}
