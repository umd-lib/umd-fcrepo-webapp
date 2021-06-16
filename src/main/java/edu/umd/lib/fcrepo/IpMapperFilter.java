package edu.umd.lib.fcrepo;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
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
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Checks a user's IP address against a properties file containing a list of
 * categories, each with one or more IP blocks. If the user's IP is found
 * in one or more of these categories, the filter inserts a header, which can
 * then be read by other applications to determine access rights.
 *
 * <p>The properties file should follow the following format:
 *
 * <pre>
 * categoryName=0.0.0.0/32,0.0.0.0/16
 * </pre>
 *
 * <p>The header value will be a comma-separated list of all of the categories
 * where the user's IP address matched the category's IP address(es).</p>
 *
 * <p>This filter can either be configured directly in a web.xml file, or
 * indirectly by using an org.springframework.web.filter.DelegatingFilterProxy
 * and a corresponding bean in your Spring configuration.</p>
 *
 * <p>Direct configuration:</p>
 *
 * <pre>
 * &lt;filter>
 *   &lt;filter-name>ip-mapper-filter&lt;/filter-name>
 *   &lt;filter-class>edu.umd.lib.fcrepo.IpMapperFilter&lt;/filter-class>
 *   &lt;init-param>
 *     &lt;param-name>headerName&lt;/param-name>
 *     &lt;param-value>Some-Header-Name&lt;/param-value>
 *   &lt;/init-param>
 *   &lt;init-param>
 *     &lt;param-name>mappingFile&lt;/param-name>
 *     &lt;param-value>/path/to/ip-mapping.properties&lt;/param-value>
 *   &lt;/init-param>
 * &lt;/filter>
 * </pre>
 *
 * <p>Via a DelegatingFilterProxy:</p>
 *
 * <pre>
 * &lt;!-- web.xml -->
 * &lt;filter>
 *   &lt;filter-name>ip-mapper-filter&lt;/filter-name>
 *   &lt;filter-class>org.springframework.web.filter.DelegatingFilterProxy&lt;/filter-class>
 *   &lt;init-param>
 *     &lt;param-name>targetBean&lt;/param-name>
 *     &lt;param-value>ipMapperFilter&lt;/param-value>
 *   &lt;/init-param>
 * &lt;/filter>
 *
 * &lt;!-- Spring XML -->
 * &lt;bean id="ipMapperFilter" class="edu.umd.lib.fcrepo.IpMapperFilter">
 *   &lt;property name="headerName" value="Some-Header-Name"/>
 *   &lt;property name="mappingFile" value="/path/to/ip-mapping.properties"/>
 * &lt;/bean>
 * </pre>
 */
public class IpMapperFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(IpMapperFilter.class);

  /**
   * The header name we want to check for.
   */
  private String headerName;

  /**
   * Name of the properties file containing the mappings.
   */
  private String mappingFile;

  /**
   * List of IpAddressMatcher objects (representing allowed IP ranges in a category),
   * indexed by category
   */
  private Map<String, List<IpAddressMatcher>> ipCategories = new HashMap<>();

  public IpMapperFilter() {}

  @PostConstruct
  public void initializeMapping() throws ServletException {
    // Retrieve list of Properties from the mapping file
    Properties mappingProperties = new Properties();
    try (FileReader in = new FileReader(mappingFile)) {
      mappingProperties.load(in);
    } catch (IOException ioe) {
      logger.error("An I/O exception occurred reading the mapping file at: '" + mappingFile + "'", ioe);
      throw new ServletException("An I/O exception occurred reading the mapping file at: '" + mappingFile + "'",
          ioe);
    }

    // Convert mapping properties into map of IP categories
    ipCategories = initIpCategories(mappingProperties);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    headerName = filterConfig.getInitParameter("headerName");
    if (headerName == null) {
      logger.error("The 'headerName' parameter has not been specified.");
      throw new ServletException("'headerName' parameter has not been set.");
    }

    mappingFile = filterConfig.getInitParameter("mappingFile");
    if (mappingFile == null) {
      logger.error("The 'mappingFile' parameter has not been specified.");
      throw new ServletException("The 'mappingFile' parameter has not been specified.");
    }

    initializeMapping();
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
    final Set<Object> keys = mappingProperties.keySet();
    for (final Object key : keys) {
      final String keyStr = (String) key;
      final String value = mappingProperties.getProperty(keyStr);

      final String[] subnets = value.split(",");
      final List<IpAddressMatcher> subnetsList = new ArrayList<>();
      for (final String subnet : subnets) {
        try {
          final IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(subnet);
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

    final HeaderRequestWrapper request = new HeaderRequestWrapper((HttpServletRequest) servletRequest);

    // Check for existing header. This is necessary to prevent spoofing.
    // If the header already exists, strip and reevaluate.
    final String existingHeader = request.getHeader(headerName);
    if (existingHeader != null) {
      logger.warn("Header: '" + existingHeader + "' found before IP mapper eval!");
      request.removeHeader(headerName);
    }

    final String userIp = getUserIp(request);

    if (userIp == null) {
      logger.debug("Could not find valid IP address for user. Skipping IP mapping.");
      chain.doFilter(request, response);
      return;
    }

    final List<String> matchingCategories = findMatchingCategories(userIp);

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
    for (final String key : ipCategories.keySet()) {
      final List<IpAddressMatcher> subnets = ipCategories.get(key);
      for (final IpAddressMatcher subnet : subnets) {
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

  public String getHeaderName() {
    return headerName;
  }

  public void setHeaderName(String headerName) {
    this.headerName = headerName;
  }

  public String getMappingFile() {
    return mappingFile;
  }

  public void setMappingFile(String mappingFile) {
    this.mappingFile = mappingFile;
  }
}

/**
 * Wrapper class allowing manipulation of headers
 */
class HeaderRequestWrapper extends HttpServletRequestWrapper {
  /**
   * A Map of headers for the request
   */
  private final Map<String, List<String>> headers = new LinkedCaseInsensitiveMap<>();

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
   * @param headerName
   *          the name of the header to add
   * @param value
   *          the value of the header
   */
  public void addHeader(String headerName, String value) {
    headers.put(headerName, Collections.singletonList(value));
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