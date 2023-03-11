import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class RequestAccessToken extends HttpServlet{
  private static final String GMAIL_ACCESS_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
  private static final String GMAIL_CLIENT_ID = "842896570142-p2e2ir76rosjbifsarvh8f2g0161e5mc.apps.googleusercontent.com";
  private static final String GMAIL_CLIENT_SECRET = "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";

  private static final String ZOHO_ACCESS_TOKEN_ENDPOINT = "https://accounts.zoho.in/oauth/v2/token";
  private static final String ZOHO_CLIENT_ID = "1000.8ENBL33K5L6S3XEXZO3G79PN7U8F6D";
  private static final String ZOHO_CLIENT_SECRET = "c999cddffdfe8595aba81ab2967594f3e3c3c38cc0";
  
  private static final String GRANT_TYPE = "refresh_token";
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    String accessTokenEndpoint = null;
    String clientId = null;
    String clientSecret = null;

    String refreshToken = (String)req.getAttribute("refresh_token");
    System.out.println("Refresh tpken in request : "+refreshToken);
    String mail = (String)req.getAttribute("mail");

    if (mail.endsWith("gmail.com")) {
      accessTokenEndpoint = GMAIL_ACCESS_TOKEN_ENDPOINT;
      clientId = GMAIL_CLIENT_ID;
      clientSecret = GMAIL_CLIENT_SECRET;
    } else if (mail.endsWith("zohotest.com")) {
      accessTokenEndpoint = ZOHO_ACCESS_TOKEN_ENDPOINT;
      clientId = ZOHO_CLIENT_ID;
      clientSecret = ZOHO_CLIENT_SECRET;
    }

    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(accessTokenEndpoint);
      List<NameValuePair> params = new ArrayList<NameValuePair>();

      params.add(new BasicNameValuePair("client_id", clientId));
      params.add(new BasicNameValuePair("client_secret", clientSecret));
      params.add(new BasicNameValuePair("refresh_token", refreshToken));
      params.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
      httpPost.setEntity(new UrlEncodedFormEntity(params));

      ResponseHandler<JSONObject> responseHandler = new ResponseHandler<JSONObject>() {

        @Override
        public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int statusCode = response.getStatusLine().getStatusCode();
          HttpEntity responseEntity = response.getEntity();
          System.out.println("Status code : "+statusCode);
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

      JSONObject jsonObject = client.execute(httpPost, responseHandler);
      
      System.out.println("Response JSON : "+jsonObject);

      String access_token = (String) jsonObject.get("access_token");

      HttpSession session = req.getSession(false);
      session.setAttribute("access_token", access_token);
      UserDAO userDAO = new UserDAO();
      userDAO.updateAccessToken(access_token,mail);
      resp.sendRedirect("welcome.html");
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}