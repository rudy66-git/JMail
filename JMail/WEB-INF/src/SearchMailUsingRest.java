import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

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

  private static final String GET_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray responseArray = null;
    String searchValue = (String) req.getParameter("searchvalue");
    boolean unReadStatus = Boolean.parseBoolean(req.getParameter("unreadstatus"));
    String option = (String) req.getParameter("option");
    try {

      HttpSession session = req.getSession(false);
      String access_token = (String) session.getAttribute("access_token");

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(GET_MESSAGE_ENDPOINT);

      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      ResponseHandler<JSONObject> responseHandler = new ResponseHandler<JSONObject>() {

        @Override
        public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int statusCode = response.getStatusLine().getStatusCode();
          HttpEntity responseEntity = response.getEntity();
          System.out.println("Status code : " + statusCode);
          if (statusCode >= 300) {
            throw new HttpResponseException(statusCode,
                response.getStatusLine().getReasonPhrase());
          }
          if (responseEntity == null) {
            throw new ClientProtocolException("No content in response");
          }
          System.out.println("Response entity : " + responseEntity.getContent());

          ContentType contentType = ContentType.get(responseEntity);
          Charset charset = contentType.getCharset();
          String json = EntityUtils.toString(responseEntity, charset);
          JSONObject responseJSON = new JSONObject(json);
          return responseJSON;
        }
      };

      JSONObject mailList = client.execute(httpGet, responseHandler);
      System.out.println("Response : " + mailList.toString());

      JSONArray responseMessage = (JSONArray) mailList.get("messages");

      JSONArray messages = new JSONArray();
      for (int i = 0; i < responseMessage.length(); i++) {
        httpGet = new HttpGet(GET_MESSAGE_ENDPOINT + responseMessage.getJSONObject(i).getString("id"));
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        JSONObject messageObject = client.execute(httpGet, responseHandler);
        messages.put(messageObject);
      }
      MailSearcher mailSearcher = new MailSearcher();
      if(option.equals("Sender"))
        responseArray = mailSearcher.searchMailBySender(messages, searchValue, unReadStatus);
      else if(option.equals("Subject"))
        responseArray = mailSearcher.searchMailBySubject(messages, searchValue, unReadStatus);
      else 
      responseArray = mailSearcher.searchMailByContent(messages, searchValue, unReadStatus);

    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(responseArray);
  }
}