package edu.umd.lib.fcrepo.filters;

import edu.umd.lib.fcrepo.requests.ProvideRequestURLRequestWrapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
