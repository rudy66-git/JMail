import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

public class AuthoriseUser extends HttpServlet {

  private static final String GMAIL_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String GMAIL_ACCESS_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
  private static final String GMAIL_SCOPE = "https%3A%2F%2Fmail.google.com%2F";
  
  private static final String ZOHO_AUTH_ENDPOINT = "https://accounts.zoho.in/oauth/v2/auth";
  private static final String ZOHO_ACCESS_TOKEN_ENDPOINT = "https://accounts.zoho.in/oauth/v2/token";
  private static final String ZOHO_SCOPE = "ZohoMail.messages.READ ZohoMail.accounts.READ ZohoMail.messages.CREATE ZohoMail.messages.UPDATE";
  
  private static final String MICROSOFT_AUTH_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
  private static final String MICROSOFT_ACCESS_TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
  private static final String MICROSOFT_SCOPE = "Mail.ReadWrite Mail.Send offline_access";

  private static final String REDIRECT_URI = "http://localhost:8080/JMail/callback";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String authEndpoint = null;
    String scope = null;
    String clientId = null;
    String accessTokenEndpoint = null;
    
    String mail = (String) req.getParameter("mail");
    UserDAO userDAO = new UserDAO();
    
    HttpSession session = req.getSession();
    session.setAttribute("mail", mail);
    JSONObject appCredentials = null;
    MailReader mailReader = null;
    if (mail.endsWith("gmail.com")) {
      appCredentials = userDAO.getAppCredentials("gmail");
      authEndpoint = GMAIL_AUTH_ENDPOINT;
      scope = GMAIL_SCOPE;
      mailReader = new GMailReader();
      accessTokenEndpoint = GMAIL_ACCESS_TOKEN_ENDPOINT;
    } else if (mail.endsWith("zohotest.com")) {
      appCredentials = userDAO.getAppCredentials("zohomail");
      authEndpoint = ZOHO_AUTH_ENDPOINT;
      scope = ZOHO_SCOPE;
      mailReader = new ZOHOMailReader();
      accessTokenEndpoint = ZOHO_ACCESS_TOKEN_ENDPOINT;
    }
    else if (mail.endsWith("outlook.com")) {
      appCredentials = userDAO.getAppCredentials("outlook");
      authEndpoint = MICROSOFT_AUTH_ENDPOINT;
      scope = MICROSOFT_SCOPE;
      mailReader = new MicrosoftMailReader();
      accessTokenEndpoint = MICROSOFT_ACCESS_TOKEN_ENDPOINT;
    }
    if(appCredentials != null)
      clientId = appCredentials.getString("clientId");

    try {
      boolean validUser = userDAO.userExists(mail);
      if (!validUser) {
        String authUrl = authEndpoint + "?redirect_uri=" + REDIRECT_URI
            + "&prompt=consent&response_type=code&client_id="
            + clientId + "&scope=" + scope + "&access_type=offline";
        resp.sendRedirect(authUrl);
      }
      else {
        String access_token = userDAO.getAccessToken(mail);
        if (access_token == null) {
          access_token = mailReader.requestAccessToken(mail,appCredentials,accessTokenEndpoint);
          System.out.println("Access token : "+access_token);
          userDAO.updateAccessToken(access_token,mail);
          resp.sendRedirect("welcome.html");
        } else {
          resp.sendRedirect("welcome.html");
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}