package io.github.stellcloud.api.orbit.release.vo;

/** 发布恢复请求页面视图对象。 */
public record RecoverRuleReleaseRequestVO(
        String operator,
        String recoveryNote,
        Boolean markFailedRecordsAsPublished) {}
