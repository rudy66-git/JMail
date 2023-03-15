import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetMailUsingRest extends HttpServlet {
  private static final String ZOHO_MESSAGE_ENDPOINT = "http://mail.zoho.in/api/accounts";
  private static final String GMAIL_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
  private static final String MICROSOFT_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/messages";
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray responseArray = null;
    String endpoint = null;
    try {
      HttpSession session = req.getSession(false);
      String mail = (String) session.getAttribute("mail");
      UserDAO userDAO = new UserDAO();
      String access_token = userDAO.getAccessToken(mail);

      MailReader mailReader = null;

      if (mail.endsWith("gmail.com")) {
        mailReader = new GMailReader();
        endpoint = GMAIL_MESSAGE_ENDPOINT;
      } else if (mail.endsWith("zohotest.com")) {
        endpoint = ZOHO_MESSAGE_ENDPOINT;
        mailReader = new ZOHOMailReader();
      } else if (mail.endsWith("outlook.com")) {
        endpoint = MICROSOFT_MESSAGE_ENDPOINT;
        mailReader = new MicrosoftMailReader();
      }
      JSONObject responseObject = mailReader.getMessageObject(access_token,endpoint);
      responseArray = mailReader.readMail(responseObject,access_token);

    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(responseArray);
  }
}