package io.github.stellcloud.api.orbit.release.vo;

import java.util.Map;

/** Stellnula 配置差异页面视图对象。 */
public record StellnulaConfigDiffVO(
        String dataId,
        String publishKind,
        String changeType,
        String baseChecksum,
        String targetChecksum,
        Map<String, Object> baseMetadata,
        Map<String, Object> targetMetadata) {}
