import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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

public class MicrosoftMailReader extends MailReader{
  private static JSONArray responseArray;
  private static JSONArray searchResponseArray;
  private static final String MICROSOFT_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/messages/";
  // private static final String MICROSOFT_SEND_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/sendMail";

  public JSONArray readMail(JSONObject userInfoObject, String access_token) {

    JSONArray responseMessage = (JSONArray) userInfoObject.getJSONArray("value");

    System.out.println("Value : "+responseMessage);

    try {
      responseArray = new JSONArray();
      for (int i = 0; i < responseMessage.length(); i++) {
        JSONObject messageObject = responseMessage.getJSONObject(i);
        if(messageObject.has("from")){
          responseArray.put(getMessageObject(messageObject, i));
        }
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
    searchResponseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      JSONObject senderObject = messageObject.getJSONObject("sender");
      JSONObject emailAddressObject = senderObject.getJSONObject("emailAddress");
      if (emailAddressObject.getString("name").contains(searchValue)
          || emailAddressObject.getString("address").contains(searchValue)) {
            searchResponseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return searchResponseArray;
  }

  public JSONArray searchMailBySubject(JSONArray messages, String searchValue) {
    searchResponseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      if (messageObject.getString("subject").contains(searchValue)) {
        searchResponseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return searchResponseArray;
  }

  public JSONArray searchMailByContent(JSONArray messages, String searchValue) {
    searchResponseArray = new JSONArray();
    int k = 0;
    for (int i = 0; i < messages.length(); i++) {
      JSONObject messageObject = messages.getJSONObject(i);
      JSONObject bodyObject = messageObject.getJSONObject("body");
      if (bodyObject.getString("content").contains(searchValue)) {
        searchResponseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return searchResponseArray;
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

  public JSONObject composeMail(String to ,String subject, String contentType, String content ) {
    
    JSONObject messageObject = new JSONObject();
    JSONObject messageContentObject = new JSONObject();

    //subject
    messageContentObject.put("subject",subject);

    //body
    JSONObject bodyObject = new JSONObject();
    bodyObject.put("contentType",contentType);
    bodyObject.put("content",content);
    messageContentObject.put("body",bodyObject);

    //to address
    JSONArray toObjects = new JSONArray();
    JSONObject toObject = new JSONObject();
    JSONObject emailAddresssObject = new JSONObject();
    emailAddresssObject.put("address",to);
    toObject.put("emailAddress",emailAddresssObject);
    toObjects.put(toObject);
    messageContentObject.put("toRecipients",toObjects);


    messageObject.put("message",messageContentObject);
    return messageObject;
  }

  public JSONObject addAttachment(JSONObject messageObject , File[] files) {

    JSONArray attachmentsObject = new JSONArray();
    JSONObject messageContentObject = (JSONObject) messageObject.remove("Message");

    for(int i = 0; i<files.length;i++) {

      String filename = files[i].getName();
      System.out.println("Filename  : "+filename);

      byte[] contentBytes = null;
      try {
        contentBytes = Files.readAllBytes(Paths.get(filename));
        System.out.println("Bytes stored : "+contentBytes.toString());
      } catch (IOException e) {
        e.printStackTrace();
      }

      JSONObject attachmentObject = new JSONObject();
      attachmentObject.put("Name",filename);
      attachmentObject.put("ContentBytes",contentBytes.toString());
      attachmentsObject.put(attachmentObject);
    }

    messageContentObject.put("attachements",attachmentsObject);
    
    messageObject.put("Message",messageContentObject);

    return messageObject;
  }

  public void sendMail(String access_token ,String to ,String subject, String contentType, String content ,boolean hasAttachment , File... files) {

    JSONObject messageObject = composeMail(to,subject,contentType,content);

    System.out.println("Mail message object : "+messageObject);

    if(hasAttachment) 
      messageObject = addAttachment(messageObject , files);

    System.out.println("Response object : "+messageObject);
    // try {
    //   CloseableHttpClient client = HttpClients.createDefault();

    //   ResponseHandler<JSONObject> responseHandler = new ResponseHandler<JSONObject>() {

    //     @Override
    //     public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
    //       int statusCode = response.getStatusLine().getStatusCode();
    //       HttpEntity responseEntity = response.getEntity();
    //       System.out.println("Response entity : "+responseEntity);
    //       if (statusCode >= 300) {
    //         throw new HttpResponseException(statusCode,
    //             response.getStatusLine().getReasonPhrase());
    //       }
    //       JSONObject responseJSON = null;
    //       if(statusCode == 202){
    //         responseJSON = new JSONObject();
    //       }
    //       return responseJSON;
    //     }
    //   };

    //   HttpPost httpPost = new HttpPost(MICROSOFT_SEND_MESSAGE_ENDPOINT);
    //   httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
    //   httpPost.addHeader(HttpHeaders.CONTENT_TYPE,"application/json");

    //   StringEntity bodyContent = new StringEntity(messageObject.toString());

    //   httpPost.setEntity(bodyContent);
    //   JSONObject  responseObject = client.execute(httpPost,responseHandler);

    //   System.out.println("Response json object after sending mail : "+responseObject);

    // }
    // catch(Exception e) {
    //   e.printStackTrace();
    // }

  }



}