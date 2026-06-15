package io.github.stellcloud.infrastructure.orbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellcloud.api.orbit.rule.vo.AuthRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.AuthRuleRequestVO;
import io.github.stellcloud.api.orbit.rule.vo.BreakerRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.BreakerRuleRequestVO;
import io.github.stellcloud.api.orbit.rule.vo.OrbitRuleAggregateVO;
import io.github.stellcloud.api.orbit.rule.vo.OrbitRuleListVO;
import io.github.stellcloud.api.orbit.rule.vo.RateLimitRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.RateLimitRuleRequestVO;
import io.github.stellcloud.api.orbit.rule.vo.RouteRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.RouteRuleRequestVO;
import io.github.stellflux.http.client.StellfluxHttpClient;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Orbit 服务治理规则控制面客户端。 */
@Slf4j
@Component
public class OrbitGovernanceRuleClient {

    private static final String ORBIT_API_PREFIX = "/api/stellorbit";
    private static final MediaType JSON = MediaType.parse("application/json");

    private static final TypeReference<List<OrbitRuleAggregateVO<RouteRuleDetailVO>>> ROUTE_RULE_LIST_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<OrbitRuleAggregateVO<RouteRuleDetailVO>> ROUTE_RULE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<List<OrbitRuleAggregateVO<BreakerRuleDetailVO>>> BREAKER_RULE_LIST_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<OrbitRuleAggregateVO<BreakerRuleDetailVO>> BREAKER_RULE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<List<OrbitRuleAggregateVO<RateLimitRuleDetailVO>>> RATE_LIMIT_RULE_LIST_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<OrbitRuleAggregateVO<RateLimitRuleDetailVO>> RATE_LIMIT_RULE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<List<OrbitRuleAggregateVO<AuthRuleDetailVO>>> AUTH_RULE_LIST_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<OrbitRuleAggregateVO<AuthRuleDetailVO>> AUTH_RULE_TYPE =
            new TypeReference<>() {};

    private final StellfluxHttpClient orbitHttpClient;
    private final ObjectMapper objectMapper;

    public OrbitGovernanceRuleClient(
            @Qualifier("orbitHttpClient") StellfluxHttpClient orbitHttpClient,
            ObjectMapper objectMapper) {
        this.orbitHttpClient = orbitHttpClient;
        this.objectMapper = objectMapper;
    }

    /** 查询路由规则列表。 */
    public OrbitRuleListVO<RouteRuleDetailVO> listRouteRules(HttpHeaders headers) {
        return new OrbitRuleListVO<>(exchange("GET", "/rules/routes", null, headers, ROUTE_RULE_LIST_TYPE));
    }

    /** 查询路由规则详情。 */
    public OrbitRuleAggregateVO<RouteRuleDetailVO> getRouteRule(String id, HttpHeaders headers) {
        return exchange("GET", "/rules/routes/" + id, null, headers, ROUTE_RULE_TYPE);
    }

    /** 创建路由规则。 */
    public OrbitRuleAggregateVO<RouteRuleDetailVO> createRouteRule(RouteRuleRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/rules/routes", request, headers, ROUTE_RULE_TYPE);
    }

    /** 更新路由规则。 */
    public OrbitRuleAggregateVO<RouteRuleDetailVO> updateRouteRule(
            String id, RouteRuleRequestVO request, HttpHeaders headers) {
        return exchange("PATCH", "/rules/routes/" + id, request, headers, ROUTE_RULE_TYPE);
    }

    /** 删除路由规则。 */
    public void deleteRouteRule(String id, HttpHeaders headers) {
        exchangeVoid("DELETE", "/rules/routes/" + id, headers);
    }

    /** 查询熔断规则列表。 */
    public OrbitRuleListVO<BreakerRuleDetailVO> listBreakerRules(HttpHeaders headers) {
        return new OrbitRuleListVO<>(exchange("GET", "/rules/breakers", null, headers, BREAKER_RULE_LIST_TYPE));
    }

    /** 查询熔断规则详情。 */
    public OrbitRuleAggregateVO<BreakerRuleDetailVO> getBreakerRule(String id, HttpHeaders headers) {
        return exchange("GET", "/rules/breakers/" + id, null, headers, BREAKER_RULE_TYPE);
    }

    /** 创建熔断规则。 */
    public OrbitRuleAggregateVO<BreakerRuleDetailVO> createBreakerRule(
            BreakerRuleRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/rules/breakers", request, headers, BREAKER_RULE_TYPE);
    }

    /** 更新熔断规则。 */
    public OrbitRuleAggregateVO<BreakerRuleDetailVO> updateBreakerRule(
            String id, BreakerRuleRequestVO request, HttpHeaders headers) {
        return exchange("PATCH", "/rules/breakers/" + id, request, headers, BREAKER_RULE_TYPE);
    }

    /** 删除熔断规则。 */
    public void deleteBreakerRule(String id, HttpHeaders headers) {
        exchangeVoid("DELETE", "/rules/breakers/" + id, headers);
    }

