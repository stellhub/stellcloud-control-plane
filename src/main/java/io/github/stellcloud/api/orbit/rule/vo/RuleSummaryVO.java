package io.github.stellcloud.api.orbit.rule.vo;

import java.util.List;

/** 服务治理规则通用信息视图对象。 */
public record RuleSummaryVO(
        String id,
        String instanceSpaceId,
        String applicationId,
        String ruleCode,
        String ruleName,
        String ruleType,
        String sourceFormat,
        String runtimeFormat,
        String checksum,
        Integer priority,
        Boolean enabled,
        String status,
        Long draftVersion,
        String latestReleaseId,
        String description,
        List<Object> tags,
        String createdBy,
        String updatedBy,
        String createdAt,
        String updatedAt,
        String publishedAt) {}
