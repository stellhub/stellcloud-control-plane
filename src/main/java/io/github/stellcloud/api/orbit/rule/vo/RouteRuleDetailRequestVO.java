package io.github.stellcloud.api.orbit.rule.vo;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** 路由规则详情编辑视图对象。 */
public record RouteRuleDetailRequestVO(
        String id,
        @NotBlank String routeType,
        String trafficDirection,
        @NotBlank String protocol,
        List<Object> gateways,
        List<Object> hosts,
        Map<String, Object> sourceSelector,
        List<Object> matchConditions,
        List<Object> destinations,
        Map<String, Object> routeAction,
        Map<String, Object> rewritePolicy,
        Map<String, Object> redirectPolicy,
        Map<String, Object> mirrorPolicy,
        Map<String, Object> faultInjectionPolicy,
        Map<String, Object> timeoutPolicy,
        Map<String, Object> retryPolicy,
        Map<String, Object> loadBalancePolicy,
        Map<String, Object> localityPolicy) {}
