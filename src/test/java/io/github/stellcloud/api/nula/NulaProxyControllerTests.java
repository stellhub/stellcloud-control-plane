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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Nula 页面控制器测试。 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
        assertThat(response.getBody()).contains("\"environments\"").contains("\"groups\"").contains("payments");
        assertThat(DOWNSTREAM_REQUESTS)
                .contains("GET /api/v1/control-plane/app-config/scope?appId=acme.retail.checkout.order.admin");
    }

    /** 验证应用配置列表接口透传到 stellnula-service 控制面 API。 */
    @Test
    void listConfigsProxiesToNulaService() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/configs?appId=acme.retail.checkout.order.admin&group=payments",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"records\"").contains("app-config-json").contains("\"group\":\"payments\"");
        assertThat(DOWNSTREAM_REQUESTS)
                .contains("GET /api/v1/control-plane/app-config?appId=acme.retail.checkout.order.admin&group=payments");
    }

    /** 验证创建、保存、发布和删除接口都透传到 stellnula-service。 */
    @Test
    void createSavePublishAndDeleteConfigProxyToNulaService() {
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
                  "group": "payments",
                  "format": "yaml",
                  "content": "app:\\n  enabled: true\\n",
                  "updatedBy": "xiaoy"
                }
                """;

        var createResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/configs",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).contains("\"status\":\"draft\"");

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
                        "POST /api/v1/control-plane/app-config",
                        "PUT /api/v1/control-plane/app-config/config-controller-test",
                        "POST /api/v1/control-plane/app-config/config-controller-test/publish");
        assertThat(DOWNSTREAM_REQUESTS)
                .anyMatch(request -> request.startsWith("DELETE /api/v1/control-plane/app-config/config-controller-test?")
                        && request.contains("appId=acme.retail.checkout.order.admin")
                        && request.contains("environment=prod")
                        && request.contains("cluster=default"));
        assertThat(DOWNSTREAM_BODIES).anyMatch(body -> body.contains("\"name\":\"controller.yaml\"")
                && body.contains("\"group\":\"payments\""));
    }

    /** 验证公共配置接口透传到 stellnula-service 控制面 API 并返回 VO。 */
    @Test
    void commonConfigApisProxyToNulaService() {
        var scopeResponse = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/common-config/scope?ownerId=global",
                String.class);
        assertThat(scopeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(scopeResponse.getBody()).contains("\"groups\"").contains("platform");

        var listResponse = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/common-config?ownerId=global&environment=prod&group=platform",
                String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody())
                .contains("\"ownerId\":\"global\"")
                .contains("\"group\":\"platform\"");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload =
                """
                {
                  "id": "common-config-test",
                  "ownerId": "global",
                  "name": "gateway.yaml",
                  "description": "gateway defaults",
                  "environment": "prod",
                  "cluster": "default",
                  "group": "platform",
                  "format": "yaml",
                  "content": "gateway:\\n  enabled: true\\n",
                  "updatedBy": "xiaoy"
                }
                """;

        var createResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/common-config",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).contains("\"status\":\"draft\"");

        var saveResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/common-config/common-config-test",
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saveResponse.getBody()).contains("\"status\":\"draft\"");

        var publishResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/common-config/common-config-test/publish",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody()).contains("\"status\":\"published\"");

        var deleteResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/common-config/common-config-test"
                        + "?ownerId=global&environment=prod&cluster=default",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(DOWNSTREAM_REQUESTS)
                .contains(
                        "POST /api/v1/control-plane/common-config",
                        "PUT /api/v1/control-plane/common-config/common-config-test",
                        "POST /api/v1/control-plane/common-config/common-config-test/publish");
        assertThat(DOWNSTREAM_REQUESTS)
                .anyMatch(request -> request.startsWith("DELETE /api/v1/control-plane/common-config/common-config-test?")
                        && request.contains("ownerId=global")
                        && request.contains("environment=prod")
                        && request.contains("cluster=default"));
    }

    /** 验证 Feature Flag 接口透传到 stellnula-service 控制面 API 并返回 VO。 */
    @Test
    void featureFlagApisProxyToNulaService() {
        var listResponse = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/feature-flags"
                        + "?appId=acme.retail.checkout.order.admin&environment=prod&group=feature-flags.checkout",
                String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody())
                .contains("\"key\":\"checkout.new-flow\"")
                .contains("\"defaultValue\":false");

        var detailResponse = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/nula/feature-flags/checkout.new-flow"
                        + "?appId=acme.retail.checkout.order.admin",
                String.class);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).contains("\"group\":\"feature-flags.checkout\"");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload =
                """
                {
                  "appId": "acme.retail.checkout.order.admin",
                  "key": "checkout.new-flow",
                  "description": "checkout rollout",
                  "environment": "prod",
                  "cluster": "default",
                  "group": "feature-flags.checkout",
                  "type": "BOOLEAN",
                  "enabled": true,
                  "defaultValue": false,
                  "updatedBy": "xiaoy"
                }
                """;

        var createResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/feature-flags",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).contains("\"status\":\"draft\"");

        var saveResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/feature-flags/checkout.new-flow",
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saveResponse.getBody()).contains("\"status\":\"draft\"");

        var publishResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/feature-flags/checkout.new-flow/publish",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody()).contains("\"status\":\"published\"");

        var deleteResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/nula/feature-flags/checkout.new-flow"
                        + "?appId=acme.retail.checkout.order.admin&environment=prod&cluster=default",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(DOWNSTREAM_REQUESTS)
                .contains(
                        "POST /api/v1/control-plane/feature-flags",
                        "PUT /api/v1/control-plane/feature-flags/checkout.new-flow",
                        "POST /api/v1/control-plane/feature-flags/checkout.new-flow/publish");
        assertThat(DOWNSTREAM_BODIES).anyMatch(body -> body.contains("\"defaultValue\":false")
                && body.contains("\"group\":\"feature-flags.checkout\""));
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
        if ("GET".equals(method) && "/api/v1/control-plane/app-config/scope".equals(path)) {
            return """
                    {
                      "environments": ["prod", "staging"],
                      "clustersByEnvironment": {
                        "prod": ["default", "cn-east-1"],
                        "staging": ["default"]
                      },
                      "groups": ["default", "payments"]
                    }
                    """;
        }
        if ("GET".equals(method)
                && ("/api/v1/configs".equals(path) || "/api/v1/control-plane/app-config".equals(path))) {
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
                          "group": "payments",
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
        if ("GET".equals(method) && "/api/v1/control-plane/common-config".equals(path)) {
            return commonConfigListResponse();
        }
        if ("GET".equals(method) && "/api/v1/control-plane/common-config/scope".equals(path)) {
            return """
                    {
                      "environments": ["prod"],
                      "clustersByEnvironment": {
                        "prod": ["default"]
                      },
                      "groups": ["platform"]
                    }
                    """;
        }
        if ("POST".equals(method) && "/api/v1/control-plane/common-config".equals(path)) {
            return commonConfigResponse("draft");
        }
        if (path.startsWith("/api/v1/control-plane/common-config/")) {
            return commonConfigResponse(path.endsWith("/publish") ? "published" : "draft");
        }
        if ("GET".equals(method) && "/api/v1/control-plane/feature-flags".equals(path)) {
            return featureFlagListResponse();
        }
        if ("POST".equals(method) && "/api/v1/control-plane/feature-flags".equals(path)) {
            return featureFlagResponse("draft");
        }
        if (path.startsWith("/api/v1/control-plane/feature-flags/")) {
            return featureFlagResponse(path.endsWith("/publish") ? "published" : "draft");
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
                  "group": "payments",
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

    private static String commonConfigListResponse() {
        return """
                {
                  "records": [
                    {
                      "id": "common-config-test",
                      "ownerId": "global",
                      "name": "gateway.yaml",
                      "description": "gateway defaults",
                      "environment": "prod",
                      "cluster": "default",
                      "group": "platform",
                      "format": "yaml",
                      "formatLocked": false,
                      "content": "gateway:\\n  enabled: true\\n",
                      "version": "v1",
                      "status": "draft",
                      "updatedBy": "xiaoy",
                      "updatedAt": "2026-06-10T00:00:00Z",
                      "publishedAt": null
                    }
                  ]
                }
                """;
    }

    private static String commonConfigResponse(String status) {
        return """
                {
                  "id": "common-config-test",
                  "ownerId": "global",
                  "name": "gateway.yaml",
                  "description": "gateway defaults",
                  "environment": "prod",
                  "cluster": "default",
                  "group": "platform",
                  "format": "yaml",
                  "formatLocked": true,
                  "content": "gateway:\\n  enabled: true\\n",
                  "version": "v2",
                  "status": "%s",
                  "updatedBy": "xiaoy",
                  "updatedAt": "2026-06-10T00:00:00Z",
                  "publishedAt": %s
                }
                """
                .formatted(status, "published".equals(status) ? "\"2026-06-10T00:00:00Z\"" : "null");
    }

    private static String featureFlagListResponse() {
        return """
                {
                  "records": [
                    {
                      "id": "feature.checkout.new-flow",
                      "appId": "acme.retail.checkout.order.admin",
                      "key": "checkout.new-flow",
                      "name": "checkout.new-flow.json",
                      "description": "checkout rollout",
                      "environment": "prod",
                      "cluster": "default",
                      "group": "feature-flags.checkout",
                      "type": "BOOLEAN",
                      "enabled": true,
                      "defaultValue": false,
                      "rules": [],
                      "variants": null,
                      "rollout": null,
                      "content": "{\\"key\\":\\"checkout.new-flow\\"}",
                      "version": "v1",
                      "status": "draft",
                      "updatedBy": "xiaoy",
                      "updatedAt": "2026-06-10T00:00:00Z",
                      "publishedAt": null
                    }
                  ]
                }
                """;
    }

    private static String featureFlagResponse(String status) {
        return """
                {
                  "id": "feature.checkout.new-flow",
                  "appId": "acme.retail.checkout.order.admin",
                  "key": "checkout.new-flow",
                  "name": "checkout.new-flow.json",
                  "description": "checkout rollout",
                  "environment": "prod",
                  "cluster": "default",
                  "group": "feature-flags.checkout",
                  "type": "BOOLEAN",
                  "enabled": true,
                  "defaultValue": false,
                  "rules": [],
                  "variants": null,
                  "rollout": null,
                  "content": "{\\"key\\":\\"checkout.new-flow\\"}",
                  "version": "v2",
                  "status": "%s",
                  "updatedBy": "xiaoy",
                  "updatedAt": "2026-06-10T00:00:00Z",
                  "publishedAt": %s
                }
                """
                .formatted(status, "published".equals(status) ? "\"2026-06-10T00:00:00Z\"" : "null");
    }
}
