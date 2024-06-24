package com.hbsoo.permisson.utils;

import com.google.gson.Gson;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Date;
import java.util.Map;


/**
 * Created by zun.wei on 2023/12/10.
 */
@ConfigurationProperties(prefix = "hbsoo.server.jwt")//绑定到配置文件 可以在yml给属性初始化值
public class JwtUtils {

    private long expire = 360000L;
    private String secret = "$%^&ad)bse(fed.?a!@#$%^&";
    private String header = "Authorization";

    @Autowired
    private AESUtil aesUtil;

    // 生成jwt
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
                .setHeaderParam("typ", "JWT")
                .setId(id)
                //.setSubject(username)
                //.claim("openid", openid)
                .claim("param", encrypt)
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)// 7天過期
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    // 解析jwt
    public Claims getClaimByToken(String jwt) {
        try {
            final Claims body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(jwt)
                    .getBody();
            String encrypt = body.get("param").toString();
            final String decrypt = aesUtil.decrypt(encrypt);
            body.put("param", decrypt);
            return body;
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
            //throw new SystemException(_102);
        }
        return null;
    }

    // jwt是否过期
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
