package io.github.stellcloud.api.orbit.release.vo;

import java.util.List;

/** 发布版本差异页面视图对象。 */
public record RuleReleaseDiffVO(
        String baseReleaseId,
        String targetReleaseId,
        List<RuleReleaseItemDiffVO> ruleDiffs,
        List<StellnulaConfigDiffVO> configDiffs) {}
