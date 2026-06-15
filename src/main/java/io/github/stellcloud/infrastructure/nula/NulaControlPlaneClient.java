package io.github.stellcloud.infrastructure.nula;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellcloud.api.nula.vo.AppConfigListVO;
import io.github.stellcloud.api.nula.vo.AppConfigRequestVO;
import io.github.stellcloud.api.nula.vo.AppConfigScopeVO;
import io.github.stellcloud.api.nula.vo.AppConfigVO;
import io.github.stellcloud.api.nula.vo.CommonConfigListVO;
import io.github.stellcloud.api.nula.vo.CommonConfigRequestVO;
import io.github.stellcloud.api.nula.vo.CommonConfigScopeVO;
import io.github.stellcloud.api.nula.vo.CommonConfigVO;
import io.github.stellcloud.api.nula.vo.FeatureFlagListVO;
import io.github.stellcloud.api.nula.vo.FeatureFlagRequestVO;
import io.github.stellcloud.api.nula.vo.FeatureFlagVO;
import io.github.stellcloud.api.nula.vo.HealthVO;
import io.github.stellcloud.api.nula.vo.MetricVO;
import io.github.stellcloud.api.nula.vo.SharedConfigPageVO;
import io.github.stellcloud.api.nula.vo.TableVO;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Nula 控制面页面接口客户端。 */
@Slf4j
@Component
public class NulaControlPlaneClient {

    private static final String CONTROL_PLANE_PREFIX = "/api/v1/control-plane";
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final String DEFAULT_OPERATOR = "config-control-plane";

    private final StellfluxHttpClient nulaHttpClient;
    private final ObjectMapper objectMapper;

    public NulaControlPlaneClient(
            @Qualifier("nulaHttpClient") StellfluxHttpClient nulaHttpClient,
            ObjectMapper objectMapper) {
        this.nulaHttpClient = nulaHttpClient;
        this.objectMapper = objectMapper;
    }

    /** 查询 Nula 数据面控制面健康状态。 */
    public HealthVO health() {
        return exchange("GET", "/health", Map.of(), Map.of(), null, HealthVO.class);
    }

    /** 查询应用配置范围。 */
    public AppConfigScopeVO scope(String appId, String appIdHeader) {
        return exchange(
                "GET",
                "/app-config/scope",
                query("appId", appId),
                headers(appIdHeader, null),
                null,
                AppConfigScopeVO.class);
    }

    /** 查询应用配置列表。 */
    public AppConfigListVO listConfigs(
            String appId, String environment, String cluster, String group, String appIdHeader) {
        return exchange(
                "GET",
                "/app-config",
                query("appId", appId, "environment", environment, "cluster", cluster, "group", group),
                headers(appIdHeader, null),
                null,
                AppConfigListVO.class);
    }

    /** 查询应用配置详情。 */
    public AppConfigVO getConfig(String configId, String appId, String appIdHeader) {
        return exchange(
                "GET",
                "/app-config/" + configId,
                query("appId", appId),
                headers(appIdHeader, null),
                null,
                AppConfigVO.class);
    }

