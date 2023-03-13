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

public class GetUserAccountInfo extends HttpServlet {
  private static final String ZOHO_MESSAGE_ENDPOINT = "http://mail.zoho.in/api/accounts";
  private static final String GMAIL_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
  private static final String MICROSOFT_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/messages";
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray responseArray = null;
    String endpoint = null;
    try {
      HttpSession session = req.getSession(false);
      String access_token = (String) session.getAttribute("access_token");
      String mail = (String) session.getAttribute("mail");

      if (mail.endsWith("gmail.com")) {
        endpoint = GMAIL_MESSAGE_ENDPOINT;
      } else if (mail.endsWith("zohotest.com")) {
        endpoint = ZOHO_MESSAGE_ENDPOINT;
      } else if (mail.endsWith("outlook.com")) {
        endpoint = MICROSOFT_MESSAGE_ENDPOINT;
      }

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(endpoint);

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

      JSONObject userInfoObject = client.execute(httpGet, responseHandler);

      if (mail.endsWith("gmail.com")) {
        GMailReader gMailReader = new GMailReader();
        responseArray = gMailReader.readMail(userInfoObject,access_token);
      } else if (mail.endsWith("zohotest.com")) {
        ZOHOMailReader zohoMailReader = new ZOHOMailReader();
        responseArray = zohoMailReader.readMail(userInfoObject,access_token);
      } else if (mail.endsWith("outlook.com")) {
        MicrosoftMailReader microsoftMailReader = new MicrosoftMailReader();
        responseArray = microsoftMailReader.readMail(userInfoObject,access_token);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(responseArray);
  }
}