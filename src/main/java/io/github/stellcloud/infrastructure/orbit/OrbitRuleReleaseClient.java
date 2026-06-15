package io.github.stellcloud.infrastructure.orbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellcloud.api.orbit.release.vo.ApprovalActionRequestVO;
import io.github.stellcloud.api.orbit.release.vo.ApprovalVO;
import io.github.stellcloud.api.orbit.release.vo.PageVO;
import io.github.stellcloud.api.orbit.release.vo.PublishGovernanceRulesRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RecoverRuleReleaseRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RetryRuleReleaseRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RollbackRuleReleaseRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseDiffVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseDryRunVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseImpactVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseSummaryVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseVO;
import io.github.stellflux.http.client.StellfluxHttpClient;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Orbit 规则发布控制面客户端。 */
@Slf4j
@Component
public class OrbitRuleReleaseClient {

    private static final String RELEASE_API_PREFIX = "/api/stellorbit/rule-releases";
    private static final MediaType JSON = MediaType.parse("application/json");

    private static final TypeReference<PageVO<RuleReleaseSummaryVO>> RELEASE_PAGE_TYPE = new TypeReference<>() {};
    private static final TypeReference<RuleReleaseVO> RELEASE_TYPE = new TypeReference<>() {};
    private static final TypeReference<RuleReleaseDiffVO> RELEASE_DIFF_TYPE = new TypeReference<>() {};
    private static final TypeReference<RuleReleaseImpactVO> RELEASE_IMPACT_TYPE = new TypeReference<>() {};
    private static final TypeReference<RuleReleaseDryRunVO> RELEASE_DRY_RUN_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<ApprovalVO>> APPROVAL_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<ApprovalVO> APPROVAL_TYPE = new TypeReference<>() {};

    private final StellfluxHttpClient orbitHttpClient;
    private final ObjectMapper objectMapper;

    public OrbitRuleReleaseClient(
            @Qualifier("orbitHttpClient") StellfluxHttpClient orbitHttpClient,
            ObjectMapper objectMapper) {
        this.orbitHttpClient = orbitHttpClient;
        this.objectMapper = objectMapper;
    }

    /** 分页查询发布列表。 */
    public PageVO<RuleReleaseSummaryVO> search(
            String instanceSpaceId,
            String applicationId,
            String releaseStatus,
            String keyword,
            Integer page,
            Integer size,
            HttpHeaders headers) {
        return exchange(
                "GET",
                "",
                query(
                        "instanceSpaceId", instanceSpaceId,
                        "applicationId", applicationId,
                        "releaseStatus", releaseStatus,
                        "keyword", keyword,
                        "page", page,
                        "size", size),
                null,
                headers,
                RELEASE_PAGE_TYPE);
    }

    /** 查询发布详情。 */
    public RuleReleaseVO detail(String id, HttpHeaders headers) {
        return exchange("GET", "/" + id, Map.of(), null, headers, RELEASE_TYPE);
    }

    /** 对比发布版本。 */
    public RuleReleaseDiffVO diff(String id, String baseReleaseId, HttpHeaders headers) {
        return exchange("GET", "/" + id + "/diff", query("baseReleaseId", baseReleaseId), null, headers, RELEASE_DIFF_TYPE);
    }

    /** 分析发布影响面。 */
    public RuleReleaseImpactVO impact(String id, HttpHeaders headers) {
        return exchange("GET", "/" + id + "/impact", Map.of(), null, headers, RELEASE_IMPACT_TYPE);
    }

    /** 发布服务治理规则。 */
    public RuleReleaseVO publish(PublishGovernanceRulesRequestVO request, HttpHeaders headers) {
        return exchange("POST", "", Map.of(), request, headers, RELEASE_TYPE);
    }

    /** 发布前 dry-run。 */
    public RuleReleaseDryRunVO dryRun(PublishGovernanceRulesRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/dry-run", Map.of(), request, headers, RELEASE_DRY_RUN_TYPE);
    }

    /** 重试发布。 */
    public RuleReleaseVO retry(String id, RetryRuleReleaseRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/" + id + "/retry", Map.of(), request, headers, RELEASE_TYPE);
    }

