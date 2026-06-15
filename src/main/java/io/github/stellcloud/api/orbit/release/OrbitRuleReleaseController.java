package io.github.stellcloud.api.orbit.release;

import io.github.stellcloud.api.orbit.release.vo.ApprovalActionRequestVO;
import io.github.stellcloud.api.orbit.release.vo.ApprovalVO;
import io.github.stellcloud.api.orbit.release.vo.PageVO;
import io.github.stellcloud.api.orbit.release.vo.PublishGovernanceRulesRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RecoverRuleReleaseRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RetryRuleReleaseRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RollbackRuleReleaseRequestVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseDiffVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseDryRunVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseImpactVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseSummaryVO;
import io.github.stellcloud.api.orbit.release.vo.RuleReleaseVO;
import io.github.stellcloud.infrastructure.orbit.OrbitRuleReleaseClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Orbit 规则发布页面接口。 */
@Validated
@RestController
@RequestMapping("/api/stellcloud/control-plane/v1/orbit/rule-releases")
public class OrbitRuleReleaseController {

    private final OrbitRuleReleaseClient orbitRuleReleaseClient;

    public OrbitRuleReleaseController(OrbitRuleReleaseClient orbitRuleReleaseClient) {
        this.orbitRuleReleaseClient = orbitRuleReleaseClient;
    }

    /** 分页查询发布列表。 */
    @GetMapping
    public PageVO<RuleReleaseSummaryVO> search(
            @RequestParam(name = "instanceSpaceId", required = false) String instanceSpaceId,
            @RequestParam(name = "applicationId", required = false) String applicationId,
            @RequestParam(name = "releaseStatus", required = false) String releaseStatus,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.search(
                instanceSpaceId, applicationId, releaseStatus, keyword, page, size, headers);
    }

    /** 查询发布详情。 */
    @GetMapping("/{id}")
    public RuleReleaseVO detail(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.detail(id, headers);
    }

    /** 对比两个发布版本。 */
    @GetMapping("/{id}/diff")
    public RuleReleaseDiffVO diff(
            @PathVariable("id") @NotBlank String id,
            @RequestParam(name = "baseReleaseId") String baseReleaseId,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.diff(id, baseReleaseId, headers);
    }

    /** 分析发布影响面。 */
    @GetMapping("/{id}/impact")
    public RuleReleaseImpactVO impact(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.impact(id, headers);
    }

    /** 发布服务治理规则。 */
    @PostMapping
    public RuleReleaseVO publish(
            @Valid @RequestBody PublishGovernanceRulesRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.publish(request, headers);
    }

    /** 发布前 dry-run 编译和解释。 */
    @PostMapping("/dry-run")
    public RuleReleaseDryRunVO dryRun(
            @Valid @RequestBody PublishGovernanceRulesRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.dryRun(request, headers);
    }

    /** 重试发布失败或待处理的发布项。 */
    @PostMapping("/{id}/retry")
    public RuleReleaseVO retry(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody RetryRuleReleaseRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.retry(id, request, headers);
    }

    /** 重试单条发布记录。 */
    @PostMapping("/{id}/publish-records/{recordId}/retry")
    public RuleReleaseVO retryPublishRecord(
            @PathVariable("id") @NotBlank String id,
            @PathVariable("recordId") @NotBlank String recordId,
            @Valid @RequestBody RetryRuleReleaseRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.retryPublishRecord(id, recordId, request, headers);
    }

    /** 人工恢复发布状态。 */
    @PostMapping("/{id}/recover")
    public RuleReleaseVO recover(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody RecoverRuleReleaseRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.recover(id, request, headers);
    }

    /** 按历史发布版本回滚并重新发布。 */
    @PostMapping("/{id}/rollback")
    public RuleReleaseVO rollback(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody RollbackRuleReleaseRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.rollback(id, request, headers);
    }

    /** 查询发布审批时间线。 */
    @GetMapping("/{id}/approvals")
    public List<ApprovalVO> approvals(
            @PathVariable("id") @NotBlank String id,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.approvals(id, headers);
    }

    /** 提交发布审批。 */
    @PostMapping("/{id}/approvals/submit")
    public ApprovalVO submitApproval(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody ApprovalActionRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.submitApproval(id, request, headers);
    }

    /** 通过发布审批。 */
    @PostMapping("/{id}/approvals/approve")
    public ApprovalVO approve(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody ApprovalActionRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.approve(id, request, headers);
    }

    /** 驳回发布审批。 */
    @PostMapping("/{id}/approvals/reject")
    public ApprovalVO reject(
            @PathVariable("id") @NotBlank String id,
            @Valid @RequestBody ApprovalActionRequestVO request,
            @RequestHeader HttpHeaders headers) {
        return orbitRuleReleaseClient.reject(id, request, headers);
    }
}
