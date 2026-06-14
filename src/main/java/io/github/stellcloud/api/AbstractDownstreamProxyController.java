package io.github.stellcloud.api;

import io.github.stellflux.http.client.StellfluxHttpClient;
import io.github.stellcloud.infrastructure.http.DownstreamHttpGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/** 下游服务代理控制器基类。 */
public abstract class AbstractDownstreamProxyController {

    private final DownstreamHttpGateway gateway;
    private final StellfluxHttpClient client;
    private final String upstreamPrefix;

    protected AbstractDownstreamProxyController(
            DownstreamHttpGateway gateway,
            StellfluxHttpClient client,
            String upstreamPrefix) {
        this.gateway = gateway;
        this.client = client;
        this.upstreamPrefix = upstreamPrefix;
    }

    /**
     * 代理当前请求到下游服务。
     *
     * @param request 当前 HTTP 请求
     * @param body 请求体
     * @return 下游响应
     */
    protected ResponseEntity<byte[]> proxy(HttpServletRequest request, byte[] body) {
        return gateway.exchange(client, downstreamPath(request), request, body);
    }

    /**
     * 将控制面路径转换为下游路径。
     *
     * @param request 当前 HTTP 请求
     * @return 下游请求路径
     */
    protected abstract String downstreamPath(HttpServletRequest request);

    protected String remainingPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (!uri.startsWith(upstreamPrefix)) {
            return "/";
        }
        String remaining = uri.substring(upstreamPrefix.length());
        return remaining.isBlank() ? "/" : remaining;
    }
}
