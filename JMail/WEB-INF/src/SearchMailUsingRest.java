import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class SearchMailUsingRest extends HttpServlet {

  private static final String GMAIL_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
  private static final String ZOHO_MESSAGE_ENDPOINT = "http://mail.zoho.in/api/accounts";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray responseArray = null;
    String searchValue = (String) req.getParameter("searchvalue");
    boolean unReadStatus = Boolean.parseBoolean(req.getParameter("unreadstatus"));
    String option = (String) req.getParameter("option");
    String messageEndpoint = null;
    try {

      HttpSession session = req.getSession(false);
      String access_token = (String) session.getAttribute("access_token");
      String mail = (String) session.getAttribute("mail");
      if (mail.endsWith("gmail.com"))
        messageEndpoint = GMAIL_MESSAGE_ENDPOINT;
      else
        messageEndpoint = ZOHO_MESSAGE_ENDPOINT;

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(messageEndpoint);

      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      ResponseHandler<JSONObject> responseHandler = new ResponseHandler<JSONObject>() {

        @Override
        public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int statusCode = response.getStatusLine().getStatusCode();
          HttpEntity responseEntity = response.getEntity();

          if (statusCode >= 300) {
            throw new HttpResponseException(statusCode,
                response.getStatusLine().getReasonPhrase());
          }
          if (responseEntity == null) {
            throw new ClientProtocolException("No content in response");
          }

          ContentType contentType = ContentType.get(responseEntity);
          Charset charset = contentType.getCharset();
          String json = EntityUtils.toString(responseEntity, charset);
          JSONObject responseJSON = new JSONObject(json);
          return responseJSON;
        }
      };

      JSONObject mailList = client.execute(httpGet, responseHandler);

      if (mail.endsWith("gmail.com")) {
        JSONArray responseMessage = (JSONArray) mailList.get("messages");
        JSONArray messages = new JSONArray();
        for (int i = 0; i < responseMessage.length(); i++) {
          httpGet = new HttpGet(messageEndpoint + responseMessage.getJSONObject(i).getString("id"));
          httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
          JSONObject messageObject = client.execute(httpGet, responseHandler);
          messages.put(messageObject);
        }
        GMailReader gmailReader = new GMailReader();
        if (option.equals("Sender"))
          responseArray = gmailReader.searchMailBySender(messages, searchValue, unReadStatus);
        else if (option.equals("Subject"))
          responseArray = gmailReader.searchMailBySubject(messages, searchValue, unReadStatus);
        else
          responseArray = gmailReader.searchMailByContent(messages, searchValue, unReadStatus);
      } else if (mail.endsWith("zohotest.com")) {

        // System.out.println("Message list : "+mailList);
        JSONArray dataArray = mailList.getJSONArray("data");
        JSONObject dataObject = dataArray.getJSONObject(0);
        String accountId = dataObject.getString("accountId");
        httpGet = new HttpGet(messageEndpoint + "/"+accountId+"/messages/view");
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        JSONObject zohoMailList = client.execute(httpGet, responseHandler);
        System.out.println("Zoho mail list : "+zohoMailList);
        JSONArray mailsObject = zohoMailList.getJSONArray("data");
        System.out.println("Zoho mails object list : "+mailsObject);
        JSONArray messages = new JSONArray();
        for (int i = 0; i < mailsObject.length(); i++) {
          JSONObject messageObject = mailsObject.getJSONObject(i);
          // System.out.println("MessageObject read status : "+messageObject.getString("status"));
          // System.out.println("Message id : "+messageObject.getString("messageId"));
          // System.out.println("Unread status : "+unReadStatus);
          if((messageObject.getString("status").equals("1") && unReadStatus) || (messageObject.getString("status").equals("0") && !(unReadStatus)))
            System.out.println("Message id condition : "+messageObject.getString("messageId"));
            messages.put(messageObject);
        }

        System.out.println("Satisfying unread status Message object : "+messages);

        ZOHOMailReader zohoMailReader = new ZOHOMailReader();
        if (option.equals("Sender")){
          responseArray = zohoMailReader.searchMailBySender(messages, searchValue,access_token,accountId);

        }
        else if (option.equals("Subject")){
          System.out.println("In subject");
          responseArray = zohoMailReader.searchMailBySubject(messages, searchValue,access_token,accountId);
        }
        else
          responseArray = zohoMailReader.searchMailByContent(messages, searchValue,access_token,accountId);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(responseArray);
  }
}