package com.hbsoo.server.utils;

import com.google.gson.Gson;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.UserSession;

import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/7/5.
 */
public final class HttpRequestParser {

    private String uri;
    private String path;
    private String method;
    private Map<String, List<String>> parameters;
    private Map<String, String> headers;
    private byte[] body;
    private UserSession userSession;

    public static HttpRequestParser parse(NetworkPacket.Decoder decoder) {
        HttpRequestParser httpRequestParser = new HttpRequestParser();
        String userSessionStr = decoder.readStr();
        String uri = decoder.readStr();
        String path = decoder.readStr();
        String method = decoder.readStr();
        String parameterStr = decoder.readStr();
        byte[] body = decoder.readBytes();
        String headerStr = decoder.readStr();
        Gson gson = new Gson();
        UserSession userSession = gson.fromJson(userSessionStr, UserSession.class);
        httpRequestParser.uri = uri;
        httpRequestParser.path = path;
        httpRequestParser.method = method;
        httpRequestParser.parameters = gson.fromJson(parameterStr, Map.class);
        httpRequestParser.body = body;
        httpRequestParser.headers = gson.fromJson(headerStr, Map.class);
        httpRequestParser.userSession = userSession;
        return httpRequestParser;
    }


    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public UserSession getUserSession() {
        return userSession;
    }
}
