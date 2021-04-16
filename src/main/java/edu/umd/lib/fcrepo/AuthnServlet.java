package edu.umd.lib.fcrepo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static edu.umd.lib.fcrepo.LdapRoleLookupService.ADMIN_ROLE;
import static edu.umd.lib.fcrepo.LdapRoleLookupService.USER_ROLE;

public class AuthnServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (request.isUserInRole(ADMIN_ROLE)) {
      request.setAttribute("userRole", ADMIN_ROLE);
    } else if (request.isUserInRole(USER_ROLE)) {
      request.setAttribute("userRole", USER_ROLE);
    }
    request.setAttribute("userName", request.getRemoteUser());
    request.setAttribute("repositoryRootPath", request.getContextPath() + "/rest");

    // check for a destination query parameter, and redirect there if one is found
    // but only if it looks like a path (i.e., don't allow redirects to absolute URIs)
    final String destination = request.getParameter("destination");
    if (destination != null && destination.startsWith("/")) {
      response.sendRedirect(destination);
    } else {
      // show a basic userinfo page
      request.getRequestDispatcher("/WEB-INF/jsp/userinfo.jsp").forward(request, response);
    }
  }
}
