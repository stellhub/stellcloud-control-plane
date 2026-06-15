package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;
import java.util.Map;

/** Stellnula 发布记录页面视图对象。 */
public record StellnulaPublishRecordVO(
        String id,
        String releaseId,
        String publishKind,
        String namespaceCode,
        String configGroup,
        String configKey,
        String dataId,
        String contentType,
        String runtimeFormat,
        Map<String, Object> payloadMetadata,
        String checksum,
        String targetVersion,
        String publishStatus,
        String idempotencyKey,
        Integer retryCount,
        Integer maxRetryCount,
        String nextRetryAt,
        String lastAttemptAt,
        List<Object> failureDetails,
        String errorMessage,
        String recoveredBy,
        String recoveredAt,
        String recoveryNote,
        String publishedAt,
        String createdAt) {}
