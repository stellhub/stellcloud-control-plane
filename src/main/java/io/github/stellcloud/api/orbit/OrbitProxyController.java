package io.github.stellcloud.api.orbit;

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

/** 服务治理页面接口代理。 */
@RestController
@RequestMapping("/api/stellcloud/control-plane/v1/orbit")
public class OrbitProxyController extends AbstractDownstreamProxyController {

    private static final String UPSTREAM_PREFIX = "/api/stellcloud/control-plane/v1/orbit";
    private static final String DOWNSTREAM_PREFIX = "/api/stellorbit";

    public OrbitProxyController(
            DownstreamHttpGateway gateway,
            @Qualifier("orbitHttpClient") StellfluxHttpClient orbitHttpClient) {
        super(gateway, orbitHttpClient, UPSTREAM_PREFIX);
    }

    /** 查询服务治理资源。 */
    @GetMapping("/**")
    public ResponseEntity<byte[]> get(HttpServletRequest request) {
        return proxy(request, null);
    }

    /** 创建服务治理资源或触发治理动作。 */
    @PostMapping("/**")
    public ResponseEntity<byte[]> post(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 全量更新服务治理资源。 */
    @PutMapping("/**")
    public ResponseEntity<byte[]> put(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 局部更新服务治理资源。 */
    @PatchMapping("/**")
    public ResponseEntity<byte[]> patch(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    /** 删除服务治理资源。 */
    @DeleteMapping("/**")
    public ResponseEntity<byte[]> delete(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxy(request, body);
    }

    @Override
    protected String downstreamPath(HttpServletRequest request) {
        return DOWNSTREAM_PREFIX + remainingPath(request);
    }
}
