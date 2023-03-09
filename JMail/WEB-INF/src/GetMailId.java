import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class GetMailId extends HttpServlet {
  // private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String mailId = null;
    HttpSession session = req.getSession(false);
    mailId = (String)session.getAttribute("mail");
    resp.setContentType("text/html");
    resp.getWriter().print(mailId);
  }
}
