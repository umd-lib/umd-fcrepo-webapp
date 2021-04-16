package edu.umd.lib.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Enumeration;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;

public class HeaderRequestWrapperTest {
  private MockFilterConfig mockFilterConfig = null;
  private MockHttpServletRequest mockRequest = null;
  
  private static String HEADER_NAME = "TEST-IPMAPPER-HEADER";

  @Before
  public void setUp() throws Exception {
    mockFilterConfig = new MockFilterConfig();
    mockFilterConfig.addInitParameter("headerName", HEADER_NAME);
            
    mockRequest = new MockHttpServletRequest();
  }
  
  @Test
  public void testEmptyHeaders() {
    HeaderRequestWrapper wrapper = new HeaderRequestWrapper(mockRequest);
    Enumeration<String> headerNames = wrapper.getHeaderNames();
    
    assertFalse(headerNames.hasMoreElements());
  }
  
  @Test
  public void testGetHeader() {
    mockRequest.addHeader("TEST-HEADER", "foobar");
    HeaderRequestWrapper wrapper = new HeaderRequestWrapper(mockRequest);

    Enumeration<String> headerNames = wrapper.getHeaderNames();

    String firstHeaderName = headerNames.nextElement();
    assertEquals("TEST-HEADER", firstHeaderName);

    String firstHeaderValue = wrapper.getHeader("TEST-HEADER");
    assertEquals("foobar", firstHeaderValue);

    assertFalse(headerNames.hasMoreElements());
  }
  
  @Test
  public void testRemoveHeader() {
    mockRequest.addHeader("TEST-HEADER-TO-REMOVE", "REMOVE_ME");
    mockRequest.addHeader("TEST-HEADER-TO-KEEP", "KEEP_ME");
    
    HeaderRequestWrapper wrapper = new HeaderRequestWrapper(mockRequest);
    wrapper.removeHeader("TEST-HEADER-TO-REMOVE");
    
    
    Enumeration<String> headerNames = wrapper.getHeaderNames();
    
    
    String firstHeaderName = headerNames.nextElement();
    assertEquals("TEST-HEADER-TO-KEEP", firstHeaderName);

    String firstHeaderValue = wrapper.getHeader("TEST-HEADER-TO-KEEP");
    assertEquals("KEEP_ME", firstHeaderValue);

    assertFalse(headerNames.hasMoreElements());
  }
}