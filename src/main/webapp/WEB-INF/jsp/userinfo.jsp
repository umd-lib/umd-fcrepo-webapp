<%@page contentType="text/html" %>
<%@page pageEncoding="UTF-8" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>UMD Libraries Fedora Repository</title>
  </head>
  <body>
    <p><strong>${userName}</strong> is logged in as ${userRole}</p>

    <p><a href="${repositoryRootPath}">Fedora REST API Endpoint</a></p>

    <form action="/user/logout" method="post">
      <button>Log out</button>
    </form>
  </body>
</html>
