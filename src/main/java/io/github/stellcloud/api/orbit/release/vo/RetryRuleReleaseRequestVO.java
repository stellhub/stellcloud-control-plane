package io.github.stellcloud.api.orbit.release.vo;

/** 发布重试请求页面视图对象。 */
public record RetryRuleReleaseRequestVO(String operator, String reason, Integer maxRetryCount) {}
