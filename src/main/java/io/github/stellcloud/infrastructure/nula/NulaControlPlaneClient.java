package io.github.stellcloud.infrastructure.nula;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellcloud.api.nula.vo.AppConfigListVO;
import io.github.stellcloud.api.nula.vo.AppConfigRequestVO;
import io.github.stellcloud.api.nula.vo.AppConfigScopeVO;
import io.github.stellcloud.api.nula.vo.AppConfigVO;
import io.github.stellcloud.api.nula.vo.HealthVO;
import io.github.stellcloud.api.nula.vo.MetricVO;
import io.github.stellcloud.api.nula.vo.SharedConfigPageVO;
import io.github.stellcloud.api.nula.vo.TableVO;
import io.github.stellflux.http.client.StellfluxHttpClient;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
                "/configs/scope",
                query("appId", appId),
                headers(appIdHeader, null),
                null,
                AppConfigScopeVO.class);
    }

    /** 查询应用配置列表。 */
    public AppConfigListVO listConfigs(String appId, String environment, String cluster, String appIdHeader) {
        return exchange(
                "GET",
                "/configs",
                query("appId", appId, "environment", environment, "cluster", cluster),
                headers(appIdHeader, null),
                null,
                AppConfigListVO.class);
    }

    /** 查询应用配置详情。 */
    public AppConfigVO getConfig(String configId, String appId, String appIdHeader) {
        return exchange(
                "GET",
                "/configs/" + configId,
                query("appId", appId),
                headers(appIdHeader, null),
                null,
                AppConfigVO.class);
    }

    /** 创建应用配置草稿。 */
    public AppConfigVO createConfig(AppConfigRequestVO request, String appIdHeader, String operatorHeader) {
        return exchange(
                "POST",
                "/configs",
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
                "/configs/" + configId,
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
                "/configs/" + configId + "/publish",
                Map.of(),
                headers(appIdHeader, operatorHeader),
                request,
                AppConfigVO.class);
    }

    /** 删除应用配置。 */
    public void deleteConfig(String configId, String appId, String environment, String cluster, String appIdHeader) {
        exchange(
                "DELETE",
                "/configs/" + configId,
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
                throw new ResponseStatusException(
                        HttpStatus.valueOf(response.code()),
                        responseBody.isBlank() ? "stellnula-service request failed" : responseBody);
            }
            if (Void.class.equals(responseType)) {
                return null;
            }
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
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

    private Map<String, String> query(String firstName, String firstValue) {
        return Map.of(firstName, defaultText(firstValue, ""));
    }

    private Map<String, String> query(String firstName, String firstValue, String secondName, String secondValue, String thirdName, String thirdValue) {
        return Map.of(
                firstName,
                defaultText(firstValue, ""),
                secondName,
                defaultText(secondValue, ""),
                thirdName,
                defaultText(thirdValue, ""));
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
