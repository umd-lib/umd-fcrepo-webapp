package edu.umd.lib.fcrepo.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.lib.fcrepo.filters.ipmanager.Check;
import edu.umd.lib.fcrepo.filters.ipmanager.CheckResponse;
import edu.umd.lib.fcrepo.requests.HeaderRequestWrapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Checks a user's IP address against an IP Manager service to determine what
 * groups (if any) the requesting IP address belongs to. These groups are added
 * to the request as additional security principals in an HTTP header.
 *
 * <p>This filter can be configured by using an org.springframework.web.filter.DelegatingFilterProxy
 * and a corresponding bean in your Spring configuration.</p>
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
 *   &lt;property name="ipManagerServiceUrl" value="https://ipmanager.lib.umd.edu"/>
 * &lt;/bean>
 * </pre>
 */
public class IpMapperFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(IpMapperFilter.class);

  private final ObjectMapper mapper = new ObjectMapper();

  private String ipManagerServiceUrl;

  /**
   * The header name we want to check for.
   */
  private String headerName;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    headerName = filterConfig.getInitParameter("headerName");
    if (headerName == null) {
      logger.error("The 'headerName' parameter has not been specified.");
      throw new ServletException("'headerName' parameter has not been set.");
    }
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
      logger.warn("Could not find valid IP address for user. Skipping IP mapping.");
      chain.doFilter(request, response);
      return;
    }

    final List<String> groups = getGroups(userIp);

    if (!groups.isEmpty()) {
      final String headerValue = String.join(",", groups);
      request.addHeader(headerName, headerValue);
      logger.info("IP Mapper added: '{}' to header '{}' for IP {}", headerValue, headerName, userIp);
    }

    // continue the filter chain
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Nothing to destroy
  }

  private List<String> getGroups(final String ip) {
    try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
      final String url = ipManagerServiceUrl + "/check?ip=" + ip;
      final ClassicHttpRequest httpGet = ClassicRequestBuilder.get(url).build();
      final List<String> groupURIs = httpClient.execute(httpGet, response -> {
        final List<String> values = new ArrayList<>();
        if (response.getCode() >= 400) {
          // received an error response: log and return an empy list
          logger.error("Unable to connect to {}: {} {}", url, response.getCode(), response.getReasonPhrase());
          logger.warn("Returning empty list for IP groups");
          return Collections.emptyList();
        }
        final HttpEntity entity = response.getEntity();
        final CheckResponse checkResponse = mapper.readValue(entity.getContent(), CheckResponse.class);
        for (Check check : checkResponse.getChecks()) {
          if (check.isContained()) {
            final String groupURI = check.getGroup().getId();
            logger.debug("Adding {} to list of IP groups", groupURI);
            values.add(groupURI);
          }
        }
        // ensure the response body is fully consumed
        EntityUtils.consume(entity);

        return values;
      });
      Collections.sort(groupURIs);
      return groupURIs;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
  public String getUserIp(HttpServletRequest request) {
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

  public String getIpManagerServiceUrl() {
    return ipManagerServiceUrl;
  }

  public void setIpManagerServiceUrl(String ipManagerServiceUrl) {
    this.ipManagerServiceUrl = ipManagerServiceUrl;
  }
}
