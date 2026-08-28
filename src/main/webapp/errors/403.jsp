<html>
  <head>
    <title>403 Forbidden</title>
  </head>
  <body>
    <h1>403 Forbidden</h1>

    <p>Access to <strong><%= request.getAttribute("jakarta.servlet.forward.request_uri") %></strong> has been forbidden.</p>
    <p>Try accessing the page after
      <a href='<%= request.getAttribute("jakarta.servlet.forward.context_path") %>/user?destination=<%= request.getAttribute("jakarta.servlet.forward.request_uri") %>'>logging in</a>.
    </p>
  </body>
</html>
