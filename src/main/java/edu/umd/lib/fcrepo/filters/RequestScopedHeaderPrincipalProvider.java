package edu.umd.lib.fcrepo.filters;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.fcrepo.auth.common.HttpHeaderPrincipalProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replacement for fcrepo's HttpHeaderPrincipalProvider (the "headerProvider" bean in
 * the Shiro filter chain), used to add the IP Mapper group principals to the current
 * Shiro subject.
 *
 * <p>The stock fcrepo implementation has two problems:</p>
 * <ol>
 *   <li>It puts all the principals into a HashSet before calling Subject.runAs(), so
 *   the "primary" principal of the run-as identity is effectively random. It is often
 *   the "fedoraAdmin" role principal instead of the user, and fcrepo then records
 *   "fedoraAdmin" as the "created by"/"modified by" user.</li>
 *   <li>Shiro stores run-as identities in the HTTP session, and fcrepo never releases
 *   them. On the next request, ServletContainerAuthFilter reads the user principal from
 *   that stale identity, so the wrong user is logged in from then on.</li>
 * </ol>
 *
 * <p>This version keeps the authenticated user as the primary principal, and releases
 * all run-as identities when the request is finished.</p>
 */
public class RequestScopedHeaderPrincipalProvider extends HttpHeaderPrincipalProvider {
  private static final Logger logger = LoggerFactory.getLogger(RequestScopedHeaderPrincipalProvider.class);

  // same realm name used by fcrepo's AbstractPrincipalProvider
  private static final String REALM_NAME = "org.fcrepo.auth.webac.WebACAuthorizingRealm";

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    final Subject currentUser = SecurityUtils.getSubject();

    // clear any run-as identity left in the session by an earlier request
    releaseAllRunAs(currentUser);

    try {
      final Set<Principal> headerPrincipals = getPrincipals((HttpServletRequest) request);
      final PrincipalCollection principals = currentUser.getPrincipals();
      if (headerPrincipals.isEmpty()) {
        logger.debug("No header principals found in the request");
      } else if (principals == null || principals.isEmpty()) {
        // Shiro does not allow runAs() for a subject with no identity
        logger.warn("Ignoring header principals {}; the current subject has no identity", headerPrincipals);
      } else {
        // copying the existing collection keeps the user principal first, so it stays
        // the primary principal (i.e., the one returned by getUserPrincipal())
        final SimplePrincipalCollection combined = new SimplePrincipalCollection(principals);
        combined.addAll(headerPrincipals, REALM_NAME);
        logger.debug("Adding header principals {} for user {}", headerPrincipals, combined.getPrimaryPrincipal());
        currentUser.runAs(combined);
      }
      chain.doFilter(request, response);
    } finally {
      // also releases identities pushed by later filters (e.g., fcrepo's delegatedPrincipalProvider)
      releaseAllRunAs(currentUser);
    }
  }

  private static void releaseAllRunAs(final Subject subject) {
    while (subject.isRunAs()) {
      subject.releaseRunAs();
    }
  }
}
