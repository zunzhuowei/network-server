package com.hbsoo.server.message;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/6/7.
 */
public final class HttpPackage {

    String uri;
    HttpHeaders headers;
    String path;
    Map<String, List<String>> parameters;
    byte[] body;
    FullHttpRequest fullHttpRequest;
    String method;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public FullHttpRequest getFullHttpRequest() {
        return fullHttpRequest;
    }

    public void setFullHttpRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = fullHttpRequest;
    }

    @Override
    public String toString() {
        return "HttpPackage{" +
                "uri='" + uri + '\'' +
                ", headers=" + headers +
                ", path='" + path + '\'' +
                ", parameters=" + parameters +
                ", body=" + Arrays.toString(body) +
                '}';
    }
}
