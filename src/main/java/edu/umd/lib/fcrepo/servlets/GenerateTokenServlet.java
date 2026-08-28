package edu.umd.lib.fcrepo.servlets;

import edu.umd.lib.fcrepo.services.AuthTokenService;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.Date;

import static edu.umd.lib.fcrepo.services.LdapRoleLookupService.ADMIN_ROLE;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

public class GenerateTokenServlet extends HttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(GenerateTokenServlet.class);

  private AuthTokenService keyService;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    final WebApplicationContext context = WebApplicationContextUtils
        .getRequiredWebApplicationContext(config.getServletContext());
    keyService = context.getBean(AuthTokenService.class);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!req.isUserInRole(ADMIN_ROLE)) {
      // only admins may create tokens
      resp.setStatus(SC_FORBIDDEN);
      resp.setContentType("text/plain");
      resp.getWriter().println("Only admins may create tokens");
    } else {
      final String subject = req.getParameter("subject");
      final String role = req.getParameter("role");
      final String requestingUser = req.getRemoteUser();
      final Date oneYearHence = Date.from(now().plus(365, DAYS));

      final String jws = keyService.createToken(subject, requestingUser, oneYearHence, role);

      resp.setContentType("text/plain");
      resp.getWriter().println(jws);
    }
  }
}
