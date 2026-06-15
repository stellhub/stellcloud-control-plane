package io.github.stellcloud.api.atlas.vo;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** CMDB 应用编辑页面请求视图对象。 */
public record ApplicationRequestVO(
        String appId,
        String appCode,
        @NotBlank String appName,
        @NotBlank String environment,
        @NotBlank String status,
        @NotBlank String lifecycle,
        String ownerTeamCode,
        String ownerTeamName,
        String language,
        String repositoryUrl,
        Map<String, String> labels) {}
