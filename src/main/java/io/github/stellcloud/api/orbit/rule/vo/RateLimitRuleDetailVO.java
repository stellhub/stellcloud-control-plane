package io.github.stellcloud.api.orbit.rule.vo;

import java.util.List;
import java.util.Map;

/** 限流规则详情页面视图对象。 */
public record RateLimitRuleDetailVO(
        String id,
        String limitType,
        String limitAlgorithm,
        String enforcementMode,
        Map<String, Object> targetSelector,
        List<Object> dimensions,
        Map<String, Object> quotaConfig,
        Map<String, Object> windowConfig,
        Map<String, Object> burstConfig,
        Map<String, Object> modelLimitConfig,
        Map<String, Object> fallbackPolicy,
        Map<String, Object> responsePolicy,
        String createdAt,
        String updatedAt) {}
