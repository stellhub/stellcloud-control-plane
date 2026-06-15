package io.github.stellcloud.api.orbit.rule.vo;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;

/** 熔断规则详情编辑视图对象。 */
public record BreakerRuleDetailRequestVO(
        String id,
        @NotBlank String breakerType,
        @NotBlank String protocol,
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
        Map<String, Object> fallbackPolicy) {}
