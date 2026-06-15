package io.github.stellcloud.api.orbit.release.vo;

import java.util.Map;

/** 发布规则项差异页面视图对象。 */
public record RuleReleaseItemDiffVO(
        String ruleId,
        String ruleCode,
        String ruleName,
        String ruleType,
        String changeType,
        String baseChecksum,
        String targetChecksum,
        Map<String, Object> baseSnapshot,
        Map<String, Object> targetSnapshot) {}
