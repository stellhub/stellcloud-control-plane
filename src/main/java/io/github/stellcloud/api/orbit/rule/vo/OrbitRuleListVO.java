package io.github.stellcloud.api.orbit.rule.vo;

import java.util.List;

/** 服务治理规则列表页面视图对象。 */
public record OrbitRuleListVO<T>(List<OrbitRuleAggregateVO<T>> records) {}
