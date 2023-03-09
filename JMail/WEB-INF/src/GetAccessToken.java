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

public class GetAccessToken extends HttpServlet {
  private static final String CLIENT_ID = "842896570142-brf7lciu8g137lqv0i0ghpkk8lsjuho5.apps.googleusercontent.com";
  private static final String CLIENT_SECRET = "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";
  private static final String CLIENT_ID_WEB = "842896570142-p2e2ir76rosjbifsarvh8f2g0161e5mc.apps.googleusercontent.com";
  private static final String CLIENT_SECRET_WEB = "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";
  private static final String ACCESS_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
  private static final String REDIRECT_URI = "http://localhost:8080/JMail/callback";
  // private static final String REDIRECT_URI = "http://localhost";
  private static final String SCOPE = "https://mail.google.com/";
  private static final String GRANT_TYPE = "authorization_code";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    String code = (String)req.getAttribute("code");
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(ACCESS_TOKEN_ENDPOINT);
      List<NameValuePair> params = new ArrayList<NameValuePair>();

      params.add(new BasicNameValuePair("code", code));
      params.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
      params.add(new BasicNameValuePair("client_id", CLIENT_ID_WEB));
      params.add(new BasicNameValuePair("client_secret", CLIENT_SECRET_WEB));
      params.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
      // params.add(new BasicNameValuePair("scope", SCOPE));

      // code=4%2F0AWtgzh4_oZS8gntt4k_a0eSkQn1BdZqWYRq2lFukt11hYdGpJlS0q6eTNn2maeqUiWhI3g
      // redirect_uri=https%3A%2F%2Fdevelopers.google.com%2Foauthplayground
      // client_id=407408718192.apps.googleusercontent.com
      // client_secret=************
      // grant_type=authorization_code
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
      System.out.println("Response : "+jsonObject.toString());
      String accessToken = (String) jsonObject.get("access_token");
      String refreshToken = (String) jsonObject.get("refresh_token");

      HttpSession session = req.getSession(false);
      String mail = (String) session.getAttribute("mail");
      session.setAttribute("access_token", accessToken);

      UserDAO userDAO = new UserDAO();
      System.out.println("Insertion result :"+ userDAO.storeAuthInfo(mail,accessToken , refreshToken));

      if( accessToken != null ){
        resp.sendRedirect("welcome.html");
      }
      else {
        resp.sendRedirect("index.html");
      }
      
    }
     catch (Exception e) {
      e.printStackTrace();
    }
  }
}
