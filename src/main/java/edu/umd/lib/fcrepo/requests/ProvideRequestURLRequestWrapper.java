package edu.umd.lib.fcrepo.requests;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class ProvideRequestURLRequestWrapper extends HttpServletRequestWrapper {
  private final String hostname;

  private final String protocol;

  /**
   * Constructs a request object wrapping the given request.
   *
   * @param request  current request object
   * @param hostname the forwarded hostname
   * @param protocol the forwarded protocol
   * @throws IllegalArgumentException if the request is null
   */
  public ProvideRequestURLRequestWrapper(final HttpServletRequest request, final String hostname, final String protocol) {
    super(request);
    this.hostname = (hostname != null) ? hostname : getServerName();
    this.protocol = (protocol != null) ? protocol : "http";
  }

  @Override
  public StringBuffer getRequestURL() {
    // assume default ports; omit them from the request URL
    return new StringBuffer(protocol + "://" + hostname + getRequestURI());
  }
}
