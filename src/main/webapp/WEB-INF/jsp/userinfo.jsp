<%@page contentType="text/html" %>
<%@page pageEncoding="UTF-8" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix = "c" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>UMD Libraries Fedora Repository</title>
  </head>
  <body>
    <p><strong>${userName}</strong> is logged in as ${userRole}</p>

    <p><a href="${repositoryRootPath}">Fedora REST API Endpoint</a></p>

    <c:if test='${userRole == "fedoraAdmin"}'>
      <form action="${userManagementRootPath}/token" method="get">
        <p>
          <label>Subject: <input name="subject"/></label>
          <label>Role:
            <select name="role">
              <option>fedoraAdmin</option>
              <option>fedoraUser</option>
            </select>
          </label>
          <button>Create token</button>
        </p>
      </form>
    </c:if>

    <form action="${userManagementRootPath}/logout" method="post">
      <p><button>Log out</button></p>
    </form>
  </body>
</html>
