package io.github.stellcloud.api.orbit.release.vo;

import java.util.Map;

/** 发布审批页面视图对象。 */
public record ApprovalVO(
        String approvalId,
        String releaseId,
        String taskId,
        String approvalStatus,
        String operator,
        String reason,
        Map<String, Object> detail,
        String createdAt) {}
