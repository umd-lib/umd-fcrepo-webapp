package edu.umd.lib.fcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import static edu.umd.lib.fcrepo.LdapRoleLookupService.ADMIN_ROLE;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

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
