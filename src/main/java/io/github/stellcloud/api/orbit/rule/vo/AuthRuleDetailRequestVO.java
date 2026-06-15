package io.github.stellcloud.api.orbit.rule.vo;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** 鉴权规则详情编辑视图对象。 */
public record AuthRuleDetailRequestVO(
        String id,
        @NotBlank String authPolicyType,
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
        Map<String, Object> auditPolicy) {}
