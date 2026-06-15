package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;
import java.util.Map;

/** 单条规则 dry-run 编译结果页面视图对象。 */
public record RuleCompileDryRunVO(
        String ruleId,
        String ruleType,
        String ruleCode,
        String ruleName,
        String schemaVersion,
        String configId,
        String targetService,
        String runtimeFormat,
        String checksum,
        Map<String, Object> normalizedSnapshotJson,
        String jsonContent,
        String protobufContentBase64,
        List<String> errors,
        List<String> warnings,
        List<String> explain) {}
