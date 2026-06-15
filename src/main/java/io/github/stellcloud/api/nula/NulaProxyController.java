package io.github.stellcloud.api.nula;

import io.github.stellcloud.api.AbstractDownstreamProxyController;
import io.github.stellcloud.api.nula.vo.AppConfigListVO;
import io.github.stellcloud.api.nula.vo.AppConfigRequestVO;
import io.github.stellcloud.api.nula.vo.AppConfigScopeVO;
import io.github.stellcloud.api.nula.vo.AppConfigVO;
import io.github.stellcloud.api.nula.vo.CommonConfigListVO;
import io.github.stellcloud.api.nula.vo.CommonConfigRequestVO;
import io.github.stellcloud.api.nula.vo.CommonConfigScopeVO;
import io.github.stellcloud.api.nula.vo.CommonConfigVO;
import io.github.stellcloud.api.nula.vo.FeatureFlagListVO;
import io.github.stellcloud.api.nula.vo.FeatureFlagRequestVO;
import io.github.stellcloud.api.nula.vo.FeatureFlagVO;
import io.github.stellcloud.api.nula.vo.HealthVO;
import io.github.stellcloud.api.nula.vo.SharedConfigPageVO;
import io.github.stellcloud.infrastructure.http.DownstreamHttpGateway;
import io.github.stellcloud.infrastructure.nula.NulaControlPlaneClient;
import io.github.stellflux.http.client.StellfluxHttpClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 配置中心页面接口代理。 */
@Validated
@RestController
@RequestMapping("/api/stellcloud/control-plane/v1/nula")
public class NulaProxyController extends AbstractDownstreamProxyController {

    private static final String UPSTREAM_PREFIX = "/api/stellcloud/control-plane/v1/nula";
    private static final String DOWNSTREAM_PREFIX = "/api/v1";
    private static final String PROXY_PREFIX = "/proxy";

    private final NulaControlPlaneClient nulaControlPlaneClient;

    public NulaProxyController(
            DownstreamHttpGateway gateway,
            @Qualifier("nulaHttpClient") StellfluxHttpClient nulaHttpClient,
            NulaControlPlaneClient nulaControlPlaneClient) {
        super(gateway, nulaHttpClient, UPSTREAM_PREFIX);
        this.nulaControlPlaneClient = nulaControlPlaneClient;
    }

    /** 查询配置中心控制面健康状态。 */
    @GetMapping("/health")
    public HealthVO health() {
        return nulaControlPlaneClient.health();
    }

    /** 查询当前应用下的配置范围选项。 */
    @GetMapping({"/configs/scope", "/app-config/scope"})
    public AppConfigScopeVO scope(
            @RequestParam(name = "appId", required = false) String appId,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.scope(appId, appIdHeader);
    }

    /** 查询应用配置列表。 */
    @GetMapping({"/configs", "/app-config"})
    public AppConfigListVO listConfigs(
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestParam(name = "group", required = false) String group,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.listConfigs(appId, environment, cluster, group, appIdHeader);
    }

    /** 查询应用配置详情。 */
    @GetMapping({"/configs/{configId}", "/app-config/{configId}"})
    public AppConfigVO getConfig(
            @PathVariable("configId") @NotBlank String configId,
            @RequestParam(name = "appId", required = false) String appId,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.getConfig(configId, appId, appIdHeader);
    }

    /** 创建应用配置草稿。 */
    @PostMapping({"/configs", "/app-config"})
    public AppConfigVO createConfig(
            @Valid @RequestBody AppConfigRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.createConfig(request, appIdHeader, operatorHeader);
    }

    /** 保存应用配置草稿。 */
    @PutMapping({"/configs/{configId}", "/app-config/{configId}"})
    public AppConfigVO saveDraft(
            @PathVariable("configId") @NotBlank String configId,
            @Valid @RequestBody AppConfigRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.saveDraft(configId, request, appIdHeader, operatorHeader);
    }

    /** 发布应用配置。 */
    @PostMapping({"/configs/{configId}/publish", "/app-config/{configId}/publish"})
    public AppConfigVO publish(
            @PathVariable("configId") @NotBlank String configId,
            @Valid @RequestBody AppConfigRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.publish(configId, request, appIdHeader, operatorHeader);
    }

    /** 删除应用配置。 */
    @DeleteMapping({"/configs/{configId}", "/app-config/{configId}"})
    public ResponseEntity<Void> deleteConfig(
            @PathVariable("configId") @NotBlank String configId,
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        nulaControlPlaneClient.deleteConfig(configId, appId, environment, cluster, appIdHeader);
        return ResponseEntity.noContent().build();
    }

    /** 查询公共配置范围选项。 */
    @GetMapping("/common-config/scope")
    public CommonConfigScopeVO commonConfigScope(
            @RequestParam(name = "ownerId", required = false) String ownerId,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader) {
        return nulaControlPlaneClient.commonConfigScope(ownerId, ownerIdHeader);
    }

    /** 查询公共配置列表。 */
    @GetMapping("/common-config")
    public CommonConfigListVO listCommonConfigs(
            @RequestParam(name = "ownerId", required = false) String ownerId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestParam(name = "group", required = false) String group,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader) {
        return nulaControlPlaneClient.listCommonConfigs(ownerId, environment, cluster, group, ownerIdHeader);
    }

