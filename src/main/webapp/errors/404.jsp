<html>
  <head>
    <title>404 Not Found</title>
    <style type="text/css">
    .credit { font-style: italic; border-top: 1px solid #999; padding-top: 1em; }
    </style>
  </head>
  <body>
    <h1>404 Not Found</h1>

    <p><img src='<%= request.getAttribute("jakarta.servlet.forward.context_path") %>/errors/404_turtle.jpg' alt=""/></p>

    <p>Nothing found at <strong><%= request.getAttribute("jakarta.servlet.forward.request_uri") %></strong></p>

    <p class="credit">
    <a href="https://commons.wikimedia.org/wiki/File:Painted_Turtle_covered_in_duckweed_(29688110516).jpg">Turtle image</a>
    by <a href="https://www.flickr.com/people/37922399@N05">Virginia State Parks</a>, via Wikimedia,
    used under the terms of the <a href="https://creativecommons.org/licenses/by/2.0/">CC BY 2.0</a> license.</p>
  </body>
</html>
