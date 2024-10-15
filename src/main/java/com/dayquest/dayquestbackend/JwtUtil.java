package com.dayquest.dayquestbackend;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("jwt.secret")
  private String secret;

  public UUID extractUuid(String token) {
    String subject = extractClaim(token, Claims::getSubject);
    return UUID.fromString(subject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
  }

  public String generateToken(UUID uuid) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, uuid);
  }

  private String createToken(Map<String, Object> claims, UUID subject) {
    return Jwts.builder().setClaims(claims).setSubject(subject.toString())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .signWith(SignatureAlgorithm.HS256, secret).compact();
  }

  public boolean validateToken(String token, UUID uuid) {
    return extractUuid(token).toString().equals(uuid.toString());
  }
}
