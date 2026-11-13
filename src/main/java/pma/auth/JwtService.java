package pma.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite e valida JWTs assinados com HMAC-SHA256.
 *
 * - O segredo vem de JWT_SECRET e precisa ter pelo menos 32 caracteres (256 bits);
 *   se for menor, o serviço nem sobe.
 * - O token guarda o id da conta no "sub" e o papel no claim "role".
 */
@Service
public class JwtService {

  private static final String ISSUER = "pma.auth";
  private static final String CLAIM_ROLE = "role";

  private final SecretKey key;
  private final Duration duration;

  public JwtService(
      @Value("${pma.jwt.secret}") String secret,
      @Value("${pma.jwt.duration}") Duration duration) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.duration = duration;
  }

  public String create(String idAccount, String role) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .issuer(ISSUER)
        .subject(idAccount)
        .claim(CLAIM_ROLE, role)
        .issuedAt(new Date(now))
        .expiration(new Date(now + duration.toMillis()))
        .signWith(key)
        .compact();
  }

  /**
   * Confere assinatura, emissor e expiração.
   *
   * @throws JwtException se o token for inválido, adulterado ou expirado
   */
  public SolveOut solve(String jwt) {
    Claims claims = Jwts.parser()
        .verifyWith(key)
        .requireIssuer(ISSUER)
        .build()
        .parseSignedClaims(jwt)
        .getPayload();
    return SolveOut.builder()
        .idAccount(claims.getSubject())
        .role(claims.get(CLAIM_ROLE, String.class))
        .build();
  }

}
