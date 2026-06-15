package io.github.stellcloud.api.orbit.release;

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

/** Orbit 规则发布控制器测试。 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "stellcloud.test.slice=orbit-release")
class OrbitRuleReleaseControllerTests {

    private static final String RELEASE_ID = "55555555-5555-5555-5555-555555555555";
    private static final String BASE_RELEASE_ID = "66666666-6666-6666-6666-666666666666";
    private static final String RECORD_ID = "77777777-7777-7777-7777-777777777777";
    private static final String INSTANCE_SPACE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String APPLICATION_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String RULE_ID = "11111111-1111-1111-1111-111111111111";

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

    /** 验证发布查询类接口返回页面 VO。 */
    @Test
    void releaseQueryApisReturnViewObjects() {
        var listResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases"
                        + "?instanceSpaceId=" + INSTANCE_SPACE_ID + "&releaseStatus=PUBLISHED&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(controlPlaneHeaders("VIEWER")),
                String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody())
                .contains("\"content\"")
                .contains("\"releaseName\":\"checkout-release\"");

        var detailResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID,
                HttpMethod.GET,
                new HttpEntity<>(controlPlaneHeaders("VIEWER")),
                String.class);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).contains("\"items\"").contains("\"publishRecords\"");

        var diffResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID
                        + "/diff?baseReleaseId=" + BASE_RELEASE_ID,
                HttpMethod.GET,
                new HttpEntity<>(controlPlaneHeaders("VIEWER")),
                String.class);
        assertThat(diffResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(diffResponse.getBody()).contains("\"changeType\":\"MODIFIED\"");

        var impactResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/impact",
                HttpMethod.GET,
                new HttpEntity<>(controlPlaneHeaders("VIEWER")),
                String.class);
        assertThat(impactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(impactResponse.getBody()).contains("\"containsAuthPolicy\":true");

        assertThat(DOWNSTREAM_REQUESTS)
                .anyMatch(request -> request.startsWith("GET /api/stellorbit/rule-releases?"))
                .contains(
                        "GET /api/stellorbit/rule-releases/" + RELEASE_ID,
                        "GET /api/stellorbit/rule-releases/" + RELEASE_ID + "/diff?baseReleaseId=" + BASE_RELEASE_ID,
                        "GET /api/stellorbit/rule-releases/" + RELEASE_ID + "/impact");
    }

    /** 验证发布动作类接口对接 stellorbit-service 并转发安全 header。 */
    @Test
    void releaseMutationApisProxyToOrbitService() {
        HttpHeaders headers = controlPlaneHeaders("PUBLISHER");
        String publishPayload = publishPayload();

        var publishResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases",
                HttpMethod.POST,
                new HttpEntity<>(publishPayload, headers),
                String.class);
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody()).contains("\"releaseStatus\":\"PUBLISHED\"");

        var dryRunResponse = restTemplate.exchange(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases/dry-run",
                HttpMethod.POST,
                new HttpEntity<>(publishPayload, headers),
                String.class);
        assertThat(dryRunResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dryRunResponse.getBody()).contains("\"warnings\"");

        post("/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/retry", retryPayload(), headers);
        post(
                "/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID
                        + "/publish-records/" + RECORD_ID + "/retry",
                retryPayload(),
                headers);
        post("/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/recover", recoverPayload(), headers);
        post("/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/rollback", rollbackPayload(), headers);
        post("/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/approvals/submit", approvalPayload(), headers);
        post("/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/approvals/approve", approvalPayload(), headers);
        post("/api/stellcloud/control-plane/v1/orbit/rule-releases/" + RELEASE_ID + "/approvals/reject", approvalPayload(), headers);

        assertThat(DOWNSTREAM_REQUESTS)
                .contains(
                        "POST /api/stellorbit/rule-releases",
                        "POST /api/stellorbit/rule-releases/dry-run",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/retry",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/publish-records/" + RECORD_ID + "/retry",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/recover",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/rollback",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/approvals/submit",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/approvals/approve",
                        "POST /api/stellorbit/rule-releases/" + RELEASE_ID + "/approvals/reject");
        assertThat(DOWNSTREAM_BODIES).anyMatch(body -> body.contains("\"releaseName\":\"checkout-release\""));
        assertThat(DOWNSTREAM_TENANTS).contains("tenant-a");
    }

    private void post(String path, String payload, HttpHeaders headers) {
        var response = restTemplate.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static void startDownstreamServer() throws IOException {
        if (downstreamServer != null) {
            return;
        }
        downstreamServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstreamServer.createContext("/", OrbitRuleReleaseControllerTests::handleDownstreamRequest);
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

        String responseBody = downstreamResponse(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath());
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        int status = "POST".equals(exchange.getRequestMethod()) && "/api/stellorbit/rule-releases".equals(exchange.getRequestURI().getRawPath())
                ? HttpStatus.CREATED.value()
                : HttpStatus.OK.value();
        exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static String downstreamResponse(String method, String path) {
        if ("GET".equals(method) && "/api/stellorbit/rule-releases".equals(path)) {
            return """
                    {
                      "content": [%s],
                      "page": 0,
                      "size": 10,
                      "totalElements": 1,
                      "totalPages": 1
                    }
                    """.formatted(releaseSummary());
        }
        if (path.endsWith("/diff")) {
            return """
                    {
                      "baseReleaseId": "%s",
                      "targetReleaseId": "%s",
                      "ruleDiffs": [{
                        "ruleId": "%s",
                        "ruleCode": "route.checkout",
                        "ruleName": "Checkout route",
                        "ruleType": "ROUTE",
                        "changeType": "MODIFIED",
                        "baseChecksum": "old",
                        "targetChecksum": "new",
                        "baseSnapshot": {},
                        "targetSnapshot": {}
                      }],
                      "configDiffs": [{
                        "dataId": "route.checkout",
                        "publishKind": "RULE",
                        "changeType": "MODIFIED",
                        "baseChecksum": "old",
                        "targetChecksum": "new",
                        "baseMetadata": {},
                        "targetMetadata": {}
                      }]
                    }
                    """.formatted(BASE_RELEASE_ID, RELEASE_ID, RULE_ID);
        }
        if (path.endsWith("/impact")) {
            return """
                    {
                      "releaseId": "%s",
                      "instanceSpaceId": "%s",
                      "applicationId": "%s",
                      "ruleCount": 1,
                      "ruleTypeCounts": {"ROUTE": 1},
                      "configCount": 1,
                      "publishKindCounts": {"RULE": 1},
                      "configIds": ["route.checkout"],
                      "impactedRuleIds": ["%s"],
                      "containsAuthPolicy": true,
                      "containsMtlsCertificate": false,
                      "containsJwks": false,
                      "containsSensitiveConfig": false
                    }
                    """.formatted(RELEASE_ID, INSTANCE_SPACE_ID, APPLICATION_ID, RULE_ID);
        }
        if (path.endsWith("/dry-run")) {
            return """
                    {
                      "instanceSpaceId": "%s",
                      "applicationId": "%s",
                      "releaseVersion": 12,
                      "runtimeFormat": "JSON",
                      "releaseSnapshotJson": {},
                      "rules": [{
                        "ruleId": "%s",
                        "ruleType": "ROUTE",
                        "ruleCode": "route.checkout",
                        "ruleName": "Checkout route",
                        "schemaVersion": "v1",
                        "configId": "route.checkout",
                        "targetService": "stellnula",
                        "runtimeFormat": "JSON",
                        "checksum": "sha256:test",
                        "normalizedSnapshotJson": {},
                        "jsonContent": "{}",
                        "protobufContentBase64": null,
                        "errors": [],
                        "warnings": [],
                        "explain": ["ok"]
                      }],
                      "errors": [],
                      "warnings": ["dry-run"],
                      "explain": ["ok"]
                    }
                    """.formatted(INSTANCE_SPACE_ID, APPLICATION_ID, RULE_ID);
        }
        if (path.endsWith("/approvals") && "GET".equals(method)) {
            return "[" + approval("SUBMITTED") + "]";
        }
        if (path.contains("/approvals/")) {
            return approval(path.endsWith("/reject") ? "REJECTED" : "APPROVED");
        }
        return releaseDetail();
    }

    private static String releaseSummary() {
        return """
                {
                  "id": "%s",
                  "instanceSpaceId": "%s",
                  "applicationId": "%s",
                  "releaseVersion": 12,
                  "releaseName": "checkout-release",
                  "releaseStatus": "PUBLISHED",
                  "idempotencyKey": "release-12",
                  "checksum": "sha256:test",
                  "rollbackFromReleaseId": null,
                  "retryCount": 0,
                  "maxRetryCount": 3,
                  "itemCount": 1,
                  "publishRecordCount": 1,
                  "failedPublishRecordCount": 0,
                  "failureDetails": [],
                  "createdBy": "xiaoy",
                  "publishedBy": "xiaoy",
                  "createdAt": "2026-06-10T00:00:00Z",
                  "publishedAt": "2026-06-10T00:01:00Z",
                  "updatedAt": "2026-06-10T00:01:00Z"
                }
                """.formatted(RELEASE_ID, INSTANCE_SPACE_ID, APPLICATION_ID);
    }

    private static String releaseDetail() {
        return """
                {
                  "id": "%s",
                  "instanceSpaceId": "%s",
                  "applicationId": "%s",
                  "releaseVersion": 12,
                  "releaseName": "checkout-release",
                  "releaseStatus": "PUBLISHED",
                  "idempotencyKey": "release-12",
                  "sourceFormat": "CUE",
                  "runtimeFormat": "JSON",
                  "checksum": "sha256:test",
                  "rollbackFromReleaseId": null,
                  "releaseNote": "release checkout rules",
                  "retryCount": 0,
                  "maxRetryCount": 3,
                  "failureDetails": [],
                  "recoveryStatus": null,
                  "recoveredBy": null,
                  "recoveredAt": null,
                  "recoveryNote": null,
                  "createdBy": "xiaoy",
                  "publishedBy": "xiaoy",
                  "createdAt": "2026-06-10T00:00:00Z",
                  "publishedAt": "2026-06-10T00:01:00Z",
                  "updatedAt": "2026-06-10T00:01:00Z",
                  "items": [{
                    "id": "88888888-8888-8888-8888-888888888888",
                    "releaseId": "%s",
                    "ruleId": "%s",
                    "ruleType": "ROUTE",
                    "ruleCode": "route.checkout",
                    "ruleName": "Checkout route",
                    "draftVersion": 3,
                    "priority": 100,
                    "checksum": "sha256:test",
                    "createdAt": "2026-06-10T00:00:00Z"
                  }],
                  "publishRecords": [{
                    "id": "%s",
                    "releaseId": "%s",
                    "publishKind": "RULE",
                    "namespaceCode": "orbit",
                    "configGroup": "governance",
                    "configKey": "route.checkout",
                    "dataId": "route.checkout",
                    "contentType": "application/json",
                    "runtimeFormat": "JSON",
                    "payloadMetadata": {},
                    "checksum": "sha256:test",
                    "targetVersion": "v12",
                    "publishStatus": "PUBLISHED",
                    "idempotencyKey": "release-12",
                    "retryCount": 0,
                    "maxRetryCount": 3,
                    "nextRetryAt": null,
                    "lastAttemptAt": "2026-06-10T00:01:00Z",
                    "failureDetails": [],
                    "errorMessage": null,
                    "recoveredBy": null,
                    "recoveredAt": null,
                    "recoveryNote": null,
                    "publishedAt": "2026-06-10T00:01:00Z",
                    "createdAt": "2026-06-10T00:00:00Z"
                  }]
                }
                """.formatted(RELEASE_ID, INSTANCE_SPACE_ID, APPLICATION_ID, RELEASE_ID, RULE_ID, RECORD_ID, RELEASE_ID);
    }

    private static String approval(String status) {
        return """
                {
                  "approvalId": "99999999-9999-9999-9999-999999999999",
                  "releaseId": "%s",
                  "taskId": "00000000-0000-0000-0000-000000000000",
                  "approvalStatus": "%s",
                  "operator": "xiaoy",
                  "reason": "controller test",
                  "detail": {},
                  "createdAt": "2026-06-10T00:00:00Z"
                }
                """.formatted(RELEASE_ID, status);
    }

    private static String publishPayload() {
        return """
                {
                  "instanceSpaceId": "%s",
                  "applicationId": "%s",
                  "releaseVersion": 12,
                  "releaseName": "checkout-release",
                  "runtimeFormat": "JSON",
                  "idempotencyKey": "release-12",
                  "maxRetryCount": 3,
                  "ruleIds": ["%s"],
                  "releaseNote": "release checkout rules",
                  "env": "prod",
                  "configGroup": "governance"
                }
                """.formatted(INSTANCE_SPACE_ID, APPLICATION_ID, RULE_ID);
    }

    private static String retryPayload() {
        return "{\"operator\":\"xiaoy\",\"reason\":\"retry\",\"maxRetryCount\":3}";
    }

    private static String recoverPayload() {
        return "{\"operator\":\"xiaoy\",\"recoveryNote\":\"recover\",\"markFailedRecordsAsPublished\":true}";
    }

    private static String rollbackPayload() {
        return "{\"releaseVersion\":13,\"releaseName\":\"rollback-checkout\",\"idempotencyKey\":\"rollback-13\"}";
    }

    private static String approvalPayload() {
        return "{\"operator\":\"xiaoy\",\"reason\":\"approval\"}";
    }

    private static HttpHeaders controlPlaneHeaders(String roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Stellorbit-Tenant-Id", "tenant-a");
        headers.set("X-Stellorbit-Instance-Space-Id", INSTANCE_SPACE_ID);
        headers.set("X-Stellorbit-Operator", "xiaoy");
        headers.set("X-Stellorbit-Roles", roles);
        headers.set("X-Stellorbit-Reason", "controller test");
        return headers;
    }
}
