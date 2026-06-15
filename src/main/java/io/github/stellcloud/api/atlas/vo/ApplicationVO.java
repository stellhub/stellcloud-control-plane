package io.github.stellcloud.api.atlas.vo;

import java.util.Map;

/** CMDB 应用页面视图对象。 */
public record ApplicationVO(
        String ciId,
        String appId,
        String appCode,
        String appName,
        String environment,
        String status,
        String lifecycle,
        String ownerTeamCode,
        String ownerTeamName,
        String language,
        String repositoryUrl,
        int instanceCount,
        int activeInstanceCount,
        Map<String, String> labels,
        ApplicationNamingVO naming,
        String createdAt,
        String updatedAt,
        long cacheVersion) {}
