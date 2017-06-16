package com.staniul.security.jwt;

import com.staniul.security.web.Teamspeak3AuthenticationToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

@Component
public class JWTTokenProvider {
    @Value("${jwt.secret}")
    private String secret;

    public String generateToken (int databaseId, String ip, long expiration) {
        SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
        Key signingKey = new SecretKeySpec(secret.getBytes(), algorithm.getJcaName());

        JwtBuilder builder = Jwts.builder()
                .setId(Integer.toString(databaseId))
                .setIssuer(ip)
                .signWith(algorithm, signingKey);

        if (expiration > 0L)
            builder.setExpiration(new Date(System.currentTimeMillis() + expiration));

        return builder.compact();
    }

    public Teamspeak3AuthenticationToken parseToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody();

        return new Teamspeak3AuthenticationToken(Integer.parseInt(claims.getId()), claims.getIssuer(), null);
    }
}
