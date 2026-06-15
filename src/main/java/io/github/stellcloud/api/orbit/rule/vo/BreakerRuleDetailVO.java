package io.github.stellcloud.api.orbit.rule.vo;

import java.math.BigDecimal;
import java.util.Map;

/** 熔断规则详情页面视图对象。 */
public record BreakerRuleDetailVO(
        String id,
        String breakerType,
        String protocol,
        Map<String, Object> targetSelector,
        String windowType,
        Long windowSize,
        Long minimumCalls,
        BigDecimal failureRateThreshold,
        BigDecimal slowCallRateThreshold,
        Long slowCallDurationMillis,
        Long openStateWaitMillis,
        Long permittedHalfOpenCalls,
        Map<String, Object> connectionPoolPolicy,
        Map<String, Object> outlierDetectionPolicy,
        Map<String, Object> retryBudgetPolicy,
        Map<String, Object> exceptionRecordPolicy,
        Map<String, Object> exceptionIgnorePolicy,
        Map<String, Object> fallbackPolicy,
        String createdAt,
        String updatedAt) {}
