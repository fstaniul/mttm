package com.staniul.security.jwt;

import com.staniul.security.web.Teamspeak3AuthenticationToken;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

@Component
public class JWTTokenProvider {
    @Value("${jwt.secret}")
    private String secret;

    public String generateToken (Client client, long expiration) {
        SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
        Key signingKey = new SecretKeySpec(secret.getBytes(), algorithm.getJcaName());

        JwtBuilder builder = Jwts.builder()
                .setId(Integer.toString(client.getDatabaseId()))
                .setIssuer(client.getIp())
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

        return new Teamspeak3AuthenticationToken(Integer.parseInt(claims.getId()), claims.getIssuer(), null, new ArrayList<>());
    }
}
