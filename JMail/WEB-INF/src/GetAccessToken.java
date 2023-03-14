import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

public class GetAccessToken extends HttpServlet {

  private static final String GMAIL_ACCESS_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
  private static final String GMAIL_SCOPE = "https%3A%2F%2Fmail.google.com%2F";

  private static final String ZOHO_ACCESS_TOKEN_ENDPOINT = "https://accounts.zoho.in/oauth/v2/token";
  private static final String ZOHO_SCOPE = "ZohoMail.messages.READ ZohoMail.accounts.READ ZohoMail.messages.CREATE ZohoMail.messages.UPDATE";

  private static final String MICROSOFT_ACCESS_TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
  private static final String MICROSOFT_SCOPE = "Mail.ReadWrite Mail.Send offline_access";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpSession session = req.getSession(false);
    String mail = (String) session.getAttribute("mail");
    String code = (String) req.getAttribute("code");

    String accessTokenEndpoint = null;
    String scope = null;

    JSONObject appCredentials = null;
    MailReader mailReader = null;

    UserDAO userDAO = new UserDAO();
    if (mail.endsWith("gmail.com")) {
      appCredentials = userDAO.getAppCredentials("gmail");
      accessTokenEndpoint = GMAIL_ACCESS_TOKEN_ENDPOINT;
      scope = GMAIL_SCOPE;
      mailReader = new GMailReader();
    } else if (mail.endsWith("zohotest.com")) {
      accessTokenEndpoint = ZOHO_ACCESS_TOKEN_ENDPOINT;
      appCredentials = userDAO.getAppCredentials("zohomail");
      mailReader = new ZOHOMailReader();
      scope = ZOHO_SCOPE;
    } else if (mail.endsWith("outlook.com")) {
      accessTokenEndpoint = MICROSOFT_ACCESS_TOKEN_ENDPOINT;
      appCredentials = userDAO.getAppCredentials("outlook");
      mailReader = new MicrosoftMailReader();
      scope = MICROSOFT_SCOPE;
    }

    JSONObject accessTokenJSON = mailReader.getAccessToken(mail, appCredentials, accessTokenEndpoint, code, scope);
    String accessToken = (String) accessTokenJSON.get("access_token");
    String refreshToken = (String) accessTokenJSON.get("refresh_token");

    System.out.println("Insertion result :" + userDAO.storeAuthInfo(mail, accessToken, refreshToken));

    if (accessToken != null) {
      resp.sendRedirect("welcome.html");
    } else {
      resp.sendRedirect("index.html");
    }
  }
}
