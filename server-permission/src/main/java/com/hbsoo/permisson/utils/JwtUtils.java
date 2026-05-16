package com.hbsoo.permisson.utils;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

/**
 * Created by zun.wei on 2023/12/10.
 */
@ConfigurationProperties(prefix = "hbsoo.server.jwt")
public class JwtUtils {

    private long expire = 360000L;
    private String secret = "$%^&ad)bse(fed.?a!@#$%^&";
    private String header = "Authorization";

    @Autowired
    private AESUtil aesUtil;

    private SecretKey signingKey() {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-512")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not available", e);
        }
    }

    public String generateToken(String id, Map<String, String> param) {
        Date nowDate = new Date();
        Date expireDate = new Date(nowDate.getTime() + 1000 * expire);
        Gson gson = new Gson();
        String json = gson.toJson(param);
        String encrypt = null;
        try {
            encrypt = aesUtil.encrypt(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .id(id)
                .claim("param", encrypt)
                .issuedAt(nowDate)
                .expiration(expireDate)
                .signWith(signingKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Claims getClaimByToken(String jwt) {
        try {
            Claims body = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            String encrypt = body.get("param").toString();
            String decrypt = aesUtil.decrypt(encrypt);
            return Jwts.claims()
                    .add(body)
                    .add("param", decrypt)
                    .build();
        } catch (Exception e) {
            if (e instanceof ExpiredJwtException) {
                //throw new SystemException(_108);
            }
            if (e instanceof SignatureException) {
                //throw new SystemException(_109);
            }
            if (e instanceof MalformedJwtException) {
                //throw new SystemException(_110);
            }
        }
        return null;
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
