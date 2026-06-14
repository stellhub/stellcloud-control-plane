package io.github.stellcloud.api.nula;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Nula 页面控制器测试。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NulaProxyControllerTests {

    private static final Queue<String> DOWNSTREAM_REQUESTS = new ConcurrentLinkedQueue<>();
    private static final Queue<String> DOWNSTREAM_BODIES = new ConcurrentLinkedQueue<>();
    private static HttpServer downstreamServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void clearDownstreamRequests() {
        DOWNSTREAM_REQUESTS.clear();
        DOWNSTREAM_BODIES.clear();
    }

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) throws IOException {
        startDownstreamServer();
        registry.add(
                "stellflux.http.client.clients.stellnula-service.base-url",
                () -> "http://127.0.0.1:" + downstreamServer.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstreamServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    /** 验证健康检查接口透传到 stellnula-service 控制面 API。 */
    @Test
    void healthProxiesToNulaService() {
        var response =
                restTemplate.getForEntity("/api/stellcloud/control-plane/v1/nula/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"component\":\"stellnula-service\"");
        assertThat(DOWNSTREAM_REQUESTS).contains("GET /api/v1/control-plane/health");
    }

    /** 验证配置范围接口透传到 stellnula-service 控制面 API。 */
    @Test
    void scopeProxiesToNulaService() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/configs/scope?appId=acme.retail.checkout.order.admin",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"environments\"").contains("prod");
        assertThat(DOWNSTREAM_REQUESTS)
                .contains("GET /api/v1/control-plane/configs/scope?appId=acme.retail.checkout.order.admin");
    }

    /** 验证应用配置列表接口透传到 stellnula-service 控制面 API。 */
    @Test
    void listConfigsProxiesToNulaService() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/configs?appId=acme.retail.checkout.order.admin",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"records\"").contains("app-config-json");
        assertThat(DOWNSTREAM_REQUESTS)
                .contains("GET /api/v1/control-plane/configs?appId=acme.retail.checkout.order.admin");
    }

    /** 验证保存、发布和删除接口都透传到 stellnula-service。 */
    @Test
    void savePublishAndDeleteConfigProxyToNulaService() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload =
                """
                {
                  "id": "config-controller-test",
                  "appId": "acme.retail.checkout.order.admin",
                  "name": "controller.yaml",
                  "description": "controller test",
                  "environment": "prod",
                  "cluster": "default",
                  "format": "yaml",
                  "content": "app:\\n  enabled: true\\n",
                  "updatedBy": "xiaoy"
                }
                """;

        var saveResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/configs/config-controller-test",
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saveResponse.getBody()).contains("\"status\":\"draft\"");

        var publishResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/configs/config-controller-test/publish",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody())
                .contains("\"status\":\"published\"")
                .contains("\"formatLocked\":true");

        var deleteResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/configs/config-controller-test"
                        + "?appId=acme.retail.checkout.order.admin&environment=prod&cluster=default",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(DOWNSTREAM_REQUESTS)
                .contains(
                        "PUT /api/v1/control-plane/configs/config-controller-test",
                        "POST /api/v1/control-plane/configs/config-controller-test/publish");
        assertThat(DOWNSTREAM_REQUESTS)
                .anyMatch(request -> request.startsWith("DELETE /api/v1/control-plane/configs/config-controller-test?")
                        && request.contains("appId=acme.retail.checkout.order.admin")
                        && request.contains("environment=prod")
                        && request.contains("cluster=default"));
        assertThat(DOWNSTREAM_BODIES).anyMatch(body -> body.contains("\"name\":\"controller.yaml\""));
    }

    /** 验证通用配置页面返回页面展示 VO。 */
    @Test
    void sharedConfigPageReturnsViewObject() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/shared-config/page",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"title\":\"通用配置\"")
                .contains("\"tableTitle\":\"通用配置组\"")
                .contains("\"actions\":[\"新建配置组\",\"查看应用\",\"同步发布\",\"冲突检测\"]");
        assertThat(DOWNSTREAM_REQUESTS).isEmpty();
    }

    /** 验证 proxy 路径仍然保留下游透传能力。 */
    @Test
    void proxyPathStillForwardsToNulaService() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/proxy/configs?tenant=acme",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"records\"");
        assertThat(DOWNSTREAM_REQUESTS).contains("GET /api/v1/configs?tenant=acme");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload = "{\"name\":\"proxy.yaml\"}";
        restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/proxy/configs/config-proxy-test",
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(DOWNSTREAM_BODIES).anyMatch(body -> body.contains("\"name\":\"proxy.yaml\""));
    }

    private static void startDownstreamServer() throws IOException {
        if (downstreamServer != null) {
            return;
        }
        downstreamServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstreamServer.createContext("/", NulaProxyControllerTests::handleDownstreamRequest);
        downstreamServer.start();
    }

    private static void handleDownstreamRequest(HttpExchange exchange) throws IOException {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        String requestLine = exchange.getRequestMethod()
                + " "
                + exchange.getRequestURI().getRawPath()
                + (rawQuery == null ? "" : "?" + rawQuery);
        DOWNSTREAM_REQUESTS.add(requestLine);
        DOWNSTREAM_BODIES.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        if ("DELETE".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(HttpStatus.NO_CONTENT.value(), -1);
            exchange.close();
            return;
        }

        String responseBody = downstreamResponse(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath());
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(HttpStatus.OK.value(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static String downstreamResponse(String method, String path) {
        if ("GET".equals(method) && "/api/v1/control-plane/health".equals(path)) {
            return "{\"status\":\"UP\",\"component\":\"stellnula-service\"}";
        }
        if ("GET".equals(method) && "/api/v1/control-plane/configs/scope".equals(path)) {
            return """
                    {
                      "environments": ["prod", "staging"],
                      "clustersByEnvironment": {
                        "prod": ["default", "cn-east-1"],
                        "staging": ["default"]
                      }
                    }
                    """;
        }
        if ("GET".equals(method)
                && ("/api/v1/configs".equals(path) || "/api/v1/control-plane/configs".equals(path))) {
            return """
                    {
                      "records": [
                        {
                          "id": "app-config-json",
                          "appId": "acme.retail.checkout.order.admin",
                          "name": "order-service.json",
                          "description": "订单服务运行参数。",
                          "environment": "prod",
                          "cluster": "cn-east-1",
                          "format": "json",
                          "formatLocked": true,
                          "content": "{}",
                          "version": "v3",
                          "status": "published",
                          "updatedBy": "platform",
                          "updatedAt": "2026-06-10T00:00:00Z",
                          "publishedAt": "2026-06-10T00:00:00Z"
                        }
                      ]
                    }
                    """;
        }
        if ("POST".equals(method) && path.endsWith("/publish")) {
            return configResponse("published", true);
        }
        return configResponse("draft", false);
    }

    private static String configResponse(String status, boolean formatLocked) {
        return """
                {
                  "id": "config-controller-test",
                  "appId": "acme.retail.checkout.order.admin",
                  "name": "controller.yaml",
                  "description": "controller test",
                  "environment": "prod",
                  "cluster": "default",
                  "format": "yaml",
                  "formatLocked": %s,
                  "content": "app:\\n  enabled: true\\n",
                  "version": "v1",
                  "status": "%s",
                  "updatedBy": "xiaoy",
                  "updatedAt": "2026-06-10T00:00:00Z",
                  "publishedAt": %s
                }
                """
                .formatted(formatLocked, status, formatLocked ? "\"2026-06-10T00:00:00Z\"" : "null");
    }
}
