package edu.umd.lib.fcrepo.filters;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.umd.lib.fcrepo.requests.HeaderRequestWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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
 * <p>The properties file should follow the following format:
 *
 * <pre>
 * categoryName=0.0.0.0/32,0.0.0.0/16
 * </pre>
 *
 * <p>The header value will be a comma-separated list of all the categories
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
 *   &lt;filter-class>edu.umd.lib.fcrepo.filters.IpMapperFilter&lt;/filter-class>
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
 * &lt;bean id="ipMapperFilter" class="edu.umd.lib.fcrepo.filters.IpMapperFilter">
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

  @PostConstruct
  public void initializeMapping() throws ServletException {
    // Retrieve list of Properties from the mapping file
    Properties mappingProperties = new Properties();
    try (FileReader in = new FileReader(mappingFile)) {
      mappingProperties.load(in);
    } catch (IOException ioe) {
      logger.error("An I/O exception occurred reading the mapping file at: '{}'", mappingFile, ioe);
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
          logger.warn("Could not parse '{}' value in '{}' property", subnet, key, iae);
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
      logger.warn("Header: '{}' found before IP mapper eval!", existingHeader);
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
      final String headerValue = String.join(",", matchingCategories);
      request.addHeader(headerName, headerValue);
      logger.info("IP Mapper added: '{}' to header '{}' for IP {}", headerValue, headerName, userIp);
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
   * <p>For now, we are assuming only IPV4. It's possible we might get a
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
