package com.staniul.api.security;

import com.staniul.api.security.auth.ApiClientDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenUtil {
    private static final String CLAIM_ID_KEY = "id";
    private static final String CLAIM_AUTHORITIES_KEY = "authorities";

    @Value("${jwt.secret}")
    private String secret;

    ApiClientDetails getApiClientDetailsFromToken(String authToken) {
        Claims claims = getClaimsFromToken(authToken);

        if (claims == null) return null;

        List<String> authorities = (ArrayList<String>) claims.get(CLAIM_AUTHORITIES_KEY);
        int id = (Integer) claims.get(CLAIM_ID_KEY);

        if (id <= 0 || authorities == null) return null;

        return new ApiClientDetails(id, authorities);
    }

    private Claims getClaimsFromToken (String authToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(authToken)
                    .getBody();
        } catch (Exception e) {
            claims = null;
        }

        return claims;
    }

    String generateAuthenticationToken (ApiClientDetails clientDetails) {
        Map<String, Object> claims = new HashMap<>();

        claims.put(CLAIM_ID_KEY, clientDetails.getDatabaseId());
        claims.put(CLAIM_AUTHORITIES_KEY, clientDetails.getAuthorities());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