    /** 查询公共配置详情。 */
    @GetMapping("/common-config/{configId}")
    public CommonConfigVO getCommonConfig(
            @PathVariable("configId") @NotBlank String configId,
            @RequestParam(name = "ownerId", required = false) String ownerId,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader) {
        return nulaControlPlaneClient.getCommonConfig(configId, ownerId, ownerIdHeader);
    }

    /** 创建公共配置草稿。 */
    @PostMapping("/common-config")
    public CommonConfigVO createCommonConfig(
            @Valid @RequestBody CommonConfigRequestVO request,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.createCommonConfig(request, ownerIdHeader, operatorHeader);
    }

    /** 保存公共配置草稿。 */
    @PutMapping("/common-config/{configId}")
    public CommonConfigVO saveCommonConfigDraft(
            @PathVariable("configId") @NotBlank String configId,
            @Valid @RequestBody CommonConfigRequestVO request,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.saveCommonConfigDraft(configId, request, ownerIdHeader, operatorHeader);
    }

    /** 发布公共配置。 */
    @PostMapping("/common-config/{configId}/publish")
    public CommonConfigVO publishCommonConfig(
            @PathVariable("configId") @NotBlank String configId,
            @Valid @RequestBody CommonConfigRequestVO request,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.publishCommonConfig(configId, request, ownerIdHeader, operatorHeader);
    }

    /** 删除公共配置。 */
    @DeleteMapping("/common-config/{configId}")
    public ResponseEntity<Void> deleteCommonConfig(
            @PathVariable("configId") @NotBlank String configId,
            @RequestParam(name = "ownerId", required = false) String ownerId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestHeader(name = "X-Stell-Public-Owner-Id", required = false) String ownerIdHeader) {
        nulaControlPlaneClient.deleteCommonConfig(configId, ownerId, environment, cluster, ownerIdHeader);
        return ResponseEntity.noContent().build();
    }

    /** 查询 Feature Flag 列表。 */
    @GetMapping("/feature-flags")
    public FeatureFlagListVO listFeatureFlags(
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestParam(name = "group", required = false) String group,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.listFeatureFlags(appId, environment, cluster, group, appIdHeader);
    }

    /** 查询 Feature Flag 详情。 */
    @GetMapping("/feature-flags/{flagKey}")
    public FeatureFlagVO getFeatureFlag(
            @PathVariable("flagKey") @NotBlank String flagKey,
            @RequestParam(name = "appId", required = false) String appId,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.getFeatureFlag(flagKey, appId, appIdHeader);
    }

    /** 创建 Feature Flag 草稿。 */
    @PostMapping("/feature-flags")
    public FeatureFlagVO createFeatureFlag(
            @Valid @RequestBody FeatureFlagRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.createFeatureFlag(request, appIdHeader, operatorHeader);
    }

    /** 保存 Feature Flag 草稿。 */
    @PutMapping("/feature-flags/{flagKey}")
    public FeatureFlagVO saveFeatureFlagDraft(
            @PathVariable("flagKey") @NotBlank String flagKey,
            @Valid @RequestBody FeatureFlagRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.saveFeatureFlagDraft(flagKey, request, appIdHeader, operatorHeader);
    }

    /** 发布 Feature Flag。 */
    @PostMapping("/feature-flags/{flagKey}/publish")
    public FeatureFlagVO publishFeatureFlag(
            @PathVariable("flagKey") @NotBlank String flagKey,
            @Valid @RequestBody FeatureFlagRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.publishFeatureFlag(flagKey, request, appIdHeader, operatorHeader);
    }

    /** 删除 Feature Flag。 */
    @DeleteMapping("/feature-flags/{flagKey}")
    public ResponseEntity<Void> deleteFeatureFlag(
            @PathVariable("flagKey") @NotBlank String flagKey,
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        nulaControlPlaneClient.deleteFeatureFlag(flagKey, appId, environment, cluster, appIdHeader);
        return ResponseEntity.noContent().build();
    }

    /** 查询通用配置页面展示数据。 */
    @GetMapping("/shared-config/page")
    public SharedConfigPageVO sharedConfigPage() {
        return nulaControlPlaneClient.sharedConfigPage();
    }

    /** 兜底代理查询配置中心下游资源。 */
    @GetMapping("/proxy/**")
    public ResponseEntity<byte[]> proxyGet(HttpServletRequest request) {
        return proxy(request, null);
    }

    /** 兜底代理创建配置中心下游资源或触发生命周期动作。 */
    @PostMapping("/proxy/**")
    public ResponseEntity<byte[]> proxyPost(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 兜底代理创建或更新配置中心下游资源。 */
    @PutMapping("/proxy/**")
    public ResponseEntity<byte[]> proxyPut(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 兜底代理局部更新配置中心下游资源。 */
    @PatchMapping("/proxy/**")
    public ResponseEntity<byte[]> proxyPatch(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 兜底代理删除配置中心下游资源。 */
    @DeleteMapping("/proxy/**")
    public ResponseEntity<byte[]> proxyDelete(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    @Override
    protected String downstreamPath(HttpServletRequest request) {
        String remaining = remainingPath(request);
        if (remaining.startsWith(PROXY_PREFIX)) {
            String downstreamRemaining = remaining.substring(PROXY_PREFIX.length());
            return DOWNSTREAM_PREFIX + (downstreamRemaining.isBlank() ? "/" : downstreamRemaining);
        }
        return DOWNSTREAM_PREFIX + (remaining.isBlank() ? "/" : remaining);
    }
}
