package io.github.stellcloud.api.orbit.rule.vo;

import java.util.List;
import java.util.Map;

/** 鉴权规则详情页面视图对象。 */
public record AuthRuleDetailVO(
        String id,
        String authPolicyType,
        String authAction,
        String mtlsMode,
        String trustDomain,
        Map<String, Object> workloadSelector,
        List<Object> peerSources,
        List<Object> requestAuthentications,
        List<Object> authorizationFrom,
        List<Object> authorizationTo,
        List<Object> authorizationWhen,
        List<Object> jwtRules,
        String extAuthzProvider,
        Map<String, Object> auditPolicy,
        String createdAt,
        String updatedAt) {}
