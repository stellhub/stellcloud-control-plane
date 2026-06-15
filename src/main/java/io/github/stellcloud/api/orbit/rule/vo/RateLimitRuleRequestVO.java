package io.github.stellcloud.api.orbit.rule.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 限流规则编辑页面请求视图对象。 */
public record RateLimitRuleRequestVO(
        @Valid @NotNull RuleMutationVO rule,
        @Valid @NotNull RateLimitRuleDetailRequestVO detail) {}
