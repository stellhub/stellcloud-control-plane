package io.github.stellcloud.api.nula.vo;

/** 应用配置页面视图对象。 */
public record AppConfigVO(
        String id,
        String appId,
        String name,
        String description,
        String environment,
        String cluster,
        String format,
        boolean formatLocked,
        String content,
        String version,
        String status,
        String updatedBy,
        String updatedAt,
        String publishedAt) {}