    /** 查询限流规则列表。 */
    public OrbitRuleListVO<RateLimitRuleDetailVO> listRateLimitRules(HttpHeaders headers) {
        return new OrbitRuleListVO<>(exchange("GET", "/rules/rate-limits", null, headers, RATE_LIMIT_RULE_LIST_TYPE));
    }

    /** 查询限流规则详情。 */
    public OrbitRuleAggregateVO<RateLimitRuleDetailVO> getRateLimitRule(String id, HttpHeaders headers) {
        return exchange("GET", "/rules/rate-limits/" + id, null, headers, RATE_LIMIT_RULE_TYPE);
    }

    /** 创建限流规则。 */
    public OrbitRuleAggregateVO<RateLimitRuleDetailVO> createRateLimitRule(
            RateLimitRuleRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/rules/rate-limits", request, headers, RATE_LIMIT_RULE_TYPE);
    }

    /** 更新限流规则。 */
    public OrbitRuleAggregateVO<RateLimitRuleDetailVO> updateRateLimitRule(
            String id, RateLimitRuleRequestVO request, HttpHeaders headers) {
        return exchange("PATCH", "/rules/rate-limits/" + id, request, headers, RATE_LIMIT_RULE_TYPE);
    }

    /** 删除限流规则。 */
    public void deleteRateLimitRule(String id, HttpHeaders headers) {
        exchangeVoid("DELETE", "/rules/rate-limits/" + id, headers);
    }

    /** 查询鉴权规则列表。 */
    public OrbitRuleListVO<AuthRuleDetailVO> listAuthRules(HttpHeaders headers) {
        return new OrbitRuleListVO<>(exchange("GET", "/rules/auth", null, headers, AUTH_RULE_LIST_TYPE));
    }

    /** 查询鉴权规则详情。 */
    public OrbitRuleAggregateVO<AuthRuleDetailVO> getAuthRule(String id, HttpHeaders headers) {
        return exchange("GET", "/rules/auth/" + id, null, headers, AUTH_RULE_TYPE);
    }

    /** 创建鉴权规则。 */
    public OrbitRuleAggregateVO<AuthRuleDetailVO> createAuthRule(AuthRuleRequestVO request, HttpHeaders headers) {
        return exchange("POST", "/rules/auth", request, headers, AUTH_RULE_TYPE);
    }

    /** 更新鉴权规则。 */
    public OrbitRuleAggregateVO<AuthRuleDetailVO> updateAuthRule(
            String id, AuthRuleRequestVO request, HttpHeaders headers) {
        return exchange("PATCH", "/rules/auth/" + id, request, headers, AUTH_RULE_TYPE);
    }

    /** 删除鉴权规则。 */
    public void deleteAuthRule(String id, HttpHeaders headers) {
        exchangeVoid("DELETE", "/rules/auth/" + id, headers);
    }

    private <T> T exchange(String method, String path, Object body, HttpHeaders headers, TypeReference<T> responseType) {
        Request request = buildRequest(method, path, body, headers);
        try (Response response = orbitHttpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.error(
                        "stellorbit-service rule request failed, method={}, path={}, status={}, response={}",
                        method,
                        path,
                        response.code(),
                        abbreviate(responseBody));
                throw new ResponseStatusException(
                        HttpStatus.valueOf(response.code()),
                        responseBody.isBlank() ? "stellorbit-service request failed" : responseBody);
            }
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            log.error("stellorbit-service rule request error, method={}, path={}", method, path, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "stellorbit-service request failed", ex);
        }
    }

    private void exchangeVoid(String method, String path, HttpHeaders headers) {
        Request request = buildRequest(method, path, null, headers);
        try (Response response = orbitHttpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.error(
                        "stellorbit-service rule request failed, method={}, path={}, status={}, response={}",
                        method,
                        path,
                        response.code(),
                        abbreviate(responseBody));
                throw new ResponseStatusException(
                        HttpStatus.valueOf(response.code()),
                        responseBody.isBlank() ? "stellorbit-service request failed" : responseBody);
            }
        } catch (IOException ex) {
            log.error("stellorbit-service rule request error, method={}, path={}", method, path, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "stellorbit-service request failed", ex);
        }
    }

    private Request buildRequest(String method, String path, Object body, HttpHeaders headers) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(orbitHttpClient.buildUrl(ORBIT_API_PREFIX + path));
        forwardControlPlaneHeaders(headers, requestBuilder);
        requestBuilder.method(method, requestBody(method, body));
        return requestBuilder.build();
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

    private RequestBody requestBody(String method, Object body) {
        if ("GET".equals(method) || "HEAD".equals(method) || body == null) {
            return null;
        }
        try {
            return RequestBody.create(objectMapper.writeValueAsBytes(body), JSON);
        } catch (JsonProcessingException ex) {
            log.error("failed to serialize stellorbit-service rule request body, method={}", method, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to serialize request body", ex);
        }
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000) + "...";
    }
}
