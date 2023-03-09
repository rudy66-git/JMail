import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class MarkAllAsReadUsingRest extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    HttpSession session = req.getSession(false);
    String access_token = (String)session.getAttribute("access_token");
    MailSearcher mailSearcher = new MailSearcher();
    mailSearcher.markAllAsRead(access_token);
  }
}