package edu.umd.lib.fcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthnServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (request.isUserInRole(FedoraRolesFilter.ADMIN_ROLE)) {
      request.setAttribute("userRole", FedoraRolesFilter.ADMIN_ROLE);
    } else if (request.isUserInRole(FedoraRolesFilter.USER_ROLE)) {
      request.setAttribute("userRole", FedoraRolesFilter.USER_ROLE);
    }
    request.setAttribute("userName", request.getRemoteUser());
    request.setAttribute("repositoryRootPath", request.getContextPath() + "/rest");

    // show a basic userinfo page
    // taken from https://github.com/cas-projects/cas-sample-java-webapp/blob/master/src/main/webapp/index.jsp
    request.getRequestDispatcher("/WEB-INF/jsp/userinfo.jsp").forward(request, response);
  }
}
