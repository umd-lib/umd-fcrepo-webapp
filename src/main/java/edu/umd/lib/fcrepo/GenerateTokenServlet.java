package edu.umd.lib.fcrepo;

import io.jsonwebtoken.Jwts;
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
import java.security.Key;
import java.util.Date;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

public class GenerateTokenServlet extends HttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(GenerateTokenServlet.class);

  private Key key;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    final WebApplicationContext context = WebApplicationContextUtils
        .getRequiredWebApplicationContext(config.getServletContext());
    key = context.getBean(SecretKeyService.class).getSecretKey();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    final String subject = req.getParameter("subject");
    final String userName = req.getRemoteUser();
    final Date expiry = Date.from(now().plus(365, DAYS));

    logger.info("Creating token with subject: {}", subject);
    logger.info("Issuer will be the requesting user: {}", userName);
    logger.info("Expiration date: {}", expiry);

    final String jws = Jwts.builder()
        .setSubject(subject)
        .setIssuer(userName)
        .setExpiration(expiry)
        .signWith(key)
        .compact();

    resp.setContentType("text/plain");
    resp.getWriter().println(jws);
  }
}
