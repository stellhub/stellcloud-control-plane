package io.github.stellcloud.api.orbit.rule;

import io.github.stellcloud.api.orbit.rule.vo.AuthRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.AuthRuleRequestVO;
import io.github.stellcloud.api.orbit.rule.vo.BreakerRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.BreakerRuleRequestVO;
import io.github.stellcloud.api.orbit.rule.vo.OrbitRuleAggregateVO;
import io.github.stellcloud.api.orbit.rule.vo.OrbitRuleListVO;
import io.github.stellcloud.api.orbit.rule.vo.RateLimitRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.RateLimitRuleRequestVO;
import io.github.stellcloud.api.orbit.rule.vo.RouteRuleDetailVO;
import io.github.stellcloud.api.orbit.rule.vo.RouteRuleRequestVO;
import io.github.stellcloud.infrastructure.orbit.OrbitGovernanceRuleClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 服务治理规则页面接口。 */
@Validated
@RestController
@RequestMapping("/api/stellcloud/control-plane/v1/orbit/rules")
public class OrbitRuleController {

    private final OrbitGovernanceRuleClient orbitGovernanceRuleClient;

    public OrbitRuleController(OrbitGovernanceRuleClient orbitGovernanceRuleClient) {
        this.orbitGovernanceRuleClient = orbitGovernanceRuleClient;
    }

    /** 查询路由规则列表。 */
    @GetMapping("/routes")
    public OrbitRuleListVO<RouteRuleDetailVO> listRouteRules(@RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.listRouteRules(headers);
    }

    /** 查询路由规则详情。 */
    @GetMapping("/routes/{id}")
    public OrbitRuleAggregateVO<RouteRuleDetailVO> getRouteRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.getRouteRule(id, headers);
    }

    /** 创建路由规则。 */
    @PostMapping("/routes")
    public OrbitRuleAggregateVO<RouteRuleDetailVO> createRouteRule(
            @Valid @RequestBody RouteRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.createRouteRule(request, headers);
    }

    /** 更新路由规则。 */
    @PatchMapping("/routes/{id}")
    public OrbitRuleAggregateVO<RouteRuleDetailVO> updateRouteRule(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody RouteRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.updateRouteRule(id, request, headers);
    }

    /** 删除路由规则。 */
    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> deleteRouteRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        orbitGovernanceRuleClient.deleteRouteRule(id, headers);
        return ResponseEntity.noContent().build();
    }

    /** 查询熔断规则列表。 */
    @GetMapping("/breakers")
    public OrbitRuleListVO<BreakerRuleDetailVO> listBreakerRules(@RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.listBreakerRules(headers);
    }

    /** 查询熔断规则详情。 */
    @GetMapping("/breakers/{id}")
    public OrbitRuleAggregateVO<BreakerRuleDetailVO> getBreakerRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.getBreakerRule(id, headers);
    }

    /** 创建熔断规则。 */
    @PostMapping("/breakers")
    public OrbitRuleAggregateVO<BreakerRuleDetailVO> createBreakerRule(
            @Valid @RequestBody BreakerRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.createBreakerRule(request, headers);
    }

    /** 更新熔断规则。 */
    @PatchMapping("/breakers/{id}")
    public OrbitRuleAggregateVO<BreakerRuleDetailVO> updateBreakerRule(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody BreakerRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.updateBreakerRule(id, request, headers);
    }

    /** 删除熔断规则。 */
    @DeleteMapping("/breakers/{id}")
    public ResponseEntity<Void> deleteBreakerRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        orbitGovernanceRuleClient.deleteBreakerRule(id, headers);
        return ResponseEntity.noContent().build();
    }

    /** 查询限流规则列表。 */
    @GetMapping("/rate-limits")
    public OrbitRuleListVO<RateLimitRuleDetailVO> listRateLimitRules(@RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.listRateLimitRules(headers);
    }

    /** 查询限流规则详情。 */
    @GetMapping("/rate-limits/{id}")
    public OrbitRuleAggregateVO<RateLimitRuleDetailVO> getRateLimitRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.getRateLimitRule(id, headers);
    }

    /** 创建限流规则。 */
    @PostMapping("/rate-limits")
    public OrbitRuleAggregateVO<RateLimitRuleDetailVO> createRateLimitRule(
            @Valid @RequestBody RateLimitRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.createRateLimitRule(request, headers);
    }

    /** 更新限流规则。 */
    @PatchMapping("/rate-limits/{id}")
    public OrbitRuleAggregateVO<RateLimitRuleDetailVO> updateRateLimitRule(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody RateLimitRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.updateRateLimitRule(id, request, headers);
    }

    /** 删除限流规则。 */
    @DeleteMapping("/rate-limits/{id}")
    public ResponseEntity<Void> deleteRateLimitRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        orbitGovernanceRuleClient.deleteRateLimitRule(id, headers);
        return ResponseEntity.noContent().build();
    }

    /** 查询鉴权规则列表。 */
    @GetMapping("/auth")
    public OrbitRuleListVO<AuthRuleDetailVO> listAuthRules(@RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.listAuthRules(headers);
    }

    /** 查询鉴权规则详情。 */
    @GetMapping("/auth/{id}")
    public OrbitRuleAggregateVO<AuthRuleDetailVO> getAuthRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.getAuthRule(id, headers);
    }

    /** 创建鉴权规则。 */
    @PostMapping("/auth")
    public OrbitRuleAggregateVO<AuthRuleDetailVO> createAuthRule(
            @Valid @RequestBody AuthRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.createAuthRule(request, headers);
    }

    /** 更新鉴权规则。 */
    @PatchMapping("/auth/{id}")
    public OrbitRuleAggregateVO<AuthRuleDetailVO> updateAuthRule(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody AuthRuleRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitGovernanceRuleClient.updateAuthRule(id, request, headers);
    }

    /** 删除鉴权规则。 */
    @DeleteMapping("/auth/{id}")
    public ResponseEntity<Void> deleteAuthRule(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        orbitGovernanceRuleClient.deleteAuthRule(id, headers);
        return ResponseEntity.noContent().build();
    }
}
