import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ZOHOMailReader extends MailReader{
  private static final String ZOHO_MESSAGE_ENDPOINT = "https://mail.zoho.in/api/accounts";
  // private static final String ACCESS_TOKEN_ENDPOINT = "https://accounts.zoho.in/oauth/v2/token";
  // private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

  private static JSONArray responseArray;
  private static String accountId;

  public void setAccountId(String accountId) {
    ZOHOMailReader.accountId = accountId;
  }

  public String getAccountId() {
    return ZOHOMailReader.accountId;
  }

  public JSONArray readMail(JSONObject userInfoObject, String access_token) {

    try {

      CloseableHttpClient client = HttpClients.createDefault();
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

      JSONArray dataArray = userInfoObject.getJSONArray("data");
      JSONObject dataObject = dataArray.getJSONObject(0);
      accountId = dataObject.getString("accountId");

      HttpGet httpGet = new HttpGet(ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages/view");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      JSONObject mailList = client.execute(httpGet, responseHandler);

      responseArray = new JSONArray();

      JSONArray mailsObject = mailList.getJSONArray("data");
      for (int i = 0; i < mailsObject.length(); i++) {
        JSONObject responseObject = new JSONObject();
        JSONObject message = mailsObject.getJSONObject(i);
        String messageId = message.getString("messageId");
        String folderId = message.getString("folderId");
        String subject = message.getString("subject");
        String From = message.getString("sender");
        long date = Long.parseLong(message.getString("receivedTime"));
        Date stringdate = new Date(date);

        httpGet = new HttpGet(
            ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/folders/" + folderId + "/messages/" + messageId + "/content");
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        JSONObject mailContentObject = client.execute(httpGet, responseHandler);
        JSONObject contentObject = mailContentObject.getJSONObject("data");
        String content = contentObject.getString("content");

        responseObject.put("stringContent", content);
        responseObject.put("messageId", i);
        responseObject.put("from", From);
        responseObject.put("date", stringdate.toString());
        responseObject.put("subject", subject);

        responseArray.put(responseObject);
        System.out.println("Message status : "+message.getString("status"));


      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println();
    return responseArray;
  }

  public JSONObject getMessageObject(JSONObject message, int id, String access_token, String accountId) {

    JSONObject messageObject = new JSONObject();

    try {
      CloseableHttpClient client = HttpClients.createDefault();
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

      String messageId = message.getString("messageId");
      String folderId = message.getString("folderId");
      String subject = message.getString("subject");
      String From = message.getString("sender");
      long date = Long.parseLong(message.getString("receivedTime"));
      Date stringdate = new Date(date);

      HttpGet httpGet = new HttpGet(
          ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/folders/" + folderId + "/messages/" + messageId + "/content");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      JSONObject mailContentObject = client.execute(httpGet, responseHandler);
      JSONObject contentObject = mailContentObject.getJSONObject("data");
      String content = contentObject.getString("content");

      messageObject.put("stringContent", content);
      messageObject.put("messageId", id);
      messageObject.put("from", From);
      messageObject.put("date", stringdate.toString());
      messageObject.put("subject", subject);
      messageObject.put("id", messageId);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return messageObject;
  }

  public JSONArray searchMailBySender(JSONArray messages, String searchValue, String access_token, String accountId) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      if (messageObject.getString("sender").contains(searchValue)) {
        responseArray.put(getMessageObject(messageObject, k++, access_token, accountId));
        break;
      }
    }
    return responseArray;
  }

  public JSONArray searchMailBySubject(JSONArray messages, String searchValue, String access_token, String accountId) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      if (messageObject.getString("subject").contains(searchValue)) {
        responseArray.put(getMessageObject(messageObject, k++, access_token, accountId));
        break;
      }
    }
    return responseArray;
  }

  public JSONArray searchMailByContent(JSONArray messages, String searchValue, String access_token, String accountId) {
    responseArray = new JSONArray();
    int k = 0;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
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

      HttpGet httpGet = new HttpGet(
          ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages/search");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      List<NameValuePair> nameValuePairs = new ArrayList<>();
      if(searchValue == ""){
        searchValue = "\0";
      }
      nameValuePairs.add(new BasicNameValuePair("searchKey", "content:" + searchValue));
      URI uri = new URIBuilder(httpGet.getURI())
          .addParameters(nameValuePairs)
          .build();
      httpGet.setURI(uri);
      JSONObject mailContentObject = client.execute(httpGet, responseHandler);

      JSONArray dataObject = mailContentObject.getJSONArray("data");
      for (int i = 0; i < dataObject.length(); i++) {
        JSONObject object = dataObject.getJSONObject(i);
        responseArray.put(getMessageObject(object, k++, access_token, accountId));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return responseArray;
  }

  public void markAllAsRead(String access_token) {
      try {
        CloseableHttpClient client = HttpClients.createDefault();
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
  
        HttpPut httpPut = new HttpPut(
            ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/updatemessage");
        httpPut.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        
        JSONArray messageIdArray = new JSONArray();
        for(int i = 0; i<responseArray.length(); i++ ) {
          messageIdArray.put(responseArray.getJSONObject(i).getString("id"));
        }
        JSONObject requestJSON = new JSONObject();
        requestJSON.put("mode","markAsRead");
        requestJSON.put("messageId",messageIdArray);

        StringEntity requestBody = new StringEntity(requestJSON.toString());
        httpPut.setEntity(requestBody);
        JSONObject responseObject = client.execute(httpPut, responseHandler);
        System.out.println("Mark as read response : "+responseObject);
  
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  public JSONObject composeMail(String from ,String to ,String subject, String contentType, String content ) {
    JSONObject messageObject = new JSONObject();

    //subject
    messageObject.put("subject",subject);

    System.out.println("From address : "+from);
    
    //from
    from = from.split("@")[0] + "@zohomail.in";
    System.out.println("From address : "+from);
    messageObject.put("fromAddress",from);

    //body
    messageObject.put("content",content);

    //to address
    messageObject.put("toAddress",to);
    return messageObject;
  }

  public void sendMail(String access_token ,String from ,String to ,String subject, String contentType, String content ) {

    JSONObject messageObject = composeMail(from,to,subject,contentType,content);

    try {
      CloseableHttpClient client = HttpClients.createDefault();

      ResponseHandler<JSONObject> responseHandler = new ResponseHandler<JSONObject>() {

        @Override
        public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int statusCode = response.getStatusLine().getStatusCode();
          HttpEntity responseEntity = response.getEntity();
          System.out.println("Response entity : "+responseEntity);
          System.out.println("Response code : "+statusCode);
          // if (statusCode >= 300) {
          //   throw new HttpResponseException(statusCode,
          //       response.getStatusLine().getReasonPhrase());
          // }
          JSONObject responseJSON = null;
          if(statusCode == 202){
            responseJSON = new JSONObject();
          }
          else {
            ContentType contentType = ContentType.get(responseEntity);
            Charset charset = contentType.getCharset();
            String json = EntityUtils.toString(responseEntity, charset);
            responseJSON = new JSONObject(json);
          }
          return responseJSON;
        }
      };

      HttpPost httpPost = new HttpPost(ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages");
      httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

      StringEntity bodyContent = new StringEntity(messageObject.toString());

      httpPost.setEntity(bodyContent);
      JSONObject  responseObject = client.execute(httpPost,responseHandler);

      System.out.println("Response : "+responseObject);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

}
