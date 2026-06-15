package io.github.stellcloud.api.orbit.release.vo;

import jakarta.validation.constraints.Positive;

/** 发布回滚请求页面视图对象。 */
public record RollbackRuleReleaseRequestVO(
        @Positive Long releaseVersion,
        String releaseName,
        String idempotencyKey,
        Integer maxRetryCount,
        String reason,
        String configGroup,
        String operator) {}
