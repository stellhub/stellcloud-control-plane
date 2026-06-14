package io.github.stellcloud.api.nula;

import io.github.stellcloud.api.AbstractDownstreamProxyController;
import io.github.stellcloud.api.nula.vo.AppConfigListVO;
import io.github.stellcloud.api.nula.vo.AppConfigRequestVO;
import io.github.stellcloud.api.nula.vo.AppConfigScopeVO;
import io.github.stellcloud.api.nula.vo.AppConfigVO;
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
    @GetMapping("/configs/scope")
    public AppConfigScopeVO scope(
            @RequestParam(name = "appId", required = false) String appId,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.scope(appId, appIdHeader);
    }

    /** 查询应用配置列表。 */
    @GetMapping("/configs")
    public AppConfigListVO listConfigs(
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.listConfigs(appId, environment, cluster, appIdHeader);
    }

    /** 查询应用配置详情。 */
    @GetMapping("/configs/{configId}")
    public AppConfigVO getConfig(
            @PathVariable("configId") @NotBlank String configId,
            @RequestParam(name = "appId", required = false) String appId,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        return nulaControlPlaneClient.getConfig(configId, appId, appIdHeader);
    }

    /** 创建应用配置草稿。 */
    @PostMapping("/configs")
    public AppConfigVO createConfig(
            @Valid @RequestBody AppConfigRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.createConfig(request, appIdHeader, operatorHeader);
    }

    /** 保存应用配置草稿。 */
    @PutMapping("/configs/{configId}")
    public AppConfigVO saveDraft(
            @PathVariable("configId") @NotBlank String configId,
            @Valid @RequestBody AppConfigRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.saveDraft(configId, request, appIdHeader, operatorHeader);
    }

    /** 发布应用配置。 */
    @PostMapping("/configs/{configId}/publish")
    public AppConfigVO publish(
            @PathVariable("configId") @NotBlank String configId,
            @Valid @RequestBody AppConfigRequestVO request,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader,
            @RequestHeader(name = "X-Operator", required = false) String operatorHeader) {
        return nulaControlPlaneClient.publish(configId, request, appIdHeader, operatorHeader);
    }

    /** 删除应用配置。 */
    @DeleteMapping("/configs/{configId}")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable("configId") @NotBlank String configId,
            @RequestParam(name = "appId", required = false) String appId,
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "cluster", required = false) String cluster,
            @RequestHeader(name = "X-Stell-App-Id", required = false) String appIdHeader) {
        nulaControlPlaneClient.deleteConfig(configId, appId, environment, cluster, appIdHeader);
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
