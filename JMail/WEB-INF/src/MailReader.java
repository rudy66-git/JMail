import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

abstract public class MailReader{

  private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
  private static final String ACCESS_TOKEN_GRANT_TYPE = "authorization_code";
  private static final String REDIRECT_URI = "http://localhost:8080/JMail/callback";


  String requestAccessToken(String mail,JSONObject appCredentials , String endpoint) {
    String access_token = null;

    UserDAO userDAO = new UserDAO();
    String refresh_token = userDAO.getRefreshToken(mail);
    
    String clientId = appCredentials.getString("clientId");
    String clientSecret = appCredentials.getString("clientSecret");
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(endpoint);
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      params.add(new BasicNameValuePair("client_id", clientId));
      params.add(new BasicNameValuePair("client_secret", clientSecret));
      params.add(new BasicNameValuePair("refresh_token", refresh_token));
      params.add(new BasicNameValuePair("grant_type", REFRESH_TOKEN_GRANT_TYPE));
      httpPost.setEntity(new UrlEncodedFormEntity(params));

      JSONObject jsonObject = client.execute(httpPost, myResponseHandler);
      access_token = jsonObject.getString("access_token");
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return access_token;
  }

  public JSONObject getAccessToken(String mail,JSONObject appCredentials,String endPoint,String code,String scope) {
    JSONObject responseObject = null;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(endPoint);
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      String clientId = appCredentials.getString("clientId");
      String clientSecret = appCredentials.getString("clientSecret");

      params.add(new BasicNameValuePair("code", code));
      params.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
      params.add(new BasicNameValuePair("client_id", clientId));
      params.add(new BasicNameValuePair("client_secret", clientSecret));
      params.add(new BasicNameValuePair("grant_type", ACCESS_TOKEN_GRANT_TYPE));
      if(mail.endsWith("outlook.com"))
        params.add(new BasicNameValuePair("scope", scope));
      httpPost.setEntity(new UrlEncodedFormEntity(params));

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      responseObject = client.execute(httpPost, myResponseHandler);
    } catch(Exception e) {
      e.printStackTrace();
    }
    return responseObject;
  } 

  public JSONObject getMessageObject(String access_token,String endpoint) {
    JSONObject responseObject = null;

    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(endpoint);

      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      responseObject = client.execute(httpGet, myResponseHandler);
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    return responseObject;
  }

  abstract JSONArray readMail(JSONObject responseObject , String token);
  abstract JSONArray searchMail(JSONObject responseObject , String token,String option , String searchValue , boolean unReadStatus);


} 