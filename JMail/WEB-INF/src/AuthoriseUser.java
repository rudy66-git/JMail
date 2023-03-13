import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthoriseUser extends HttpServlet {

  private static final String GMAIL_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String GMAIL_CLIENT_ID = "842896570142-p2e2ir76rosjbifsarvh8f2g0161e5mc.apps.googleusercontent.com";
  private static final String GMAIL_SCOPE = "https%3A%2F%2Fmail.google.com%2F";

  private static final String ZOHO_AUTH_ENDPOINT = "https://accounts.zoho.in/oauth/v2/auth";
  private static final String ZOHO_CLIENT_ID = "1000.8ENBL33K5L6S3XEXZO3G79PN7U8F6D";
  private static final String ZOHO_SCOPE = "ZohoMail.messages.READ+ZohoMail.accounts.READ";
  
  private static final String MICROSOFT_AUTH_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
  private static final String MICROSOFT_CLIENT_ID = "379f2921-f731-4cf2-9c3d-624db1936026";
  private static final String MICROSOFT_SCOPE = "Mail.ReadWrite offline_access";

  private static final String REDIRECT_URI = "http://localhost:8080/JMail/callback";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String authEndpoint = null;
    String scope = null;
    String clientId = null;
    
    String mail = (String) req.getParameter("mail");
    UserDAO userDAO = new UserDAO();
    
    HttpSession session = req.getSession();
    session.setAttribute("mail", mail);
    
    if (mail.endsWith("gmail.com")) {
      authEndpoint = GMAIL_AUTH_ENDPOINT;
      scope = GMAIL_SCOPE;
      clientId = GMAIL_CLIENT_ID;
    } else if (mail.endsWith("zohotest.com")) {
      authEndpoint = ZOHO_AUTH_ENDPOINT;
      scope = ZOHO_SCOPE;
      clientId = ZOHO_CLIENT_ID;
    }
    else if (mail.endsWith("outlook.com")) {
      authEndpoint = MICROSOFT_AUTH_ENDPOINT;
      scope = MICROSOFT_SCOPE;
      clientId = MICROSOFT_CLIENT_ID;
    }

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
        System.out.println("Access token : " + access_token);
        if (access_token == null) {
          String refresh_token = userDAO.getRefreshToken(mail);
          RequestDispatcher requestDispatcher = req.getRequestDispatcher("requestAccessToken");
          req.setAttribute("refresh_token", refresh_token);
          req.setAttribute("mail", mail);
          requestDispatcher.forward(req, resp);
        } else {
          session.setAttribute("access_token", access_token);
          resp.sendRedirect("welcome.html");
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}