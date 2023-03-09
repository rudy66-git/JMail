import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;

public class GetMail extends HttpServlet{
  private static final String gmailHost = "imap.gmail.com";
  private static final String outlookHost = "outlook.office365.com";
  private static final String storeType = "imap";
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray jsonArray = null;

    try {
      HttpSession session = req.getSession(false);
      String access_token = (String) session.getAttribute("access_token");
      String mail = (String) session.getAttribute("mail");
      MailReader mailReader = new MailReader();
      if(mail != null) {
        
        String host = (mail.endsWith("gmail.com")) ? gmailHost : outlookHost;
        
        jsonArray = mailReader.readMessages(host, mail, access_token, storeType);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(jsonArray);
    
  }
}
