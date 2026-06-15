package io.github.stellcloud.api.atlas;

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

/** Atlas 应用页面控制器测试。 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AtlasAppControllerTests {

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
                "stellflux.http.client.clients.stellatlas-service.base-url",
                () -> "http://127.0.0.1:" + downstreamServer.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstreamServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    /** 验证应用列表接口对接 Atlas 服务并返回页面 VO。 */
    @Test
    void listApplicationsReturnsViewObject() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/atlas/apps?environment=prod&status=active&search=antifraud",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"records\"")
                .contains("\"appId\":\"stellaxis.payment.risk.antifraud.api\"")
                .contains("\"ownerTeamName\":\"Payment Platform\"")
                .contains("\"activeInstanceCount\":2");
        assertThat(DOWNSTREAM_REQUESTS)
                .anyMatch(request -> request.startsWith("GET /api/stellatlas/v1/apps?")
                        && request.contains("environment=prod")
                        && request.contains("status=active")
                        && request.contains("search=antifraud"));
    }

    /** 验证应用详情接口对接 Atlas 服务并映射标准命名信息。 */
    @Test
    void getApplicationReturnsViewObject() {
        var response = restTemplate.getForEntity(
                "/api/stellcloud/control-plane/v1/atlas/apps/stellaxis.payment.risk.antifraud.api",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"ciId\":\"ci-app-001\"")
                .contains("\"businessDomain\":\"payment\"")
                .contains("\"capabilityDomain\":\"risk\"");
        assertThat(DOWNSTREAM_REQUESTS)
                .contains("GET /api/stellatlas/v1/apps/detail?app_id=stellaxis.payment.risk.antifraud.api");
    }

    /** 验证创建和更新接口使用 Atlas 服务端约定的请求字段。 */
    @Test
    void createAndUpdateApplicationProxyToAtlasService() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload =
                """
                {
                  "appId": "stellaxis.payment.risk.antifraud.api",
                  "appName": "Payment Risk Antifraud API",
                  "environment": "prod",
                  "status": "active",
                  "lifecycle": "managed",
                  "ownerTeamCode": "payment-platform",
                  "ownerTeamName": "Payment Platform",
                  "language": "go",
                  "repositoryUrl": "https://git.example.com/stellaxis/payment/risk/antifraud/api",
                  "labels": {
                    "tier": "core"
                  }
                }
                """;

        var createResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/atlas/apps",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).contains("\"appName\":\"Payment Risk Antifraud API\"");

        var updateResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/atlas/apps/stellaxis.payment.risk.antifraud.api",
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains("\"lifecycle\":\"managed\"");

        assertThat(DOWNSTREAM_REQUESTS)
                .contains("POST /api/stellatlas/v1/apps", "PUT /api/stellatlas/v1/apps");
        assertThat(DOWNSTREAM_BODIES)
                .anyMatch(body -> body.contains("\"app_id\":\"stellaxis.payment.risk.antifraud.api\"")
                        && body.contains("\"repository_url\":\"https://git.example.com/stellaxis/payment/risk/antifraud/api\""));
    }

    /** 验证删除接口对接 Atlas 服务并返回删除结果 VO。 */
    @Test
    void deleteApplicationReturnsViewObject() {
        var response = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/atlas/apps/stellaxis.payment.risk.antifraud.api",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"appId\":\"stellaxis.payment.risk.antifraud.api\"")
                .contains("\"success\":true");
        assertThat(DOWNSTREAM_REQUESTS)
                .contains("DELETE /api/stellatlas/v1/apps?app_id=stellaxis.payment.risk.antifraud.api");
    }

    private static void startDownstreamServer() throws IOException {
        if (downstreamServer != null) {
            return;
        }
        downstreamServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstreamServer.createContext("/", AtlasAppControllerTests::handleDownstreamRequest);
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

        int status = "POST".equals(exchange.getRequestMethod()) ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
        String responseBody = downstreamResponse(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath());
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static String downstreamResponse(String method, String path) {
        if ("GET".equals(method) && "/api/stellatlas/v1/apps".equals(path)) {
            return """
                    {
                      "items": [
                        {
                          "app_id": "stellaxis.payment.risk.antifraud.api",
                          "app_code": "antifraud-api",
                          "app_name": "Payment Risk Antifraud API",
                          "environment": "prod",
                          "status": "active",
                          "lifecycle": "managed",
                          "owner_team_code": "payment-platform",
                          "owner_team_name": "Payment Platform",
                          "language": "go",
                          "repository_url": "https://git.example.com/stellaxis/payment/risk/antifraud/api",
                          "instance_count": 3,
                          "active_instance_count": 2,
                          "updated_at": "2026-06-10T00:00:00Z",
                          "cache_version": 7
                        }
                      ],
                      "count": 1,
                      "limit": 50,
                      "offset": 0
                    }
                    """;
        }
        return """
                {
                  "ci_id": "ci-app-001",
                  "app_id": "stellaxis.payment.risk.antifraud.api",
                  "app_code": "antifraud-api",
                  "app_name": "Payment Risk Antifraud API",
                  "environment": "prod",
                  "status": "active",
                  "lifecycle": "managed",
                  "owner_team_code": "payment-platform",
                  "owner_team_name": "Payment Platform",
                  "language": "go",
                  "repository_url": "https://git.example.com/stellaxis/payment/risk/antifraud/api",
                  "instance_count": 3,
                  "active_instance_count": 2,
                  "labels": {
                    "tier": "core"
                  },
                  "naming": {
                    "organization": "stellaxis",
                    "business_domain": "payment",
                    "capability_domain": "risk",
                    "application": "antifraud",
                    "role": "api"
                  },
                  "created_at": "2026-06-09T00:00:00Z",
                  "updated_at": "2026-06-10T00:00:00Z",
                  "cache_version": 8
                }
                """;
    }
}