    /** 重试单条发布记录。 */
    public RuleReleaseVO retryPublishRecord(
            String id, String recordId, RetryRuleReleaseRequestVO request, HttpHeaders headers) {
        return exchange(
                "POST",
                "/" + id + "/publish-records/" + recordId + "/retry",
                Map.of(),
                request,
                headers,
                RELEASE_TYPE);
    }

    /** 人工恢复发布状态。 */
    public RuleReleaseVO recover(String id, RecoverRuleReleaseRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/" + id + "/recover", Map.of(), request, headers, RELEASE_TYPE);
    }

    /** 回滚发布。 */
    public RuleReleaseVO rollback(String id, RollbackRuleReleaseRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/" + id + "/rollback", Map.of(), request, headers, RELEASE_TYPE);
    }

    /** 查询发布审批时间线。 */
    public List<ApprovalVO> approvals(String id, HttpHeaders headers) {
        return exchange("GET", "/" + id + "/approvals", Map.of(), null, headers, APPROVAL_LIST_TYPE);
    }

    /** 提交发布审批。 */
    public ApprovalVO submitApproval(String id, ApprovalActionRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/" + id + "/approvals/submit", Map.of(), request, headers, APPROVAL_TYPE);
    }

    /** 通过发布审批。 */
    public ApprovalVO approve(String id, ApprovalActionRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/" + id + "/approvals/approve", Map.of(), request, headers, APPROVAL_TYPE);
    }

    /** 驳回发布审批。 */
    public ApprovalVO reject(String id, ApprovalActionRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/" + id + "/approvals/reject", Map.of(), request, headers, APPROVAL_TYPE);
    }

    private <T> T exchange(
            String method,
            String path,
            Map<String, String> query,
            Object body,
            HttpHeaders headers,
            TypeReference<T> responseType) {
        Request request = buildRequest(method, path, query, body, headers);
        try (Response response = orbitHttpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.error(
                        "stellorbit-service release request failed, method={}, path={}, status={}, response={}",
                        method,
                        path,
                        response.code(),
                        abbreviate(responseBody));
                throw new ResponseStatusException(
                        HttpStatus.valueOf(response.code()),
                        responseBody.isBlank() ? "stellorbit-service release request failed" : responseBody);
            }
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            log.error("stellorbit-service release request error, method={}, path={}", method, path, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "stellorbit-service release request failed", ex);
        }
    }

    private Request buildRequest(
            String method, String path, Map<String, String> query, Object body, HttpHeaders headers) {
        HttpUrl.Builder urlBuilder = orbitHttpClient.buildUrl(RELEASE_API_PREFIX + path).newBuilder();
        query.forEach((name, value) -> {
            if (value != null && !value.isBlank()) {
                urlBuilder.addQueryParameter(name, value.trim());
            }
        });
        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
        forwardControlPlaneHeaders(headers, requestBuilder);
        requestBuilder.method(method, requestBody(method, body));
        return requestBuilder.build();
    }

    private RequestBody requestBody(String method, Object body) {
        if ("GET".equals(method) || "HEAD".equals(method) || body == null) {
            return null;
        }
        try {
            return RequestBody.create(objectMapper.writeValueAsBytes(body), JSON);
        } catch (JsonProcessingException ex) {
            log.error("failed to serialize stellorbit-service release request body, method={}", method, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to serialize request body", ex);
        }
    }

    private void forwardControlPlaneHeaders(HttpHeaders headers, Request.Builder requestBuilder) {
        if (headers == null) {
            return;
        }
        headers.forEach((name, values) -> {
            if (isForwardedHeader(name) && values != null) {
                values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .forEach(value -> requestBuilder.addHeader(name, value));
            }
        });
    }

    private boolean isForwardedHeader(String name) {
        return name != null
                && (name.regionMatches(true, 0, "X-Stellorbit-", 0, "X-Stellorbit-".length())
                        || "X-Request-Id".equalsIgnoreCase(name)
                        || "User-Agent".equalsIgnoreCase(name));
    }

    private Map<String, String> query(Object... namesAndValues) {
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("query arguments must be name/value pairs");
        }
        Map<String, String> query = new LinkedHashMap<>();
        for (int index = 0; index < namesAndValues.length; index += 2) {
            query.put(namesAndValues[index].toString(), namesAndValues[index + 1] == null ? "" : namesAndValues[index + 1].toString());
        }
        return query;
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000) + "...";
    }
}
