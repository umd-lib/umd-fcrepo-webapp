package edu.umd.lib.fcrepo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static java.time.Instant.now;

public class AuthTokenService {
  private static final Logger logger = LoggerFactory.getLogger(AuthTokenService.class);

  private String secret;

  public AuthTokenService() {}

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public Key getSecretKey() {
    return new SecretKeySpec(Base64.getDecoder().decode(secret), SignatureAlgorithm.HS256.getJcaName());
  }

  public String createToken(final String subject, final String issuer, final Date expirationDate, final String role) {
    logger.info("Creating token with subject: {}", subject);
    logger.info("Issuer: {}", issuer);
    logger.info("Expiration date: {}", expirationDate);
    logger.info("Role: {}", role);

    return Jwts.builder()
        .setSubject(subject)
        .setIssuer(issuer)
        .setExpiration(expirationDate)
        .claim("role", role)
        .signWith(getSecretKey())
        .compact();
  }
}
