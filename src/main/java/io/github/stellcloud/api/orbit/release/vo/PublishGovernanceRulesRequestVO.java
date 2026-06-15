package io.github.stellcloud.api.orbit.release.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** 规则发布请求页面视图对象。 */
public record PublishGovernanceRulesRequestVO(
        @NotNull String instanceSpaceId,
        @NotNull String applicationId,
        @NotNull @Positive Long releaseVersion,
        @NotBlank String releaseName,
        String runtimeFormat,
        String idempotencyKey,
        Integer maxRetryCount,
        List<String> ruleIds,
        String releaseNote,
        String env,
        String region,
        String zone,
        String cluster,
        String scopeMode,
        String configGroup,
        String publishedBy) {}
