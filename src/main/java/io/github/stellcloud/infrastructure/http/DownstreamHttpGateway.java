package io.github.stellcloud.infrastructure.http;

import io.github.stellflux.http.client.StellfluxHttpClient;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** 下游 HTTP 透传调用网关。 */
@Component
public class DownstreamHttpGateway {

    /**
     * 通过 Stellflux HTTP Client 调用下游接口。
     *
     * @param client 下游 HTTP 客户端
     * @param downstreamPath 下游路径
     * @param servletRequest 当前 HTTP 请求
     * @param body 请求体
     * @return 下游响应
     */
    public ResponseEntity<byte[]> exchange(
            StellfluxHttpClient client,
            String downstreamPath,
            HttpServletRequest servletRequest,
            byte[] body) {
        Request request = buildRequest(client, downstreamPath, servletRequest, body);
        try (Response response = client.newCall(request).execute()) {
            byte[] responseBody =
                    response.body() == null ? new byte[0] : response.body().bytes();
            return ResponseEntity.status(response.code())
                    .headers(toResponseHeaders(response))
                    .body(responseBody);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(errorBody(ex).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private Request buildRequest(
            StellfluxHttpClient client,
            String downstreamPath,
            HttpServletRequest servletRequest,
            byte[] body) {
        HttpUrl url = appendQuery(client.buildUrl(downstreamPath), servletRequest.getParameterMap());
        Request.Builder builder = new Request.Builder().url(url);
        copyRequestHeaders(servletRequest, builder);
        builder.method(servletRequest.getMethod(), requestBody(servletRequest, body));
        return builder.build();
    }

    private HttpUrl appendQuery(HttpUrl baseUrl, Map<String, String[]> parameters) {
        HttpUrl.Builder builder = baseUrl.newBuilder();
        parameters.forEach(
                (name, values) -> {
                    if (values == null || values.length == 0) {
                        builder.addQueryParameter(name, "");
                        return;
                    }
                    for (String value : values) {
                        builder.addQueryParameter(name, value);
                    }
                });
        return builder.build();
    }

    private RequestBody requestBody(HttpServletRequest request, byte[] body) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(method) || "HEAD".equals(method)) {
            return null;
        }
        byte[] safeBody = body == null ? new byte[0] : body;
        MediaType mediaType =
                request.getContentType() == null ? null : MediaType.parse(request.getContentType());
        return RequestBody.create(safeBody, mediaType);
    }

    private void copyRequestHeaders(HttpServletRequest servletRequest, Request.Builder builder) {
        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        if (headerNames == null) {
            return;
        }
        for (String headerName : Collections.list(headerNames)) {
            if (isHopByHopHeader(headerName)) {
                continue;
            }
            Enumeration<String> values = servletRequest.getHeaders(headerName);
            while (values.hasMoreElements()) {
                builder.addHeader(headerName, values.nextElement());
            }
        }
    }

    private HttpHeaders toResponseHeaders(Response response) {
        HttpHeaders headers = new HttpHeaders();
        response.headers()
                .forEach(
                        pair -> {
                            if (!isHopByHopHeader(pair.getFirst())) {
                                headers.add(pair.getFirst(), pair.getSecond());
                            }
                        });
        return headers;
    }

    private boolean isHopByHopHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        String normalized = headerName.toLowerCase(Locale.ROOT);
        return normalized.equals("connection")
                || normalized.equals("keep-alive")
                || normalized.equals("proxy-authenticate")
                || normalized.equals("proxy-authorization")
                || normalized.equals("te")
                || normalized.equals("trailer")
                || normalized.equals("transfer-encoding")
                || normalized.equals("upgrade")
                || normalized.equals("host")
                || normalized.equals("content-length");
    }

    private String errorBody(IOException ex) {
        String message = ex.getMessage() == null ? "downstream request failed" : ex.getMessage();
        return "{\"code\":\"DOWNSTREAM_UNAVAILABLE\",\"message\":\"" + escapeJson(message) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
