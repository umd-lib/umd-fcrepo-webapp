package edu.umd.lib.fcrepo.requests;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class allowing manipulation of headers
 */
public class HeaderRequestWrapper extends HttpServletRequestWrapper {
  /**
   * A Map of headers for the request
   */
  private final Map<String, List<String>> headers = new LinkedCaseInsensitiveMap<>();

  /**
   * Wraps the given HttpServletRequest
   *
   * @param request the HttpServletRequest to wrap
   */
  public HeaderRequestWrapper(HttpServletRequest request) {
    super(request);

    // Populate the "headers" map with all the existing header keys/values.
    Enumeration<String> headerNameEnums = request.getHeaderNames();
    if (headerNameEnums != null) {
      ArrayList<String> headerNames = Collections.list(request.getHeaderNames());
      for (String headerName : headerNames) {
        headers.put(headerName, Collections.list(request.getHeaders(headerName)));
      }
    }
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    final Set<String> headerKeys = new HashSet<>();
    for (final String headerName : headers.keySet()) {
      headerKeys.add(headerName.toLowerCase(Locale.ROOT));
    }
    return Collections.enumeration(headerKeys);
  }

  @Override
  public Enumeration<String> getHeaders(String headerName) {
    List<String> values = headers.get(headerName);
    if (values == null || values.isEmpty()) {
      return Collections.emptyEnumeration();
    }
    return Collections.enumeration(values);
  }

  @Override
  public String getHeader(String headerName) {
    final Enumeration<String> headerValues = getHeaders(headerName);
    if (headerValues != null && headerValues.hasMoreElements()) {
      return headerValues.nextElement();
    }
    return null;
  }

  /**
   * Adds a header with the given name, replacing the header if
   * it already exists.
   *
   * @param headerName the name of the header to add
   * @param value      the value of the header
   */
  public void addHeader(String headerName, String value) {
    headers.put(headerName, Collections.singletonList(value));
  }

  /**
   * Removes the header (if present) with the given name.
   *
   * @param headerName the name of the header to remove.
   */
  public void removeHeader(String headerName) {
    headers.remove(headerName);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("headers", headers).append("request", this.getRequest()).toString();
  }
}
