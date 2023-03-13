import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MicrosoftMailReader {
  private static JSONArray responseArray;
  private static final String MICROSOFT_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/messages/";

  public JSONArray readMail(JSONObject userInfoObject, String access_token) {
    JSONArray responseMessage = (JSONArray) userInfoObject.getJSONArray("value");

    try {
      responseArray = new JSONArray();
      for (int i = 0; i < responseMessage.length(); i++) {
        JSONObject messageObject = responseMessage.getJSONObject(i);
        responseArray.put(getMessageObject(messageObject, i));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return responseArray;
  }

  public JSONObject getMessageObject(JSONObject messageObject, int i) {

    JSONObject message = new JSONObject();
    JSONObject fromObject = messageObject.getJSONObject("from");
    message.put("from", fromObject.getJSONObject("emailAddress").getString("name"));
    message.put("subject", messageObject.getString("subject"));
    JSONObject bodyObject = messageObject.getJSONObject("body");
    message.put("stringContent", bodyObject.getString("content"));
    message.put("messageId", i);
    message.put("id", messageObject.getString("id"));
    message.put("date", messageObject.getString("receivedDateTime"));
    return message;
  }

  public JSONArray searchMailBySender(JSONArray messages, String searchValue) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      JSONObject senderObject = messageObject.getJSONObject("sender");
      JSONObject emailAddressObject = senderObject.getJSONObject("emailAddress");
      if (emailAddressObject.getString("name").contains(searchValue)
          || emailAddressObject.getString("address").contains(searchValue)) {
        responseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return responseArray;
  }

  public JSONArray searchMailBySubject(JSONArray messages, String searchValue) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      if (messageObject.getString("subject").contains(searchValue)) {
        responseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return responseArray;
  }

  public JSONArray searchMailByContent(JSONArray messages, String searchValue) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      JSONObject bodyObject = messageObject.getJSONObject("body");
      if (bodyObject.getString("content").contains(searchValue)) {
        responseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return responseArray;
  }

  public void markAllAsRead(String access_token) {

    
    System.out.println("Within mark all as Read function ");
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

      for (int i = 0; i < responseArray.length(); i++) {
        HttpPatch httpPatch = new HttpPatch(
            MICROSOFT_MESSAGE_ENDPOINT + responseArray.getJSONObject(i).getString("id"));
        httpPatch.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        httpPatch.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        JSONObject read = new JSONObject();
        read.put("isRead",true); 

        StringEntity params = new StringEntity(read.toString());
        httpPatch.setEntity(params);
        JSONObject  reponseJSON = client.execute(httpPatch,responseHandler);
        System.out.println("Respose object : "+reponseJSON);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}