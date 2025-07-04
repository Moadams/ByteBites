package com.moadams.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {


    @Value("${application.security.jwt.secret-key}")
    private String SECRET_KEY;


    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }



    public String extractRoles(String token) {
        final String ROLES_CLAIM_NAME = "roles";
        List<?> rolesList = extractClaim(token, claims -> claims.get(ROLES_CLAIM_NAME, List.class));
        if (rolesList == null || rolesList.isEmpty()){
            return "";
        }
        return rolesList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }



    public boolean validateToken(String token) {
        try {

            Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token);
            log.info("JWT token is valid.");
            return true;
        } catch (Exception e) {

            log.error("JWT Validation Error: " + e.getMessage());
            return false;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

}