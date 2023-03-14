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
    String mail = (String) session.getAttribute("mail");
    UserDAO userDAO = new UserDAO();
    String access_token = userDAO.getAccessToken(mail);

    if(mail.endsWith("gmail.com")){
      GMailReader gMailReader = new GMailReader();
      gMailReader.markAllAsRead(access_token);
    }
    else if(mail.endsWith("zohotest.com")) {
      ZOHOMailReader zohoMailReader = new ZOHOMailReader();
      zohoMailReader.markAllAsRead(access_token);
    }
    else if(mail.endsWith("outlook.com")) {
      MicrosoftMailReader microsoftMailReader = new MicrosoftMailReader();
      microsoftMailReader.markAllAsRead(access_token);
    } 
  }
}