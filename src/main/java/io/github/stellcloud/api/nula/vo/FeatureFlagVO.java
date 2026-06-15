package io.github.stellcloud.api.nula.vo;

import com.fasterxml.jackson.databind.JsonNode;

/** Feature Flag 页面视图对象。 */
public record FeatureFlagVO(
        String id,
        String appId,
        String key,
        String name,
        String description,
        String environment,
        String cluster,
        String group,
        String type,
        boolean enabled,
        JsonNode defaultValue,
        JsonNode rules,
        JsonNode variants,
        JsonNode rollout,
        String content,
        String version,
        String status,
        String updatedBy,
        String updatedAt,
        String publishedAt) {}
