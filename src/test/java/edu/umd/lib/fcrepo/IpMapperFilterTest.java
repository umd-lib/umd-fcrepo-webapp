package edu.umd.lib.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class IpMapperFilterTest {
  private MockFilterConfig mockFilterConfig = null;
  private MockHttpServletRequest mockRequest = null;
  private MockHttpServletResponse mockResponse = null;
  private MockFilterChain mockChain = null;
  
  private IpMapperFilter ipMapperFilter = null;
  private static final String HEADER_NAME = "TEST-IPMAPPER-HEADER";

  @Before
  public void setUp() throws Exception {
    mockFilterConfig = new MockFilterConfig();
    mockFilterConfig.addInitParameter("headerName", HEADER_NAME);
    
    mockFilterConfig.addInitParameter("mappingFile", "src/test/resources/test-ip-mapping.properties");   
    
    ipMapperFilter = new IpMapperFilter();
    ipMapperFilter.init(mockFilterConfig);
    
    mockRequest = new MockHttpServletRequest();
    mockResponse = new MockHttpServletResponse();
    mockChain = new MockFilterChain();
  }

  @Test(expected = ServletException.class)
  public void testHeaderNameInitParameterIsNotSet() throws Exception {
    IpMapperFilter ipMapperFilter = new IpMapperFilter();
    ipMapperFilter.init(new MockFilterConfig());
  }

  @Test(expected = ServletException.class)
  public void testMappingFileInitParameterIsNotSet() throws Exception {
    IpMapperFilter ipMapperFilter = new IpMapperFilter();
    
    mockFilterConfig = new MockFilterConfig();
    mockFilterConfig.addInitParameter("headerName", HEADER_NAME);
    
    ipMapperFilter.init(mockFilterConfig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullRequest() throws Exception {
    ipMapperFilter.doFilter(null, null, null);
  }
    
  @Test
  public void testHeaderNamePassedInToRequestShouldBeStripped() throws Exception {
    // This test verifies that if a HEADER_NAME header is given in the request,
    // that it is stripped out. This is to prevent spoofing.
    mockRequest.addHeader(HEADER_NAME, "campus");
    
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNull(finalRequest.getHeader(HEADER_NAME));
  } 
  
  @Test
  public void testNoRemoteORForwardedAddr() throws Exception {
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNull(finalRequest.getHeader(HEADER_NAME));
  }
  
  @Test
  public void testRemoteAddrNotAllowed() throws Exception {
    mockRequest.setRemoteAddr("193.168.39.1");
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNull(finalRequest.getHeader(HEADER_NAME));
  }
  
  @Test
  public void testRemoteAddrAllowed() throws Exception {
    mockRequest.setRemoteAddr("192.168.40.1");
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNotNull(finalRequest.getHeader(HEADER_NAME));
    assertEquals("annex,campus,office", finalRequest.getHeader(HEADER_NAME).trim());
  }
  
  @Test
  public void testNoForwardedAddr() throws Exception {
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNull(finalRequest.getHeader(HEADER_NAME));
  }
  
  @Test
  public void testForwardedAddrNotAllowed() throws Exception {
    mockRequest.addHeader("X-FORWARDED-FOR", "192.163.40.1");
    mockRequest.setRemoteAddr("193.168.39.1");
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNull(finalRequest.getHeader(HEADER_NAME));
  }
  
  @Test
  public void testForwardedAddrAllowed() throws Exception {
    mockRequest.setRemoteAddr("192.168.38.1");
    ipMapperFilter.doFilter(mockRequest, mockResponse, mockChain);
    
    HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNotNull(finalRequest.getHeader(HEADER_NAME));
    assertEquals("campus,housing", finalRequest.getHeader(HEADER_NAME).trim());
  }  
}