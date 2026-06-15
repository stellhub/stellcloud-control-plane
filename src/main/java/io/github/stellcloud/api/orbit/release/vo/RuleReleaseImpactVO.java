package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;
import java.util.Map;

/** 发布影响面页面视图对象。 */
public record RuleReleaseImpactVO(
        String releaseId,
        String instanceSpaceId,
        String applicationId,
        Integer ruleCount,
        Map<String, Long> ruleTypeCounts,
        Integer configCount,
        Map<String, Long> publishKindCounts,
        List<String> configIds,
        List<String> impactedRuleIds,
        Boolean containsAuthPolicy,
        Boolean containsMtlsCertificate,
        Boolean containsJwks,
        Boolean containsSensitiveConfig) {}
