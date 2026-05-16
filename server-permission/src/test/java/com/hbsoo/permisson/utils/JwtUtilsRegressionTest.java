package com.hbsoo.permisson.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsRegressionTest {

    private JwtUtils jwtUtils;
    private AESUtil aesUtil;

    @BeforeEach
    void setUp() {
        aesUtil = new AESUtil();
        jwtUtils = new JwtUtils();
        jwtUtils.setSecret("regression-test-secret-key-32bytes!!");
        jwtUtils.setExpire(3600L);
        org.springframework.test.util.ReflectionTestUtils.setField(jwtUtils, "aesUtil", aesUtil);
    }

    @Test
    void generateAndParseToken_roundTrip() {
        String token = jwtUtils.generateToken("user-1", Map.of("permission", "READ,WRITE"));
        assertNotNull(token);

        Claims claims = jwtUtils.getClaimByToken(token);
        assertNotNull(claims);
        assertEquals("user-1", claims.getId());
        assertFalse(jwtUtils.isTokenExpired(claims));

        Object param = claims.get("param");
        assertNotNull(param);
        assertTrue(param.toString().contains("permission"));
        assertTrue(param.toString().contains("READ"));
    }

    @Test
    void parseToken_invalidToken_returnsNull() {
        assertNull(jwtUtils.getClaimByToken("invalid.token.value"));
    }

    @Test
    void parseToken_tamperedSignature_returnsNull() {
        String token = jwtUtils.generateToken("user-2", Map.of("permission", "ADMIN"));
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertNull(jwtUtils.getClaimByToken(tampered));
    }

}
