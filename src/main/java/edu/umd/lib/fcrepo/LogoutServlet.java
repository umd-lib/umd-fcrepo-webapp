package edu.umd.lib.fcrepo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class LogoutServlet extends HttpServlet {
  private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    final String remoteUser = request.getRemoteUser();
    log.debug("Logging out: " + remoteUser);

    request.getSession().invalidate();
    // Redirect to CAS logout to destroy CAS session
    final WebApplicationContext context = WebApplicationContextUtils
        .getRequiredWebApplicationContext(request.getServletContext());
    final String logoutURL = context.getBean(CasService.class).getCasLogoutUrl();
    log.info("Redirecting to CAS logout URL: {}", logoutURL);
    response.sendRedirect(logoutURL);
  }
}

