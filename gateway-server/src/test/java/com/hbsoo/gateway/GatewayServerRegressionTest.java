package com.hbsoo.gateway;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.config.ServerInfoProperties;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GatewayServerApplication.class, properties = "spring.profiles.include=")
@ActiveProfiles("regression")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayServerRegressionTest {

    private static final String BASE = "http://127.0.0.1:25555";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static String jwtToken;

    @Autowired
    private ServerInfoProperties serverInfoProperties;

    @Autowired
    @Qualifier("insideServer")
    private NetworkServer insideServer;

    @Autowired
    @Qualifier("outsideServer")
    private NetworkServer outsideServer;

    @Test
    @Order(1)
    void contextLoads_andServersBindExpectedPorts() {
        assertEquals(1000, serverInfoProperties.getId());
        assertNotNull(insideServer);
        assertNotNull(outsideServer);
        assertEquals(26000, serverInfoProperties.getInsideServers().stream()
                .filter(s -> s.getId().equals(1000))
                .findFirst()
                .orElseThrow()
                .getPort());
        assertTrue((Boolean) serverInfoProperties.getOutsideServer().get("enable"));
        assertEquals(25555, Integer.parseInt(serverInfoProperties.getOutsideServer().get("port").toString()));
    }

    @Test
    @Order(2)
    void httpIndex_returnsJson() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(BASE + "/index")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("zun"));
    }

    @Test
    @Order(3)
    void httpProtected_withoutToken_denied() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(BASE + "/regression/protected")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(response.body().contains("权限不足") || response.body().isEmpty());
    }

    @Test
    @Order(4)
    void httpLogin_returnsJwt() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(BASE + "/regression/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Map<String, Object> body = GSON.fromJson(response.body(), MAP_TYPE);
        jwtToken = body.get("token").toString();
        assertNotNull(jwtToken);
        assertEquals("Authentication", body.get("header"));
    }

    @Test
    @Order(5)
    void httpProtected_withValidToken_ok() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(BASE + "/regression/protected"))
                        .header("Authentication", jwtToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Map<String, Object> body = GSON.fromJson(response.body(), MAP_TYPE);
        assertEquals("ok", body.get("status"));
    }

    @Test
    @Order(6)
    void httpProtected_withTamperedToken_denied() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(BASE + "/regression/protected"))
                        .header("Authentication", jwtToken + ".tampered")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(response.body().contains("权限不足") || response.body().isEmpty());
    }

}
