package edu.umd.lib.fcrepo.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletException;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

public class IpMapperFilterTest {
  private MockHttpServletRequest mockRequest = null;
  private MockHttpServletResponse mockResponse = null;
  private MockFilterChain mockChain = null;
  private MockWebServer mockWebServer = null;
  private MockResponse successContains4 = null;
  
  private IpMapperFilter ipMapperFilter = null;

  private static final String HEADER_NAME = "TEST-IPMAPPER-HEADER";
  private static final String CONTAINS_4_MATCHES = "http://ipmanager.lib.umd.edu/groups/films,http://ipmanager.lib.umd.edu/groups/prange,http://ipmanager.lib.umd.edu/groups/um,http://ipmanager.lib.umd.edu/groups/world";
  private static final String XFF_IP = "192.163.40.1";
  private static final String REMOTE_ADDR_IP = "193.168.39.1";
  private static final String LOCALHOST_IP = "127.0.0.1";

  private String getBodyFromResource(final String resourceName) {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
      assertNotNull(stream);
      return new String(stream.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private MockResponse getJsonOK(final String resourceName) {
    return new MockResponse.Builder()
        .code(200)
        .setHeader("Content-Type", "application/json")
        .body(getBodyFromResource(resourceName))
        .build();
  }

  @Before
  public void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    ipMapperFilter = new IpMapperFilter();
    ipMapperFilter.setHeaderName(HEADER_NAME);
    ipMapperFilter.setIpManagerServiceUrl(mockWebServer.url("/check").toString());
    
    mockRequest = new MockHttpServletRequest();
    mockResponse = new MockHttpServletResponse();
    mockChain = new MockFilterChain();

    successContains4 = getJsonOK("ip-manager-results/success-contains-4.json");
  }

  @Test
  public void testSuccess() throws ServletException, IOException {
    mockWebServer.enqueue(successContains4);

    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    final HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();

    assertNotNull(finalRequest);
    assertNotNull(finalRequest.getHeader(HEADER_NAME));
    assertEquals(4, finalRequest.getHeader(HEADER_NAME).split(",").length);
  }

  @Test
  public void testSuccessNoMatches() throws ServletException, IOException {
    mockWebServer.enqueue(getJsonOK("ip-manager-results/success-contains-0.json"));

    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    final HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();

    assertNotNull(finalRequest);
    assertNull(finalRequest.getHeader(HEADER_NAME));
  }

  @Test
  public void testIpManagerServiceFails() throws ServletException, IOException {
    mockWebServer.enqueue(new MockResponse.Builder().code(404).build());

    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    final HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();

    // if the IP Manager service fails, the filter should just omit the header
    assertNotNull(finalRequest);
    assertNull(finalRequest.getHeader(HEADER_NAME));
  }

  @Test
  public void testHeaderNamePassedInToRequestShouldBeStripped() throws Exception {
    mockWebServer.enqueue(successContains4);

    // This test verifies that if a HEADER_NAME header is given in the request,
    // that it is stripped out. This is to prevent spoofing.
    mockRequest.addHeader(HEADER_NAME, "campus");
    
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNotNull(finalRequest);
    assertEquals(CONTAINS_4_MATCHES, finalRequest.getHeader(HEADER_NAME));
  } 
  
  @Test
  public void testNoRemoteOrXFFAddrDefaultsToLocalhost() {
    assertEquals(LOCALHOST_IP, ipMapperFilter.getUserIp(mockRequest));
  }
  
  @Test
  public void testXFF() {
    mockRequest.addHeader("X-FORWARDED-FOR", XFF_IP);

    assertEquals(XFF_IP, ipMapperFilter.getUserIp(mockRequest));
  }
  
  @Test
  public void testPreferXFFtoRemoteAddr() {
    mockRequest.addHeader("X-FORWARDED-FOR", XFF_IP);
    mockRequest.setRemoteAddr(REMOTE_ADDR_IP);

    assertEquals(XFF_IP, ipMapperFilter.getUserIp(mockRequest));
  }
  
  @Test
  public void testIpFromRemoteAddr() {
    mockWebServer.enqueue(successContains4);

    mockRequest.setRemoteAddr(REMOTE_ADDR_IP);
    assertEquals(REMOTE_ADDR_IP, ipMapperFilter.getUserIp(mockRequest));
  }
}