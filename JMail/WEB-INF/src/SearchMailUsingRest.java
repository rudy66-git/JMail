import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

public class SearchMailUsingRest extends HttpServlet {

  private static final String GMAIL_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
  private static final String ZOHO_MESSAGE_ENDPOINT = "http://mail.zoho.in/api/accounts";
  private static final String MICROSOFT_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/messages";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray responseArray = null;
    String searchValue = (String) req.getParameter("searchvalue");
    boolean unReadStatus = Boolean.parseBoolean(req.getParameter("unreadstatus"));
    String option = (String) req.getParameter("option");
    String messageEndpoint = null;
    try {

      HttpSession session = req.getSession(false);
      String mail = (String) session.getAttribute("mail");
      UserDAO userDAO = new UserDAO();
      String access_token = userDAO.getAccessToken(mail);
      MailReader mailReader = null;
      if (mail.endsWith("gmail.com")) {
        messageEndpoint = GMAIL_MESSAGE_ENDPOINT;
        mailReader = new GMailReader();
      }
      else if (mail.endsWith("zohotest.com")) {
        messageEndpoint = ZOHO_MESSAGE_ENDPOINT;
        mailReader = new ZOHOMailReader();
      }
      else if (mail.endsWith("outlook.com")) {
        messageEndpoint = MICROSOFT_MESSAGE_ENDPOINT;
        mailReader = new MicrosoftMailReader();
      }

      JSONObject mailList = mailReader.getMessageObject(access_token,messageEndpoint);
      responseArray = mailReader.searchMail(mailList,access_token , option, searchValue,unReadStatus);

      // if (mail.endsWith("gmail.com")) {
      //   JSONArray responseMessage = (JSONArray) mailList.get("messages");
      //   JSONArray gmailMessages = new JSONArray();
      //   for (int i = 0; i < responseMessage.length(); i++) {
      //     httpGet = new HttpGet(messageEndpoint + responseMessage.getJSONObject(i).getString("id"));
      //     httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      //     JSONObject messageObject = client.execute(httpGet, responseHandler);
      //     gmailMessages.put(messageObject);
      //   }
      //   GMailReader gmailReader = new GMailReader();
      //   if (option.equals("Sender"))
      //     responseArray = gmailReader.searchMailBySender(gmailMessages, searchValue, unReadStatus);
      //   else if (option.equals("Subject"))
      //     responseArray = gmailReader.searchMailBySubject(gmailMessages, searchValue, unReadStatus);
      //   else
      //     responseArray = gmailReader.searchMailByContent(gmailMessages, searchValue, unReadStatus);
      // } else if (mail.endsWith("zohotest.com")) {

      //   JSONArray dataArray = mailList.getJSONArray("data");
      //   JSONObject dataObject = dataArray.getJSONObject(0);
      //   String accountId = dataObject.getString("accountId");
      //   httpGet = new HttpGet(messageEndpoint + "/" + accountId + "/messages/view");
      //   httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      //   JSONObject zohoMailList = client.execute(httpGet, responseHandler);
      //   JSONArray mailsObject = zohoMailList.getJSONArray("data");
      //   JSONArray messages = new JSONArray();
      //   for (int i = 0; i < mailsObject.length(); i++) {
      //     JSONObject messageObject = mailsObject.getJSONObject(i);
      //     if ((messageObject.getString("status").equals("1") && !(unReadStatus))
      //         || (messageObject.getString("status").equals("0") && unReadStatus)) {
      //       messages.put(messageObject);
      //     }
      //   }

      //   ZOHOMailReader zohoMailReader = new ZOHOMailReader();
      //   if (option.equals("Sender")) {
      //     responseArray = zohoMailReader.searchMailBySender(messages, searchValue, access_token, accountId);
      //   } else if (option.equals("Subject")) {
      //     responseArray = zohoMailReader.searchMailBySubject(messages, searchValue, access_token, accountId);
      //   } else
      //     responseArray = zohoMailReader.searchMailByContent(messages, searchValue, access_token, accountId);
      // } else if (mail.endsWith("outlook.com")) {

      //   JSONArray mailsObject = mailList.getJSONArray("value");
      //   JSONArray messages = new JSONArray();
      //   for (int i = 0; i < mailsObject.length(); i++) {
      //     JSONObject messageObject = mailsObject.getJSONObject(i);
      //     if ((messageObject.getBoolean("isRead") && !(unReadStatus))
      //         || ((!messageObject.getBoolean("isRead")) && unReadStatus)) {
      //       messages.put(messageObject);
      //     }
      //   }

      //   MicrosoftMailReader microsoftMailReader = new MicrosoftMailReader();
      //   if (option.equals("Sender")) {
      //     responseArray = microsoftMailReader.searchMailBySender(messages, searchValue);
      //   } else if (option.equals("Subject")) {
      //     responseArray = microsoftMailReader.searchMailBySubject(messages, searchValue);
      //   } else
      //     responseArray = microsoftMailReader.searchMailByContent(messages, searchValue);
      // }

    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(responseArray);
  }
}