package edu.umd.lib.fcrepo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.security.Principal;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

public class TokenAuthnzFilter implements Filter {
  private final Logger logger = LoggerFactory.getLogger(TokenAuthnzFilter.class);

  private Key key;

  private LdapRoleLookupService ldapRoleLookupService;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    final WebApplicationContext context = WebApplicationContextUtils
        .getRequiredWebApplicationContext(filterConfig.getServletContext());
    key = context.getBean(SecretKeyService.class).getSecretKey();
    ldapRoleLookupService = context.getBean(LdapRoleLookupService.class);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;

    final String authorizationHeader = httpRequest.getHeader("Authorization");
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer")) {
      String tokenString = authorizationHeader.substring(authorizationHeader.indexOf(" ") + 1);
      logger.info("Bearer token: '{}'", tokenString);
      try {
        final Jws<Claims> jws = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(tokenString);
        logger.info("Successfully parsed JWT");
        // we can safely trust the JWT
        final Claims tokenBody = jws.getBody();
        final String subject = tokenBody.getSubject();
        logger.info("Token subject is {}", subject);
        final String issuer = tokenBody.getIssuer();
        logger.info("Token created by {}", issuer);
        final String issuerRole = ldapRoleLookupService.getRole(issuer);
        logger.info("Token creator has role {}", issuerRole);
        logger.info("Token expires on {}", tokenBody.getExpiration());
        final Principal userPrincipal = new AttributePrincipalImpl(subject);
        final HttpServletRequest newRequest = requestWithUserAndRole(httpRequest, userPrincipal, issuerRole);
        logger.debug("Request remote user: {}", newRequest.getRemoteUser());
        logger.debug("Request user principal: {}", newRequest.getUserPrincipal());
        logger.debug("Request user is admin? {}", newRequest.isUserInRole("fedoraAdmin"));
        chain.doFilter(newRequest, response);
      } catch (final JwtException e) {
        // we *cannot* use the JWT as intended by its creator
        logger.error("Unable to parse JWT: {}", e.getMessage());
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        // see https://tools.ietf.org/html/rfc6750#section-3.1
        httpResponse.setHeader("WWW-Authenticate",
            "Bearer error=\"invalid_token\", error_description=\"" + e.getMessage() + "\"");
        httpResponse.sendError(SC_UNAUTHORIZED);
      }
    } else {
      // no "Authorization: Bearer ..." header, go to next filter
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {

  }

  private HttpServletRequest requestWithUserAndRole(HttpServletRequest request, Principal userPrincipal, String role) {
    return new ProvideRoleRequestWrapper(new ProvideUserPrincipalRequestWrapper(request, userPrincipal), role);
  }
}