    /** 创建应用配置草稿。 */
    public AppConfigVO createConfig(AppConfigRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/app-config",
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                AppConfigVO.class);
    }

    /** 保存应用配置草稿。 */
    public AppConfigVO saveDraft(
            String configId, AppConfigRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "PUT",
                "/app-config/" + configId,
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                AppConfigVO.class);
    }

    /** 发布应用配置。 */
    public AppConfigVO publish(
            String configId, AppConfigRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/app-config/" + configId + "/publish",
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                AppConfigVO.class);
    }

    /** 删除应用配置。 */
    public void deleteConfig(String configId, String appId, String environment, String cluster, String appIdHeader) {
        exchange(
                "DELETE",
                "/app-config/" + configId,
                query("appId", appId, "environment", environment, "cluster", cluster),
                headers(appIdHeader, null),
                null,
                Void.class);
    }

    /** 查询公共配置范围。 */
    public CommonConfigScopeVO commonConfigScope(String ownerId, String ownerIdHeader) {
        return exchange(
                "GET",
                "/common-config/scope",
                query("ownerId", ownerId),
                publicHeaders(ownerIdHeader, null),
                null,
                CommonConfigScopeVO.class);
    }

    /** 查询公共配置列表。 */
    public CommonConfigListVO listCommonConfigs(
            String ownerId, String environment, String cluster, String group, String ownerIdHeader) {
        return exchange(
                "GET",
                "/common-config",
                query("ownerId", ownerId, "environment", environment, "cluster", cluster, "group", group),
                publicHeaders(ownerIdHeader, null),
                null,
                CommonConfigListVO.class);
    }

    /** 查询公共配置详情。 */
    public CommonConfigVO getCommonConfig(String configId, String ownerId, String ownerIdHeader) {
        return exchange(
                "GET",
                "/common-config/" + configId,
                query("ownerId", ownerId),
                publicHeaders(ownerIdHeader, null),
                null,
                CommonConfigVO.class);
    }

    /** 创建公共配置草稿。 */
    public CommonConfigVO createCommonConfig(
            CommonConfigRequestVO request, String ownerIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/common-config",
                Map.of(),
                publicHeaders(ownerIdHeader, operatorHeader),
                request,
                CommonConfigVO.class);
    }

    /** 保存公共配置草稿。 */
    public CommonConfigVO saveCommonConfigDraft(
            String configId, CommonConfigRequestVO request, String ownerIdHeader, String operatorHeader) {
        return exchange(
                "PUT",
                "/common-config/" + configId,
                Map.of(),
                publicHeaders(ownerIdHeader, operatorHeader),
                request,
                CommonConfigVO.class);
    }

    /** 发布公共配置。 */
    public CommonConfigVO publishCommonConfig(
            String configId, CommonConfigRequestVO request, String ownerIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/common-config/" + configId + "/publish",
                Map.of(),
                publicHeaders(ownerIdHeader, operatorHeader),
                request,
                CommonConfigVO.class);
    }

    /** 删除公共配置。 */
    public void deleteCommonConfig(
            String configId, String ownerId, String environment, String cluster, String ownerIdHeader) {
        exchange(
                "DELETE",
                "/common-config/" + configId,
                query("ownerId", ownerId, "environment", environment, "cluster", cluster),
                publicHeaders(ownerIdHeader, null),
                null,
                Void.class);
    }

    /** 查询 Feature Flag 列表。 */
    public FeatureFlagListVO listFeatureFlags(
            String appId, String environment, String cluster, String group, String appIdHeader) {
        return exchange(
                "GET",
                "/feature-flags",
                query("appId", appId, "environment", environment, "cluster", cluster, "group", group),
                headers(appIdHeader, null),
                null,
                FeatureFlagListVO.class);
    }

    /** 查询 Feature Flag 详情。 */
    public FeatureFlagVO getFeatureFlag(String flagKey, String appId, String appIdHeader) {
        return exchange(
                "GET",
                "/feature-flags/" + flagKey,
                query("appId", appId),
                headers(appIdHeader, null),
                null,
                FeatureFlagVO.class);
    }

    /** 创建 Feature Flag 草稿。 */
    public FeatureFlagVO createFeatureFlag(
            FeatureFlagRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/feature-flags",
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                FeatureFlagVO.class);
    }

    /** 保存 Feature Flag 草稿。 */
    public FeatureFlagVO saveFeatureFlagDraft(
            String flagKey, FeatureFlagRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "PUT",
                "/feature-flags/" + flagKey,
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                FeatureFlagVO.class);
    }

    /** 发布 Feature Flag。 */
    public FeatureFlagVO publishFeatureFlag(
            String flagKey, FeatureFlagRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/feature-flags/" + flagKey + "/publish",
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                FeatureFlagVO.class);
    }

    /** 删除 Feature Flag。 */
    public void deleteFeatureFlag(
            String flagKey, String appId, String environment, String cluster, String appIdHeader) {
        exchange(
                "DELETE",
                "/feature-flags/" + flagKey,
                query("appId", appId, "environment", environment, "cluster", cluster),
                headers(appIdHeader, null),
                null,
                Void.class);
    }

    /** 查询通用配置页面展示数据。 */
    public SharedConfigPageVO sharedConfigPage() {
        return new SharedConfigPageVO(
                "通用配置",
                "沉淀跨应用共享配置，例如网关域名、公共开关、平台密钥应用和默认策略。",
                List.of(
                        new MetricVO("配置组", "42", "blue"),
                        new MetricVO("应用应用", "129", "green"),
                        new MetricVO("待同步", "7", "orange"),
                        new MetricVO("冲突项", "1", "red")),
                "通用配置组",
                new TableVO(
                        List.of("配置组", "作用域", "应用数", "最后版本", "状态"),
                        List.of(
                                List.of("platform.gateway", "global", "43", "2026.05.28.02", "Published"),
                                List.of("observability.defaults", "prod", "28", "2026.05.27.05", "Published"),
                                List.of("security.keys", "global", "18", "2026.05.26.12", "Review"),
                                List.of("traffic.policy", "prod", "40", "2026.05.25.09", "Published"))),
                List.of("新建配置组", "查看应用", "同步发布", "冲突检测"));
    }

    private <T> T exchange(
            String method,
            String path,
            Map<String, String> query,
            Map<String, String> headers,
            Object body,
            Class<T> responseType) {
        Request request = buildRequest(method, path, query, headers, body);
        try (Response response = nulaHttpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.error(
                        "stellnula-service request failed, method={}, path={}, status={}, response={}",
                        method,
                        path,
                        response.code(),
                        abbreviate(responseBody));
                throw new ResponseStatusException(
                        HttpStatus.valueOf(response.code()),
                        responseBody.isBlank() ? "stellnula-service request failed" : responseBody);
            }
            if (Void.class.equals(responseType)) {
                return null;
            }
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            log.error("stellnula-service request error, method={}, path={}", method, path, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "stellnula-service request failed", ex);
        }
    }

    private Request buildRequest(
            String method, String path, Map<String, String> query, Map<String, String> headers, Object body) {
        HttpUrl.Builder urlBuilder = nulaHttpClient.buildUrl(CONTROL_PLANE_PREFIX + path).newBuilder();
        query.forEach(
                (name, val) -> {
                    if (hasText(val)) {
                        urlBuilder.addQueryParameter(name, val.trim());
                    }
                });
        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
        headers.forEach(
                (name, val) -> {
                    if (hasText(val)) {
                        requestBuilder.addHeader(name, val.trim());
                    }
                });
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
            log.error("failed to serialize stellnula-service request body, method={}", method, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to serialize request body", ex);
        }
    }

    private Map<String, String> headers(String appIdHeader, String operatorHeader) {
        return Map.of(
                "X-Stell-App-Id",
                defaultText(appIdHeader, ""),
                "X-Operator",
                defaultText(operatorHeader, DEFAULT_OPERATOR));
    }

    private Map<String, String> publicHeaders(String ownerIdHeader, String operatorHeader) {
        return Map.of(
                "X-Stell-Public-Owner-Id",
                defaultText(ownerIdHeader, ""),
                "X-Operator",
                defaultText(operatorHeader, DEFAULT_OPERATOR));
    }

    private Map<String, String> query(Object... namesAndValues) {
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("query arguments must be name/value pairs");
        }
        Map<String, String> query = new LinkedHashMap<>();
        for (int index = 0; index < namesAndValues.length; index += 2) {
            Object name = namesAndValues[index];
            Object value = namesAndValues[index + 1];
            query.put(name.toString(), value == null ? "" : value.toString());
        }
        return query;
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000) + "...";
    }
}
