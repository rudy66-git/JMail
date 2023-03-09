import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

public class SearchMail extends HttpServlet{
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String searchValue = (String) req.getParameter("searchvalue");
    boolean unread =Boolean.parseBoolean(req.getParameter("unreadstatus"));
    String option = (String) req.getParameter("option");
    MailReader mailReader = new MailReader();
    JSONArray jsonArray = null;
    try {
      jsonArray = mailReader.searchMessages(option, searchValue, unread);
    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(jsonArray);
  }
}