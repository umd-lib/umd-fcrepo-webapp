package edu.umd.lib.fcrepo;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchExecutor;
import org.ldaptive.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class FedoraRolesFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(FedoraRolesFilter.class);

  private ConnectionFactory cf;

  private String memberAttribute;

  private String adminGroup;

  private String userGroup;

  private SearchExecutor searchExecutor;

  private LdapRoleLookupService ldapRoleLookupService;

  @Override
  public void init(FilterConfig filterConfig) {
    ldapRoleLookupService = WebApplicationContextUtils
        .getRequiredWebApplicationContext(filterConfig.getServletContext())
        .getBean(LdapRoleLookupService.class);
  }

  /**
   * If the user is authenticated, use their Grouper groups to determine which Fedora roles to
   * place them in.
   *
   * @param request the current request object
   * @param response the current response object
   * @param chain the current filter chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (httpRequest.getUserPrincipal() != null) {
      final AttributePrincipal principal = (AttributePrincipal) httpRequest.getUserPrincipal();
      logger.info("User principal found in request: {}", principal);
      final String userName = principal.toString();

      final String role = getRole(httpRequest, userName);

      if (role != null) {
        logger.info("User {} has role {}", userName, role);
        // wrap the request so that it will answer "true" for the correct roles
        chain.doFilter(new ProvideRoleRequestWrapper(httpRequest, role), response);
      } else {
        logger.debug("No fedora roles found for user {}", userName);
        chain.doFilter(httpRequest, response);
      }
    } else {
      logger.debug("No user principal in request");
      chain.doFilter(httpRequest, response);
    }
  }

  @Override
  public void destroy() {

  }

  /**
   * Returns the role for the user, or null if the role cannot be found.
   * 
   * Implementation: This method first checks the HttpRequest session for the
   * role. If a role is not found in the session, it performs an LDAP search
   * using the given userName. If a role is found, it is stored in the
   * session and returned.
   * 
   * @param httpRequest the current HttpRequest object
   * @param userName the login id of the user
   * @return a role name string: "fedoraAdmin", "fedoraUser", or null
   */
  private String getRole(final HttpServletRequest httpRequest, final String userName) {
    final String SESSION_ROLE_KEY = "fedoraUserRole";
    
    // Attempt to retrieve role from session
    final HttpSession session = httpRequest.getSession(false);
    if ((session != null) && (session.getAttribute(SESSION_ROLE_KEY) != null)) {
       final String role = session.getAttribute(SESSION_ROLE_KEY).toString();
       logger.debug("User {} has role {} from session", userName, role);
       return role;
    }
    
    // Retrieve role from LDAP 
    logger.debug("Attempting to retrieve role for {} from LDAP", userName);
    final String role = ldapRoleLookupService.getRole(userName);
    logger.debug("User {} has role {} from LDAP", userName, role);
    
    // Store role in session
    if (role != null && session != null) {
      session.setAttribute(SESSION_ROLE_KEY, role);
    }
    return role;
  }
}
