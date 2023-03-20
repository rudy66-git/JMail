import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RespHandler  extends HttpServlet{
  private static final String accessTokenServlet = "getAccessToken";
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String code = req.getParameter("code");

    if(code != null){
      req.setAttribute("code", code);
      RequestDispatcher rd = req.getRequestDispatcher(accessTokenServlet);
      rd.forward(req,resp);
    }
    else {
      resp.sendRedirect("http://localhost:8080/JMail/index.html");
    }
  }
}