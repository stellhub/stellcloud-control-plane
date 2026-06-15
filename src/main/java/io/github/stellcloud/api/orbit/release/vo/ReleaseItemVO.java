package io.github.stellcloud.api.orbit.release.vo;

/** 发布规则项页面视图对象。 */
public record ReleaseItemVO(
        String id,
        String releaseId,
        String ruleId,
        String ruleType,
        String ruleCode,
        String ruleName,
        Long draftVersion,
        Integer priority,
        String checksum,
        String createdAt) {}
