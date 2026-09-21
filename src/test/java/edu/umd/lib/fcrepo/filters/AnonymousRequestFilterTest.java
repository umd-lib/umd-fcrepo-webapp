package edu.umd.lib.fcrepo.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.auth.BasicUserPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.security.Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnonymousRequestFilterTest {
  final MockHttpServletRequest mockServletRequest = new MockHttpServletRequest();
  final MockHttpServletResponse mockServletResponse = new MockHttpServletResponse();
  final MockFilterChain mockChain = new MockFilterChain();
  final Filter anonymousRequestFilter = new AnonymousRequestFilter();

  @Test
  public void testProvideAnonymousPrincipalIfNonePresent() throws ServletException, IOException {
    anonymousRequestFilter.doFilter(mockServletRequest, mockServletResponse, mockChain);

    final HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNotNull(finalRequest);

    final Principal principal = finalRequest.getUserPrincipal();
    assertNotNull(principal);
    assertTrue(principal instanceof AnonymousRequestFilter.AnonymousUserPrincipal);
    assertEquals("http://xmlns.com/foaf/0.1/Agent", principal.getName());
  }

  @Test
  public void testLeavePrincipalAsIsIfPresent() throws ServletException, IOException {
    final Principal preExistingPrincipal = new BasicUserPrincipal("somebody");
    mockServletRequest.setUserPrincipal(preExistingPrincipal);

    anonymousRequestFilter.doFilter(mockServletRequest, mockServletResponse, mockChain);

    final HttpServletRequest finalRequest = (HttpServletRequest) mockChain.getRequest();
    assertNotNull(finalRequest);

    final Principal principal = finalRequest.getUserPrincipal();
    assertNotNull(principal);
    assertFalse(principal instanceof AnonymousRequestFilter.AnonymousUserPrincipal);
    assertEquals(preExistingPrincipal.getName(), principal.getName());
  }
}
