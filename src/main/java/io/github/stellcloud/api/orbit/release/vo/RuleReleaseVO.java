package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;

/** 规则发布详情页面视图对象。 */
public record RuleReleaseVO(
        String id,
        String instanceSpaceId,
        String applicationId,
        Long releaseVersion,
        String releaseName,
        String releaseStatus,
        String idempotencyKey,
        String sourceFormat,
        String runtimeFormat,
        String checksum,
        String rollbackFromReleaseId,
        String releaseNote,
        Integer retryCount,
        Integer maxRetryCount,
        List<Object> failureDetails,
        String recoveryStatus,
        String recoveredBy,
        String recoveredAt,
        String recoveryNote,
        String createdBy,
        String publishedBy,
        String createdAt,
        String publishedAt,
        String updatedAt,
        List<ReleaseItemVO> items,
        List<StellnulaPublishRecordVO> publishRecords) {}
