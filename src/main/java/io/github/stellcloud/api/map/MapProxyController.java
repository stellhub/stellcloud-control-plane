package io.github.stellcloud.api.map;

import io.github.stellflux.http.client.StellfluxHttpClient;
import io.github.stellcloud.api.AbstractDownstreamProxyController;
import io.github.stellcloud.infrastructure.http.DownstreamHttpGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册中心页面接口代理。 */
@RestController
@RequestMapping("/api/stellcloud/control-plane/v1/map")
public class MapProxyController extends AbstractDownstreamProxyController {

    private static final String UPSTREAM_PREFIX = "/api/stellcloud/control-plane/v1/map";

    public MapProxyController(
            DownstreamHttpGateway gateway,
            @Qualifier("mapHttpClient") StellfluxHttpClient mapHttpClient) {
        super(gateway, mapHttpClient, UPSTREAM_PREFIX);
    }

    /** 查询注册中心资源。 */
    @GetMapping("/**")
    public ResponseEntity<byte[]> get(HttpServletRequest request) {
        return proxy(request, null);
    }

    /** 创建注册中心资源或触发管理动作。 */
    @PostMapping("/**")
    public ResponseEntity<byte[]> post(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 全量更新注册中心资源。 */
    @PutMapping("/**")
    public ResponseEntity<byte[]> put(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 局部更新注册中心资源。 */
    @PatchMapping("/**")
    public ResponseEntity<byte[]> patch(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 删除注册中心资源。 */
    @DeleteMapping("/**")
    public ResponseEntity<byte[]> delete(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    @Override
    protected String downstreamPath(HttpServletRequest request) {
        String remaining = remainingPath(request);
        if (remaining.startsWith("/admin/") || remaining.equals("/healthz") || remaining.equals("/readyz")) {
            return remaining;
        }
        return "/api/v1" + remaining;
    }
}
