import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthoriseUser extends HttpServlet {

  private static final String CLIENT_ID_WEB = "842896570142-p2e2ir76rosjbifsarvh8f2g0161e5mc.apps.googleusercontent.com";
  private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String REDIRECT_URI = "http://localhost:8080/JMail/callback";
  private static final String SCOPE = "https://mail.google.com/";
  // private static final String SCOPE1 = "https://mail.google.com/";
  // private static final String RESPONSE_TYPE = "code";
  // https://accounts.google.com/o/oauth2/v2/auth?redirect_uri=https%3A%2F%2Fdevelopers.google.com%2Foauthplayground&prompt=consent&response_type=code&client_id=407408718192.apps.googleusercontent.com&scope=https%3A%2F%2Fmail.google.com%2F&access_type=offline

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String mail = (String) req.getParameter("mail");
    UserDAO userDAO = new UserDAO();
    HttpSession session = req.getSession();
    session.setAttribute("mail",mail);
    try {
      boolean validUser = userDAO.userExists(mail);
      if (!validUser) {
        String authUrl = AUTH_ENDPOINT + "?redirect_uri=" + REDIRECT_URI + "&prompt=consent&response_type=code&client_id="
            + CLIENT_ID_WEB + "&scope=https%3A%2F%2Fmail.google.com%2F&access_type=offline";
        resp.sendRedirect(authUrl);
      } else {
        String access_token = userDAO.getAccessToken(mail);
        System.out.println("Access token : "+access_token);
        if (access_token == null) {
          String refresh_token = userDAO.getRefreshToken(mail);
          System.out.println("Refresh token : "+refresh_token);
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