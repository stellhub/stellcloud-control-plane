package io.github.stellcloud.infrastructure.atlas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellcloud.api.atlas.vo.ApplicationDeleteVO;
import io.github.stellcloud.api.atlas.vo.ApplicationListVO;
import io.github.stellcloud.api.atlas.vo.ApplicationNamingVO;
import io.github.stellcloud.api.atlas.vo.ApplicationRequestVO;
import io.github.stellcloud.api.atlas.vo.ApplicationVO;
import io.github.stellflux.http.client.StellfluxHttpClient;
import java.io.IOException;
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

/** Atlas CMDB 控制面页面接口客户端。 */
@Slf4j
@Component
public class AtlasControlPlaneClient {

    private static final String ATLAS_API_PREFIX = "/api/stellatlas/v1";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final StellfluxHttpClient atlasHttpClient;
    private final ObjectMapper objectMapper;

    public AtlasControlPlaneClient(
            @Qualifier("atlasHttpClient") StellfluxHttpClient atlasHttpClient,
            ObjectMapper objectMapper) {
        this.atlasHttpClient = atlasHttpClient;
        this.objectMapper = objectMapper;
    }

    /** 查询 Atlas 应用列表。 */
    public ApplicationListVO listApplications(
            String environment, String status, String search, Integer limit, Integer offset) {
        ApplicationListResponse response = exchange(
                "GET",
                "/apps",
                query(
                        "environment", environment,
                        "status", status,
                        "search", search,
                        "limit", limit,
                        "offset", offset),
                null,
                ApplicationListResponse.class);
        List<ApplicationVO> records = response.items().stream()
                .map(ApplicationPayload::toVO)
                .toList();
        return new ApplicationListVO(records, response.count(), response.limit(), response.offset());
    }

    /** 查询 Atlas 应用详情。 */
    public ApplicationVO getApplication(String appId) {
        return exchange(
                        "GET",
                        "/apps/detail",
                        query("app_id", appId),
                        null,
                        ApplicationPayload.class)
                .toVO();
    }

    /** 创建 Atlas 应用。 */
    public ApplicationVO createApplication(ApplicationRequestVO request) {
        return exchange(
                        "POST",
                        "/apps",
                        Map.of(),
                        ApplicationRequestPayload.from(request, request.appId()),
                        ApplicationPayload.class)
                .toVO();
    }

    /** 更新 Atlas 应用。 */
    public ApplicationVO updateApplication(String appId, ApplicationRequestVO request) {
        return exchange(
                        "PUT",
                        "/apps",
                        Map.of(),
                        ApplicationRequestPayload.from(request, appId),
                        ApplicationPayload.class)
                .toVO();
    }

    /** 删除 Atlas 应用。 */
    public ApplicationDeleteVO deleteApplication(String appId) {
        exchange("DELETE", "/apps", query("app_id", appId), null, Void.class);
        return new ApplicationDeleteVO(appId, true, "应用已删除");
    }

    private <T> T exchange(String method, String path, Map<String, String> query, Object body, Class<T> responseType) {
        Request request = buildRequest(method, path, query, body);
        try (Response response = atlasHttpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.error(
                        "stellatlas-service request failed, method={}, path={}, status={}, response={}",
                        method,
                        path,
                        response.code(),
                        abbreviate(responseBody));
                throw new ResponseStatusException(
                        HttpStatus.valueOf(response.code()),
                        responseBody.isBlank() ? "stellatlas-service request failed" : responseBody);
            }
            if (Void.class.equals(responseType)) {
                return null;
            }
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            log.error("stellatlas-service request error, method={}, path={}", method, path, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "stellatlas-service request failed", ex);
        }
    }

    private Request buildRequest(String method, String path, Map<String, String> query, Object body) {
        HttpUrl.Builder urlBuilder = atlasHttpClient.buildUrl(ATLAS_API_PREFIX + path).newBuilder();
        query.forEach(
                (name, val) -> {
                    if (hasText(val)) {
                        urlBuilder.addQueryParameter(name, val.trim());
                    }
                });
        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
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
            log.error("failed to serialize stellatlas-service request body, method={}", method, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to serialize request body", ex);
        }
    }

    private Map<String, String> query(String firstName, String firstValue) {
        return Map.of(firstName, defaultText(firstValue, ""));
    }

    private Map<String, String> query(
            String firstName,
            String firstValue,
            String secondName,
            String secondValue,
            String thirdName,
            String thirdValue,
            String fourthName,
            Integer fourthValue,
            String fifthName,
            Integer fifthValue) {
        return Map.of(
                firstName,
                defaultText(firstValue, ""),
                secondName,
                defaultText(secondValue, ""),
                thirdName,
                defaultText(thirdValue, ""),
                fourthName,
                fourthValue == null ? "" : fourthValue.toString(),
                fifthName,
                fifthValue == null ? "" : fifthValue.toString());
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

    private record ApplicationListResponse(List<ApplicationPayload> items, int count, int limit, int offset) {}

    private record ApplicationPayload(
            @JsonProperty("ci_id") String ciId,
            @JsonProperty("app_id") String appId,
            @JsonProperty("app_code") String appCode,
            @JsonProperty("app_name") String appName,
            String environment,
            String status,
            String lifecycle,
            @JsonProperty("owner_team_code") String ownerTeamCode,
            @JsonProperty("owner_team_name") String ownerTeamName,
            String language,
            @JsonProperty("repository_url") String repositoryUrl,
            @JsonProperty("instance_count") int instanceCount,
            @JsonProperty("active_instance_count") int activeInstanceCount,
            Map<String, String> labels,
            ApplicationNamingPayload naming,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("cache_version") long cacheVersion) {

        private ApplicationVO toVO() {
            return new ApplicationVO(
                    ciId,
                    appId,
                    appCode,
                    appName,
                    environment,
                    status,
                    lifecycle,
                    ownerTeamCode,
                    ownerTeamName,
                    language,
                    repositoryUrl,
                    instanceCount,
                    activeInstanceCount,
                    labels == null ? Map.of() : labels,
                    naming == null ? null : naming.toVO(),
                    createdAt,
                    updatedAt,
                    cacheVersion);
        }
    }

    private record ApplicationNamingPayload(
            String organization,
            @JsonProperty("business_domain") String businessDomain,
            @JsonProperty("capability_domain") String capabilityDomain,
            String application,
            String role) {

        private ApplicationNamingVO toVO() {
            return new ApplicationNamingVO(organization, businessDomain, capabilityDomain, application, role);
        }
    }

    private record ApplicationRequestPayload(
            @JsonProperty("app_id") String appId,
            @JsonProperty("app_code") String appCode,
            @JsonProperty("app_name") String appName,
            String environment,
            String status,
            String lifecycle,
            @JsonProperty("owner_team_code") String ownerTeamCode,
            @JsonProperty("owner_team_name") String ownerTeamName,
            String language,
            @JsonProperty("repository_url") String repositoryUrl,
            Map<String, String> labels) {

        private static ApplicationRequestPayload from(ApplicationRequestVO request, String appId) {
            return new ApplicationRequestPayload(
                    appId,
                    request.appCode(),
                    request.appName(),
                    request.environment(),
                    request.status(),
                    request.lifecycle(),
                    request.ownerTeamCode(),
                    request.ownerTeamName(),
                    request.language(),
                    request.repositoryUrl(),
                    request.labels());
        }
    }
}
