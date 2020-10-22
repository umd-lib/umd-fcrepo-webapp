package edu.umd.lib.fcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class ReverseProxyFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(ReverseProxyFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;

    final String forwardedHost = httpRequest.getHeader("X-Forwarded-Host");
    final String forwardedProto = httpRequest.getHeader("X-Forwarded-Proto");

    logger.debug("Forwarded host: {}", forwardedHost);
    logger.debug("Forwarded protocol: {}", forwardedProto);

    // if either the X-Forwarded-Host or -Proto are present, wrap the servlet request
    // otherwise, just go on to the next filter with no modification to the request
    if (forwardedHost != null || forwardedProto != null) {
      logger.debug("Wrapping request to use forwarded host {} and protocol {}", forwardedHost, forwardedProto);
      chain.doFilter(new ProvideRequestURLRequestWrapper(httpRequest, forwardedHost, forwardedProto), response);
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {

  }
}

class ProvideRequestURLRequestWrapper extends HttpServletRequestWrapper {
  private final String hostname;

  private final String protocol;

  /**
   * Constructs a request object wrapping the given request.
   *
   * @param request current request object
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
