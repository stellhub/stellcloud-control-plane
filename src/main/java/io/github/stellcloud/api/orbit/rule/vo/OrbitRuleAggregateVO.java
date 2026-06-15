package io.github.stellcloud.api.orbit.rule.vo;

/** 服务治理聚合规则页面视图对象。 */
public record OrbitRuleAggregateVO<T>(RuleSummaryVO rule, T detail) {}
