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

public class FedoraRolesFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(FedoraRolesFilter.class);

  public static final String ADMIN_ROLE = "fedoraAdmin";

  public static final String USER_ROLE = "fedoraUser";

  private ConnectionFactory cf;

  private String memberAttribute;

  private String adminGroup;

  private String userGroup;

  private SearchExecutor searchExecutor;

  @Override
  public void init(FilterConfig filterConfig) {
    final String ldapURL = filterConfig.getInitParameter("ldapURL");
    final String baseDN = filterConfig.getInitParameter("baseDN");
    final String bindDN = filterConfig.getInitParameter("bindDN");
    final String bindPassword = filterConfig.getInitParameter("bindPassword");

    memberAttribute = filterConfig.getInitParameter("memberAttribute");
    adminGroup = filterConfig.getInitParameter("adminGroup");
    userGroup = filterConfig.getInitParameter("userGroup");

    final ConnectionConfig connConfig = new ConnectionConfig(ldapURL);
    connConfig.setUseStartTLS(true);
    connConfig.setConnectionInitializer(new BindConnectionInitializer(bindDN, new Credential(bindPassword)));
    cf = new DefaultConnectionFactory(connConfig);
    searchExecutor = new SearchExecutor();
    searchExecutor.setBaseDn(baseDN);

    logger.info("Configured LDAP for user role lookup");
    logger.info("LDAP URL: {} Base DN: {} Bind DN: {}", ldapURL, baseDN, bindDN);
    logger.debug("Group {} => Role {}", adminGroup, ADMIN_ROLE);
    logger.debug("Group {} => Role {}", userGroup, USER_ROLE);
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

      final String role = getRole(getUserEntry(userName));
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
   * Look up the given userName in the configured LDAP directory, and return the
   * matching entry (if found).
   *
   * @param userName this should match a single uid in the directory
   * @return matching entry or null
   */
  LdapEntry getUserEntry(final String userName) {
    try {
      final String uidFilter = "uid=" + userName;
      final SearchResult result = searchExecutor.search(cf, uidFilter, memberAttribute).getResult();
      return result.getEntry();
    } catch (LdapException e) {
      logger.error("LDAP Exception: " + e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * If the userEntry is a member of either the admin group or the user group,
   * return the appropriate role string ("fedoraAdmin" or "fedoraUser", respectively).
   * If the userEntry is null, or has neither membership relation, return null.
   *
   * @param userEntry LDAP entry for a user
   * @return role name string: "fedoraAdmin" or "fedoraUser"
   */
  String getRole(final LdapEntry userEntry) {
    if (userEntry == null) {
      return null;
    }

    final LdapAttribute memberOfAttr = userEntry.getAttribute(memberAttribute);
    final Collection<String> memberships = memberOfAttr.getStringValues();
    if (memberships.contains(adminGroup)) {
      return ADMIN_ROLE;
    } else if (memberships.contains(userGroup)){
      return USER_ROLE;
    }
    return null;
  }
}
