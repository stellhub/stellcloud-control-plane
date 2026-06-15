package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;
import java.util.Map;

/** 规则发布 dry-run 页面视图对象。 */
public record RuleReleaseDryRunVO(
        String instanceSpaceId,
        String applicationId,
        Long releaseVersion,
        String runtimeFormat,
        Map<String, Object> releaseSnapshotJson,
        List<RuleCompileDryRunVO> rules,
        List<String> errors,
        List<String> warnings,
        List<String> explain) {}
