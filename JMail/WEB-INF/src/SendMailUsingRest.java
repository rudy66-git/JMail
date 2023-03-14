import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SendMailUsingRest extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String to = req.getParameter("to");
    String subject = req.getParameter("subject");
    String content = req.getParameter("content");

    boolean hasAttachment = Boolean.parseBoolean(req.getParameter("hasAttachment"));

    if (hasAttachment) {
      
    }

    HttpSession session = req.getSession(false);
    String mail = (String) session.getAttribute("mail");
    UserDAO userDAO = new UserDAO();
    String access_token = userDAO.getAccessToken(mail);

    if (mail.endsWith("outlook.com")) {
      MicrosoftMailReader microsoftMailReader = new MicrosoftMailReader();
      microsoftMailReader.sendMail(access_token, to, subject, "text", content, hasAttachment);

    } else if (mail.endsWith("zohotest.com")) {
      ZOHOMailReader zohoMailReader = new ZOHOMailReader();
      zohoMailReader.sendMail(access_token, mail, to, subject, "text", content);

    }

  }
}