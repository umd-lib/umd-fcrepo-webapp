package edu.umd.lib.fcrepo;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Checks a user's IP address against a properties file containing a list of
 * categories, each with one or more IP blocks. If the user's IP is found
 * in one or more of these categories, the filter inserts a header, which can
 * then be read by other applications to determine access rights.
 *
 * The properties file should follow the following format:
 *
 * categoryName=0.0.0.0/32,0.0.0.0/16
 *
 * The header value will be a comma-separated list of all of the categories
 * where the user's IP address matched the cateogory's IP address(es).
 */
public class IpMapperFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(IpMapperFilter.class);

  /**
   * The header name we want to check for.
   */
  private String headerName = null;

  /**
   * List of IpAdressMatcher objects (representing allowed IP ranges in a category),
   * indexed by category
   */
  private Map<String, List<IpAddressMatcher>> ipCategories = new HashMap<>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    headerName = filterConfig.getInitParameter("headerName");
    if (headerName == null) {
      logger.error("The 'headerName' parameter has not been specified.");
      throw new ServletException("'headerName' parameter has not been set.");
    }

    String mappingFileName = filterConfig.getInitParameter("mappingFile");
    if (mappingFileName == null) {
      logger.error("The 'mappingFile' parameter has not been specified.");
      throw new ServletException("The 'mappingFile' parameter has not been specified.");
    }

    // Retrieve list of Properties from the mapping file
    Properties mappingProperties = new Properties();
    try (FileReader in = new FileReader(mappingFileName)) {
      mappingProperties.load(in);
    } catch (IOException ioe) {
      logger.error("An I/O exception occurred reading the mapping file at: '" + mappingFileName + "'", ioe);
      throw new ServletException("An I/O exception occurred reading the mapping file at: '" + mappingFileName + "'",
          ioe);
    }

    // Convert mapping properties into map of IP categories
    ipCategories = initIpCategories(mappingProperties);
  }

  /**
   * Converts the given Properties object (which is assumed to contain of
   * String keys and values), into a Map of IP address ranges, indexed
   * by category.
   *
   * @param mappingProperties
   *          a Properties object containing the allowed IP ranges
   * @return a Map of IP address ranges, indexed by category.
   */
  private Map<String, List<IpAddressMatcher>> initIpCategories(Properties mappingProperties) {
    Map<String, List<IpAddressMatcher>> allowedIps = new HashMap<>();

    // Initialize allowed IP ranges map
    Set<Object> keys = mappingProperties.keySet();
    for (Object key : keys) {
      String keyStr = (String) key;
      String value = mappingProperties.getProperty(keyStr);

      String[] subnets = value.split(",");
      List<IpAddressMatcher> subnetsList = new ArrayList<>();
      for (String subnet : subnets) {
        try {
          IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(subnet);
          subnetsList.add(ipAddressMatcher);
        } catch (IllegalArgumentException iae) {
          logger.warn("Could not parse '" + subnet + "' value in '" + key + "' property", iae);
        }
      }

      allowedIps.put(keyStr, subnetsList);
    }

    return allowedIps;
  }

  /**
   * Determines which categories (if any) match a user's IP address, and adds
   * them to the request headers using the configured header name.
   * 
   * @param servletRequest
   *          the ServletRequest (assumed to be an HttpServletRequest)
   * @param response
   *          the ServletResponse object
   * @param chain
   *          the FilterChain to use to continue processing
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HeaderRequestWrapper request = new HeaderRequestWrapper((HttpServletRequest) servletRequest);

    // Check for existing header. This is necessary to prevent spoofing.
    // If the header already exists, strip and reevaluate.
    String existingHeader = request.getHeader(headerName);
    if (existingHeader != null) {
      logger.warn("Header: '" + existingHeader + "' found before IP mapper eval!");
      request.removeHeader(headerName);
    }

    String userIp = getUserIp(request);

    if (userIp == null) {
      logger.debug("Could not find valid IP address for user. Skipping IP mapping.");
      chain.doFilter(request, response);
      return;
    }

    List<String> matchingCategories = findMatchingCategories(userIp);

    if (!matchingCategories.isEmpty()) {
      final String headerValue = StringUtils.join(matchingCategories, ",");
      request.addHeader(headerName, headerValue);
      logger.info("IP Mapper added: '" + headerValue + "' to header '" + headerName + "' for IP " + userIp);
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Nothing to destroy
  }

  /**
   * Finds the categories for the user's IP address
   * in the HttpServletRequest
   * 
   * @param userIp
   *          the IP address of the user.
   * @return a (possibly empty) list of categories that match the user's IP address.
   */
  private List<String> findMatchingCategories(String userIp) {
    List<String> matchingCategories = new ArrayList<>();

    if (userIp == null) {
      logger.warn("findMatchingCategories called with null user IP address.");
      return matchingCategories;
    }

    // Find matching categories, if any
    for (String key : ipCategories.keySet()) {
      List<IpAddressMatcher> subnets = ipCategories.get(key);
      for (IpAddressMatcher subnet : subnets) {
        if (subnet.matches(userIp)) {
          matchingCategories.add(key);
        }
      }
    }

    // Sorting alphabetically to simplify testing
    Collections.sort(matchingCategories);

    return matchingCategories;
  }

  /**
   * Get the user's IP.
   *
   * For now, we are assuming only IPV4. It's possible we might get a
   * comma-separated list of IPs, in which case, we should split prior to
   * evaluation. Real IP should always come first.
   *
   * @param request
   *          incoming HttpServletRequest object
   * @return the user's IP address, or null
   */
  private String getUserIp(HttpServletRequest request) {
    String userIp = request.getHeader("X-FORWARDED-FOR");
    if (userIp == null) {
      userIp = request.getRemoteAddr();
    }

    String[] userIps = userIp.split(",");
    if (userIps[0] != null) {
      userIp = userIps[0].trim();
    } else {
      userIp = null;
    }

    if (InetAddressValidator.getInstance().isValidInet4Address(userIp)) {
      return userIp;
    }
    return null;
  }
}

/**
 * Wrapper class allowing manipulation of headers
 */
class HeaderRequestWrapper extends HttpServletRequestWrapper {
  /**
   * A Map of headers for the request
   */
  private Map<String, String> headers = new HashMap<>();

  /**
   * Wraps the given HttpServletRequest
   * 
   * @param request
   *          the HttpServletRequest to wrap
   */
  public HeaderRequestWrapper(HttpServletRequest request) {
    super(request);

    // Populate the "headers" map with all the existing header keys/values.
    Enumeration<String> headerNameEnums = request.getHeaderNames();
    if (headerNameEnums != null) {
      ArrayList<String> headerNames = Collections.list(request.getHeaderNames());
      for (String headerName : headerNames) {
        headers.put(headerName, request.getHeader(headerName));
      }
    }
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Set<String> headerKeys = headers.keySet();
    return java.util.Collections.enumeration(headerKeys);
  }

  @Override
  public String getHeader(String headerName) {
    return headers.get(headerName);
  }

  /**
   * Adds a header with the given name, replacing the header if
   * it already exists.
   * 
   * @param headerName
   *          the name of the header to add
   * @param value
   *          the value of the header
   */
  public void addHeader(String headerName, String value) {
    headers.put(headerName, value);
  }

  /**
   * Removes the header (if present) with the given name.
   * 
   * @param headerName
   *          the name of the header to remove.
   */
  public void removeHeader(String headerName) {
    headers.remove(headerName);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("headers", headers).append("request", this.getRequest()).toString();
  }
}