import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
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

public class GMailReader {
  private static final String GMAIL_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
  private static JSONArray responseArray;

  public JSONArray readMail(JSONObject messageListObject, String access_token) {

    JSONArray responseMessage = (JSONArray) messageListObject.get("messages");

    try {

      CloseableHttpClient client = HttpClients.createDefault();
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
      responseArray = new JSONArray();
      for (int i = 0; i < responseMessage.length(); i++) {
  
        JSONObject message = new JSONObject();
        HttpGet httpGet = new HttpGet(GMAIL_MESSAGE_ENDPOINT + responseMessage.getJSONObject(i).getString("id"));
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
  
        JSONObject messageObject = client.execute(httpGet, responseHandler);
        System.out.println("Message object : " + messageObject);
        JSONObject payloadObject = messageObject.getJSONObject("payload");
        JSONArray headersObject = payloadObject.getJSONArray("headers");
  
        for (int j = 0; j < headersObject.length(); j++) {
          if (headersObject.getJSONObject(j).getString("name").equals("From")) {
            System.out.println("(" + j + ") From : " + headersObject.getJSONObject(j).getString("value"));
            message.put("from", headersObject.getJSONObject(j).getString("value"));
          }
          if (headersObject.getJSONObject(j).getString("name").equals("Subject")) {
            System.out.println("(" + j + ") Subject : " + headersObject.getJSONObject(j).getString("value"));
            message.put("subject", headersObject.getJSONObject(j).getString("value"));
          }
          if (headersObject.getJSONObject(j).getString("name").equals("Date")) {
            System.out.println("(" + j + ") Date : " + headersObject.getJSONObject(j).getString("value"));
            message.put("date", headersObject.getJSONObject(j).getString("value"));
          }
        }
        JSONObject body = null;
        if (payloadObject.has("parts")) {
          JSONArray partsObject = payloadObject.getJSONArray("parts");
          JSONObject part1 = partsObject.getJSONObject(0);
          body = part1.getJSONObject("body");
        } else {
          body = payloadObject.getJSONObject("body");
        }
  
        if(body.has("data")) {
          String base64Content = body.getString("data");
          Base64.Decoder decoder = Base64.getUrlDecoder();
          String content = new String(decoder.decode(base64Content));
          message.put("stringContent", content);
        }
        else {
          message.put("stringContent","No content in the mail body!");
        }
  
        message.put("messageId", i);
        responseArray.put(message);
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return responseArray;
  }

  public void markAllAsRead(String access_token) {

    CloseableHttpClient client = HttpClients.createDefault();
    
    try {
      for(int i = 0;i<responseArray.length(); i++) {
        HttpPost httpPost = new HttpPost(GMAIL_MESSAGE_ENDPOINT+responseArray.getJSONObject(i).getString("id")+"/modify");
        httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        JSONArray unreadJson = new JSONArray();
        unreadJson.put("UNREAD");
        params.add(new BasicNameValuePair("removeLabelIds",unreadJson.toString()));
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        client.execute(httpPost);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JSONObject getMessageObject(JSONObject messageObject, int messageId) {

    JSONObject message = new JSONObject();

    JSONObject payloadObject = messageObject.getJSONObject("payload");
      JSONArray headersObject = payloadObject.getJSONArray("headers"); 

      for(int j = 0;j<headersObject.length(); j++) {
        if(headersObject.getJSONObject(j).getString("name").equals("From")){
          System.out.println("("+j+") From : "+headersObject.getJSONObject(j).getString("value"));
          message.put("from", headersObject.getJSONObject(j).getString("value"));
        }
        if(headersObject.getJSONObject(j).getString("name").equals("Subject")){
          System.out.println("("+j+") Subject : "+headersObject.getJSONObject(j).getString("value"));
          message.put("subject", headersObject.getJSONObject(j).getString("value"));
        }
        if(headersObject.getJSONObject(j).getString("name").equals("Date")){
          System.out.println("("+j+") Date : "+headersObject.getJSONObject(j).getString("value"));
          message.put("date", headersObject.getJSONObject(j).getString("value"));
        }
      }
      JSONObject body = null;
      if(payloadObject.has("parts")){
        JSONArray partsObject = payloadObject.getJSONArray("parts");
        JSONObject part1 = partsObject.getJSONObject(0);
        body = part1.getJSONObject("body");
      }
      else {
        body = payloadObject.getJSONObject("body");
      }
      String base64Content = body.getString("data");
      
      Base64.Decoder decoder = Base64.getUrlDecoder();  
      String content = new String(decoder.decode(base64Content));
			System.out.println("decodedmail : " + content);
      message.put("stringContent", content);
      message.put("messageId", messageId);
      message.put("id",messageObject.getString("threadId"));
    return message;
  }

  public JSONArray searchMailBySender(JSONArray messages, String searchValue, boolean unReadStatus) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {

      JSONObject messageObject = messages.getJSONObject(i);
      JSONArray labelObject = messageObject.getJSONArray("labelIds");

      // read Or Unread
      boolean readOrNot = labelObject.toString().contains("UNREAD") ? true : false;

      JSONObject payloadObject = messageObject.getJSONObject("payload");
      JSONArray headersObject = payloadObject.getJSONArray("headers");
      for (int j = 0; j < headersObject.length(); j++) {
        if (headersObject.getJSONObject(j).getString("name").equals("From")
            && headersObject.getJSONObject(j).getString("value").contains(searchValue) && unReadStatus == readOrNot) {
          responseArray.put(getMessageObject(messageObject,k++));
          break;
        }
      }

    }
    return responseArray;
  }

  public JSONArray searchMailBySubject(JSONArray messages, String searchValue, boolean unReadStatus) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {

      JSONObject messageObject = messages.getJSONObject(i);
      JSONArray labelObject = messageObject.getJSONArray("labelIds");

      // read Or Unread
      boolean readOrNot = labelObject.toString().contains("UNREAD") ? true : false;

      JSONObject payloadObject = messageObject.getJSONObject("payload");
      JSONArray headersObject = payloadObject.getJSONArray("headers");
      for (int j = 0; j < headersObject.length(); j++) {
        if (headersObject.getJSONObject(j).getString("name").equals("Subject")
            && headersObject.getJSONObject(j).getString("value").contains(searchValue) && unReadStatus == readOrNot) {
          responseArray.put(getMessageObject(messageObject,k++));
          break;
        }
      }
    }
    return responseArray;
  }

  public JSONArray searchMailByContent(JSONArray messages, String searchValue, boolean unReadStatus) {
    responseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {

      JSONObject messageObject = messages.getJSONObject(i);
      JSONArray labelObject = messageObject.getJSONArray("labelIds");

      // read Or Unread
      boolean readOrNot = labelObject.toString().contains("UNREAD") ? true : false;

      JSONObject payloadObject = messageObject.getJSONObject("payload");
      JSONObject body = null;
      if (payloadObject.has("parts")) {
        JSONArray partsObject = payloadObject.getJSONArray("parts");
        JSONObject part1 = partsObject.getJSONObject(0);
        body = part1.getJSONObject("body");
      } else {
        body = payloadObject.getJSONObject("body");
      }

      String base64Content = body.getString("data");

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String content = new String(decoder.decode(base64Content));
      if(content.contains(searchValue) && readOrNot == unReadStatus) {
        responseArray.put(getMessageObject(messageObject,k++));
      }
    }
    return responseArray;
  }

}