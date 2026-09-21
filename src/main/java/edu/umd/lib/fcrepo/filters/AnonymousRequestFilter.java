package edu.umd.lib.fcrepo.filters;

import edu.umd.lib.fcrepo.requests.ProvideRolesRequestWrapper;
import edu.umd.lib.fcrepo.requests.ProvideUserPrincipalRequestWrapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.security.Principal;

import static edu.umd.lib.fcrepo.services.LdapRoleLookupService.USER_ROLE;

public class AnonymousRequestFilter implements Filter {
  public static class AnonymousUserPrincipal implements Principal {
    final public static String FOAF_AGENT = "http://xmlns.com/foaf/0.1/Agent";

    @Override
    public String getName() {
      return FOAF_AGENT;
    }
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    if (httpServletRequest.getUserPrincipal() == null) {
      // if there is no user principal in the request (i.e., anonymous)
      // add the foaf:Agent user as the explicit user principal in the
      // request
      filterChain.doFilter(
        new ProvideRolesRequestWrapper(
          new ProvideUserPrincipalRequestWrapper(httpServletRequest, new AnonymousUserPrincipal()),
          USER_ROLE
        ),
        servletResponse
      );
    } else {
      // if there is already a user principal, do nothing
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }
}
