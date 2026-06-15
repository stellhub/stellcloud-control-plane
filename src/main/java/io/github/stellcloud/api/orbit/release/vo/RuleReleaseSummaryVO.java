package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;

/** 规则发布摘要页面视图对象。 */
public record RuleReleaseSummaryVO(
        String id,
        String instanceSpaceId,
        String applicationId,
        Long releaseVersion,
        String releaseName,
        String releaseStatus,
        String idempotencyKey,
        String checksum,
        String rollbackFromReleaseId,
        Integer retryCount,
        Integer maxRetryCount,
        Integer itemCount,
        Integer publishRecordCount,
        Integer failedPublishRecordCount,
        List<Object> failureDetails,
        String createdBy,
        String publishedBy,
        String createdAt,
        String publishedAt,
        String updatedAt) {}
