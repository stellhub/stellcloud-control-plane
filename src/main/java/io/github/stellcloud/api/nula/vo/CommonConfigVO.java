package io.github.stellcloud.api.nula.vo;

/** 公共配置页面视图对象。 */
public record CommonConfigVO(
        String id,
        String ownerId,
        String name,
        String description,
        String environment,
        String cluster,
        String group,
        String format,
        boolean formatLocked,
        String content,
        String version,
        String status,
        String updatedBy,
        String updatedAt,
        String publishedAt) {}
