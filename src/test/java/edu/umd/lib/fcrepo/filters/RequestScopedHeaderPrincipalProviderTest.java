package edu.umd.lib.fcrepo.filters;

import jakarta.servlet.ServletException;
import org.apache.hc.client5.http.auth.BasicUserPrincipal;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RequestScopedHeaderPrincipalProviderTest {
  private static final String HEADER_NAME = "X-Auth-IP-Mapping";

  private final Principal user = new BasicUserPrincipal("jsmith");

  private final Principal adminRole = new ContainerRolesPrincipal("fedoraAdmin");

  private final RequestScopedHeaderPrincipalProvider provider = new RequestScopedHeaderPrincipalProvider();

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private Subject subject;

  @Before
  public void setUp() {
    provider.setHeaderName(HEADER_NAME);
    provider.setSeparator(",");

    final DefaultSecurityManager securityManager = new DefaultSecurityManager();
    // simulates the subject after ServletContainerAuthFilter has logged in the user
    subject = new Subject.Builder(securityManager)
        .principals(new SimplePrincipalCollection(List.of(user, adminRole), "servlet container authentication"))
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }

  @After
  public void tearDown() {
    ThreadContext.remove();
  }

  @Test
  public void testUserStaysPrimaryPrincipalWithHeaderPrincipals() throws ServletException, IOException {
    request.addHeader(HEADER_NAME, "http://example.com/campus,http://example.com/library");
    final AtomicReference<PrincipalCollection> duringRequest = new AtomicReference<>();

    provider.doFilter(request, response, (req, res) -> duringRequest.set(SecurityUtils.getSubject().getPrincipals()));

    final PrincipalCollection principals = duringRequest.get();
    assertNotNull(principals);
    assertEquals(user, principals.getPrimaryPrincipal());
    assertTrue(principals.asSet().contains(adminRole));
    assertTrue(principals.asSet().contains(new HttpHeaderPrincipalForTest("http://example.com/campus")));
    assertTrue(principals.asSet().contains(new HttpHeaderPrincipalForTest("http://example.com/library")));
  }

  @Test
  public void testRunAsIsReleasedAfterRequest() throws ServletException, IOException {
    request.addHeader(HEADER_NAME, "http://example.com/campus");

    provider.doFilter(request, response, (req, res) -> assertTrue(SecurityUtils.getSubject().isRunAs()));

    assertFalse(subject.isRunAs());
    assertEquals(user, subject.getPrincipal());
  }

  @Test
  public void testStaleRunAsFromEarlierRequestIsCleared() throws ServletException, IOException {
    // what fcrepo's provider could leave in the session: the role principal first
    subject.runAs(new SimplePrincipalCollection(List.of(adminRole, user), "stale"));
    assertEquals(adminRole, subject.getPrincipal());

    request.addHeader(HEADER_NAME, "http://example.com/campus");
    final AtomicReference<Object> primary = new AtomicReference<>();

    provider.doFilter(request, response, (req, res) -> primary.set(SecurityUtils.getSubject().getPrincipal()));

    assertEquals(user, primary.get());
    assertFalse(subject.isRunAs());
  }

  @Test
  public void testNoHeaderLeavesPrincipalsUnchanged() throws ServletException, IOException {
    provider.doFilter(request, response, (req, res) -> assertFalse(SecurityUtils.getSubject().isRunAs()));

    assertEquals(user, subject.getPrincipal());
  }

  // HttpHeaderPrincipal's constructor is protected, so use a subclass to create test instances
  private static class HttpHeaderPrincipalForTest extends HttpHeaderPrincipal {
    HttpHeaderPrincipalForTest(final String name) {
      super(name);
    }
  }
}
