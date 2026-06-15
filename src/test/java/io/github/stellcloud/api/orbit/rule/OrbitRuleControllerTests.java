package io.github.stellcloud.api.orbit.rule;

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

/** Orbit 服务治理规则控制器测试。 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "stellcloud.test.slice=orbit-rule")
class OrbitRuleControllerTests {

    private static final String ROUTE_RULE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String BREAKER_RULE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String RATE_LIMIT_RULE_ID = "33333333-3333-3333-3333-333333333333";
    private static final String AUTH_RULE_ID = "44444444-4444-4444-4444-444444444444";
    private static final String INSTANCE_SPACE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String APPLICATION_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    private static final Queue<String> DOWNSTREAM_REQUESTS = new ConcurrentLinkedQueue<>();
    private static final Queue<String> DOWNSTREAM_BODIES = new ConcurrentLinkedQueue<>();
    private static final Queue<String> DOWNSTREAM_TENANTS = new ConcurrentLinkedQueue<>();
    private static HttpServer downstreamServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void clearDownstreamRequests() {
        DOWNSTREAM_REQUESTS.clear();
        DOWNSTREAM_BODIES.clear();
        DOWNSTREAM_TENANTS.clear();
    }

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) throws IOException {
        startDownstreamServer();
        registry.add(
                "stellflux.http.client.clients.stellorbit-service.base-url",
                () -> "http://127.0.0.1:" + downstreamServer.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstreamServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    /** 验证路由规则 CRUD 使用分类接口并返回页面 VO。 */
    @Test
    void routeRuleCrudApisReturnViewObject() {
        var listResponse = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/orbit/rules/routes",
                String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody())
                .contains("\"records\"")
                .contains("\"ruleType\":\"ROUTE\"")
                .contains("\"routeType\":\"HTTP\"");

        HttpHeaders headers = controlPlaneHeaders();
        var createResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rules/routes",
                HttpMethod.POST,
                new HttpEntity<>(routePayload(), headers),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).contains("\"ruleCode\":\"route.checkout\"");

        var updateResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rules/routes/" + ROUTE_RULE_ID,
                HttpMethod.PATCH,
                new HttpEntity<>(routePayload(), headers),
                String.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains("\"protocol\":\"HTTP\"");

        var deleteResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rules/routes/" + ROUTE_RULE_ID,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(DOWNSTREAM_REQUESTS)
                .contains(
                        "GET /api/stellorbit/rules/routes",
                        "POST /api/stellorbit/rules/routes",
                        "PATCH /api/stellorbit/rules/routes/" + ROUTE_RULE_ID,
                        "DELETE /api/stellorbit/rules/routes/" + ROUTE_RULE_ID);
        assertThat(DOWNSTREAM_BODIES).anyMatch(body -> body.contains("\"ruleCode\":\"route.checkout\"")
                && body.contains("\"routeType\":\"HTTP\""));
        assertThat(DOWNSTREAM_TENANTS).contains("tenant-a");
    }

    /** 验证熔断、限流、鉴权规则都按分类对接下游 CRUD 路径。 */
    @Test
    void breakerRateLimitAndAuthRuleApisUseCategoryPaths() {
        HttpHeaders headers = controlPlaneHeaders();

        var breakerResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rules/breakers",
                HttpMethod.POST,
                new HttpEntity<>(breakerPayload(), headers),
                String.class);
        assertThat(breakerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(breakerResponse.getBody()).contains("\"breakerType\":\"ERROR_RATE\"");

        var rateLimitResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rules/rate-limits/" + RATE_LIMIT_RULE_ID,
                HttpMethod.PATCH,
                new HttpEntity<>(rateLimitPayload(), headers),
                String.class);
        assertThat(rateLimitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rateLimitResponse.getBody()).contains("\"limitAlgorithm\":\"TOKEN_BUCKET\"");

        var authResponse = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/orbit/rules/auth/" + AUTH_RULE_ID,
                String.class);
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authResponse.getBody()).contains("\"authPolicyType\":\"JWT\"");

        restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rules/auth/" + AUTH_RULE_ID,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);

        assertThat(DOWNSTREAM_REQUESTS)
                .contains(
                        "POST /api/stellorbit/rules/breakers",
                        "PATCH /api/stellorbit/rules/rate-limits/" + RATE_LIMIT_RULE_ID,
                        "GET /api/stellorbit/rules/auth/" + AUTH_RULE_ID,
                        "DELETE /api/stellorbit/rules/auth/" + AUTH_RULE_ID);
    }

    private static void startDownstreamServer() throws IOException {
        if (downstreamServer != null) {
            return;
        }
        downstreamServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstreamServer.createContext("/", OrbitRuleControllerTests::handleDownstreamRequest);
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
        String tenantId = exchange.getRequestHeaders().getFirst("X-Stellorbit-Tenant-Id");
        if (tenantId != null) {
            DOWNSTREAM_TENANTS.add(tenantId);
        }

        if ("DELETE".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(HttpStatus.NO_CONTENT.value(), -1);
            exchange.close();
            return;
        }

        String responseBody = downstreamResponse(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath());
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        int status = "POST".equals(exchange.getRequestMethod()) ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static String downstreamResponse(String method, String path) {
        if ("GET".equals(method) && "/api/stellorbit/rules/routes".equals(path)) {
            return "[" + aggregate("ROUTE", routeDetail()) + "]";
        }
        if (path.startsWith("/api/stellorbit/rules/routes")) {
            return aggregate("ROUTE", routeDetail());
        }
        if (path.startsWith("/api/stellorbit/rules/breakers")) {
            return aggregate("BREAKER", breakerDetail());
        }
        if (path.startsWith("/api/stellorbit/rules/rate-limits")) {
            return aggregate("RATE_LIMIT", rateLimitDetail());
        }
        if (path.startsWith("/api/stellorbit/rules/auth")) {
            return aggregate("AUTH", authDetail());
        }
        return "{}";
    }

    private static String aggregate(String ruleType, String detail) {
        String ruleId = switch (ruleType) {
            case "BREAKER" -> BREAKER_RULE_ID;
            case "RATE_LIMIT" -> RATE_LIMIT_RULE_ID;
            case "AUTH" -> AUTH_RULE_ID;
            default -> ROUTE_RULE_ID;
        };
        return """
                {
                  "rule": {
                    "id": "%s",
                    "instanceSpaceId": "%s",
                    "applicationId": "%s",
                    "ruleCode": "%s.checkout",
                    "ruleName": "%s checkout rule",
                    "ruleType": "%s",
                    "sourceFormat": "CUE",
                    "runtimeFormat": "JSON",
                    "checksum": "sha256:test",
                    "priority": 100,
                    "enabled": true,
                    "status": "DRAFT",
                    "draftVersion": 1,
                    "latestReleaseId": null,
                    "description": "controller test",
                    "tags": ["checkout"],
                    "createdBy": "xiaoy",
                    "updatedBy": "xiaoy",
                    "createdAt": "2026-06-10T00:00:00Z",
                    "updatedAt": "2026-06-10T00:00:00Z",
                    "publishedAt": null
                  },
                  "detail": %s
                }
                """.formatted(ruleId, INSTANCE_SPACE_ID, APPLICATION_ID, ruleType.toLowerCase(), ruleType, ruleType, detail);
    }

    private static String routeDetail() {
        return """
                {
                  "id": "%s",
                  "routeType": "HTTP",
                  "trafficDirection": "INBOUND",
                  "protocol": "HTTP",
                  "gateways": [],
                  "hosts": ["checkout.example.com"],
                  "sourceSelector": {},
                  "matchConditions": [],
                  "destinations": [],
                  "routeAction": {},
                  "rewritePolicy": {},
                  "redirectPolicy": {},
                  "mirrorPolicy": {},
                  "faultInjectionPolicy": {},
                  "timeoutPolicy": {},
                  "retryPolicy": {},
                  "loadBalancePolicy": {},
                  "localityPolicy": {},
                  "createdAt": "2026-06-10T00:00:00Z",
                  "updatedAt": "2026-06-10T00:00:00Z"
                }
                """.formatted(ROUTE_RULE_ID);
    }

    private static String breakerDetail() {
        return """
                {
                  "id": "%s",
                  "breakerType": "ERROR_RATE",
                  "protocol": "HTTP",
                  "targetSelector": {},
                  "windowType": "SLIDING",
                  "windowSize": 60,
                  "minimumCalls": 20,
                  "failureRateThreshold": 50,
                  "slowCallRateThreshold": 30,
                  "slowCallDurationMillis": 1000,
                  "openStateWaitMillis": 30000,
                  "permittedHalfOpenCalls": 5,
                  "connectionPoolPolicy": {},
                  "outlierDetectionPolicy": {},
                  "retryBudgetPolicy": {},
                  "exceptionRecordPolicy": {},
                  "exceptionIgnorePolicy": {},
                  "fallbackPolicy": {},
                  "createdAt": "2026-06-10T00:00:00Z",
                  "updatedAt": "2026-06-10T00:00:00Z"
                }
                """.formatted(BREAKER_RULE_ID);
    }

    private static String rateLimitDetail() {
        return """
                {
                  "id": "%s",
                  "limitType": "REQUEST",
                  "limitAlgorithm": "TOKEN_BUCKET",
                  "enforcementMode": "BLOCK",
                  "targetSelector": {},
                  "dimensions": [],
                  "quotaConfig": {},
                  "windowConfig": {},
                  "burstConfig": {},
                  "modelLimitConfig": {},
                  "fallbackPolicy": {},
                  "responsePolicy": {},
                  "createdAt": "2026-06-10T00:00:00Z",
                  "updatedAt": "2026-06-10T00:00:00Z"
                }
                """.formatted(RATE_LIMIT_RULE_ID);
    }

    private static String authDetail() {
        return """
                {
                  "id": "%s",
                  "authPolicyType": "JWT",
                  "authAction": "ALLOW",
                  "mtlsMode": "STRICT",
                  "trustDomain": "cluster.local",
                  "workloadSelector": {},
                  "peerSources": [],
                  "requestAuthentications": [],
                  "authorizationFrom": [],
                  "authorizationTo": [],
                  "authorizationWhen": [],
                  "jwtRules": [],
                  "extAuthzProvider": null,
                  "auditPolicy": {},
                  "createdAt": "2026-06-10T00:00:00Z",
                  "updatedAt": "2026-06-10T00:00:00Z"
                }
                """.formatted(AUTH_RULE_ID);
    }

    private static String routePayload() {
        return """
                {
                  "rule": %s,
                  "detail": {
                    "routeType": "HTTP",
                    "trafficDirection": "INBOUND",
                    "protocol": "HTTP",
                    "destinations": []
                  }
                }
                """.formatted(rulePayload("route.checkout", "Checkout route"));
    }

    private static String breakerPayload() {
        return """
                {
                  "rule": %s,
                  "detail": {
                    "breakerType": "ERROR_RATE",
                    "protocol": "HTTP",
                    "windowType": "SLIDING"
                  }
                }
                """.formatted(rulePayload("breaker.checkout", "Checkout breaker"));
    }

    private static String rateLimitPayload() {
        return """
                {
                  "rule": %s,
                  "detail": {
                    "limitType": "REQUEST",
                    "limitAlgorithm": "TOKEN_BUCKET",
                    "enforcementMode": "BLOCK"
                  }
                }
                """.formatted(rulePayload("rate-limit.checkout", "Checkout rate limit"));
    }

    private static String rulePayload(String ruleCode, String ruleName) {
        return """
                {
                  "instanceSpaceId": "%s",
                  "applicationId": "%s",
                  "ruleCode": "%s",
                  "ruleName": "%s",
                  "sourceFormat": "CUE",
                  "runtimeFormat": "JSON",
                  "cueSource": "package orbit\\\\n",
                  "priority": 100,
                  "enabled": true,
                  "status": "DRAFT",
                  "description": "controller test",
                  "tags": ["checkout"],
                  "createdBy": "xiaoy",
                  "updatedBy": "xiaoy"
                }
                """.formatted(INSTANCE_SPACE_ID, APPLICATION_ID, ruleCode, ruleName);
    }

    private static HttpHeaders controlPlaneHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Stellorbit-Tenant-Id", "tenant-a");
        headers.set("X-Stellorbit-Instance-Space-Id", INSTANCE_SPACE_ID);
        headers.set("X-Stellorbit-Operator", "xiaoy");
        headers.set("X-Stellorbit-Roles", "OPERATOR");
        headers.set("X-Stellorbit-Reason", "controller test");
        return headers;
    }
}
