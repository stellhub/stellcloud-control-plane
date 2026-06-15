package io.github.stellcloud.api.nula.vo;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/** Feature Flag 编辑页面请求视图对象。 */
public record FeatureFlagRequestVO(
        String id,
        String appId,
        String key,
        String description,
        @NotBlank String environment,
        @NotBlank String cluster,
        String group,
        String type,
        Boolean enabled,
        JsonNode defaultValue,
        JsonNode rules,
        JsonNode variants,
        JsonNode rollout,
        String content,
        String updatedBy,
        String reason) {}
