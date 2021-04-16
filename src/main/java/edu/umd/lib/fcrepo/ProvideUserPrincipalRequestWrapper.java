package edu.umd.lib.fcrepo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.security.Principal;

public class ProvideUserPrincipalRequestWrapper extends HttpServletRequestWrapper {
  private final Principal userPrincipal;

  /**
   * Constructs a request object wrapping the given request.
   *
   * @param request request object
   * @param userPrincipal user principal to return for getUserPrincipal()
   * @throws IllegalArgumentException if the request is null
   */
  public ProvideUserPrincipalRequestWrapper(HttpServletRequest request, Principal userPrincipal) {
    super(request);
    this.userPrincipal = userPrincipal;
  }

  @Override
  public String getRemoteUser() {
    return userPrincipal.getName();
  }

  @Override
  public Principal getUserPrincipal() {
    return userPrincipal;
  }
}
