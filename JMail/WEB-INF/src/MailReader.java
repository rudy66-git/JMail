import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
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

      params.add(new BasicNameValuePair("client_id", clientId));
      params.add(new BasicNameValuePair("client_secret", clientSecret));
      params.add(new BasicNameValuePair("refresh_token", refresh_token));
      params.add(new BasicNameValuePair("grant_type", REFRESH_TOKEN_GRANT_TYPE));
      httpPost.setEntity(new UrlEncodedFormEntity(params));

      ResponseHandler<JSONObject> responseHandler = new ResponseHandler<JSONObject>() {
        @Override
        public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int statusCode = response.getStatusLine().getStatusCode();
          HttpEntity responseEntity = response.getEntity();
          System.out.println("Response code : "+statusCode);
          if (statusCode >= 300) {
            throw new HttpResponseException(statusCode,
                response.getStatusLine().getReasonPhrase());
          }
          if (responseEntity == null) {
            throw new ClientProtocolException("No content in response");
          }

          String json = EntityUtils.toString(responseEntity, ContentType.get(responseEntity).getCharset());
          JSONObject responseJSON = new JSONObject(json);
          return responseJSON;
        }
      };

      JSONObject jsonObject = client.execute(httpPost, responseHandler);
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

      responseObject = client.execute(httpPost, responseHandler);
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

      responseObject = client.execute(httpGet, responseHandler);
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    return responseObject;
  }

  abstract JSONArray readMail(JSONObject responseObject , String token);


} 