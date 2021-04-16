package edu.umd.lib.fcrepo;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.BasicUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

public class BasicAuthFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(BasicAuthFilter.class);

  private String credentialsFile;

  private final Map<String, String> userPasswords = new HashMap<>();

  private final Map<String, Set<String>> userRoles = new HashMap<>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @PostConstruct
  public void initializeUsers() throws ServletException {
    final Properties credentialProperties = new Properties();
    try (FileReader in = new FileReader(credentialsFile)) {
      credentialProperties.load(in);
    } catch (IOException ioe) {
      logger.error("An I/O exception occurred reading the mapping file at: '" + credentialsFile + "'", ioe);
      throw new ServletException("An I/O exception occurred reading the mapping file at: '" + credentialsFile + "'",
          ioe);
    }
    for (final Map.Entry<Object, Object> entry : credentialProperties.entrySet()) {
      final String username = (String) entry.getKey();
      final String[] values = ((String) entry.getValue()).split(",");
      final String password = values[0];
      final String[] roles = Arrays.copyOfRange(values, 1, values.length);
      logger.debug("User {} has roles: {}", username, roles);
      userPasswords.put(username, password);
      userRoles.put(username, new HashSet<>(Arrays.asList(roles)));
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;

    final String authorizationHeader = httpRequest.getHeader("Authorization");
    if (authorizationHeader != null && authorizationHeader.startsWith("Basic")) {
      final String tokenString = authorizationHeader.trim().substring(authorizationHeader.indexOf(" ") + 1);
      final String credentials = new String(Base64.decodeBase64(tokenString), StandardCharsets.UTF_8);
      final String[] login = credentials.split(":", 2);
      final String username = login[0];
      final String password = login[1];

      if (username != null && password != null) {
        logger.debug("Authenticating {}", username);
        if (authenticate(username, password)) {
          final Principal userPrincipal = new BasicUserPrincipal(username);
          logger.debug("User {} is authenticated", username);
          final Set<String> roles = userRoles.get(username);
          logger.info("Authenticated user {} has roles: {}", username, roles);
          chain.doFilter(requestWithUserAndRoles(httpRequest, userPrincipal, roles), response);
        } else {
          logger.error("User {} is NOT authenticated", username);
          ((HttpServletResponse) response).sendError(SC_UNAUTHORIZED);
        }
      } else {
        logger.error("Invalid basic authentication token");
        ((HttpServletResponse) response).sendError(SC_BAD_REQUEST);
      }
    } else {
      // no "Authorization: Basic ..." header, go to next filter
      chain.doFilter(request, response);
    }
  }

  private boolean authenticate(final String username, final String password) {
    return userPasswords.containsKey(username) && userPasswords.get(username).equals(password);
  }

  private HttpServletRequest requestWithUserAndRoles(HttpServletRequest request, Principal userPrincipal, Set<String> roles) {
    return new ProvideRolesRequestWrapper(new ProvideUserPrincipalRequestWrapper(request, userPrincipal), roles);
  }

  @Override
  public void destroy() {

  }

  public String getCredentialsFile() {
    return credentialsFile;
  }

  public void setCredentialsFile(String credentialsFile) {
    this.credentialsFile = credentialsFile;
  }
}
