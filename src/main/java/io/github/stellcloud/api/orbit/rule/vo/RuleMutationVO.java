package io.github.stellcloud.api.orbit.rule.vo;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** 服务治理规则通用编辑视图对象。 */
public record RuleMutationVO(
        String id,
        @NotBlank String instanceSpaceId,
        @NotBlank String applicationId,
        @NotBlank String ruleCode,
        @NotBlank String ruleName,
        String sourceFormat,
        String runtimeFormat,
        @NotBlank String cueSource,
        Map<String, Object> runtimeSnapshotJson,
        String checksum,
        Integer priority,
        Boolean enabled,
        String status,
        Long draftVersion,
        String description,
        List<Object> tags,
        @NotBlank String createdBy,
        @NotBlank String updatedBy) {}
