import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

public class MicrosoftMailReader extends MailReader {

  private static JSONArray responseArray;
  private static JSONArray searchResponseArray;
  private static final String UPLOAD_DIRECTORY = "C:/Program Files/Apache Software Foundation/Tomcat 9.0/webapps//JMail/upload/";
  private static final String MICROSOFT_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/messages/";
  private static final String MICROSOFT_SEND_MESSAGE_ENDPOINT = "https://graph.microsoft.com/v1.0/me/sendMail";

  public JSONArray readMail(JSONObject userInfoObject, String access_token) {

    JSONArray responseMessage = (JSONArray) userInfoObject.getJSONArray("value");

    try {
      responseArray = new JSONArray();
      for (int i = 0; i < responseMessage.length(); i++) {
        JSONObject messageObject = responseMessage.getJSONObject(i);
        System.out.println();
        System.out.println(i + " Message object : " + messageObject);
        System.out.println();
        if (messageObject.has("from")) {
          JSONObject responseObject = getMessageObject(messageObject, i);
          String id = messageObject.getString("id");
          if (messageObject.getBoolean("hasAttachments")) {
            responseObject.put("hasAttachments", true);
            responseObject = getAttachments(responseObject, access_token, id);
          } else {
            responseObject.put("hasAttachments", false);
          }
          responseArray.put(responseObject);
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return responseArray;
  }

  private JSONObject getAttachments(JSONObject messageObject, String access_token, String id) {
    try {

      CloseableHttpClient client = HttpClients.createDefault();

      HttpGet httpGet = new HttpGet(MICROSOFT_MESSAGE_ENDPOINT + id + "/attachments");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      JSONObject responseObject = client.execute(httpGet, myResponseHandler);

      JSONArray attachments = responseObject.getJSONArray("value");

      for (int i = 0; i < attachments.length(); i++) {

        JSONObject attachmentObject = attachments.getJSONObject(i);

        String fileName = attachmentObject.getString("name");
        String contentBytes = attachmentObject.getString("contentBytes");
        byte[] bytes = Base64.getDecoder().decode(contentBytes);

        try (OutputStream stream = new FileOutputStream(UPLOAD_DIRECTORY + fileName)) {
          stream.write(bytes);
        }
        File file = new File(UPLOAD_DIRECTORY + fileName);
        messageObject.put("file" + i, file);
      }

      return messageObject;

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public JSONArray searchMail(JSONObject mailList, String access_token, String option, String searchValue,
      boolean unReadStatus) {
    JSONArray searchRespArray = null;
    JSONArray mailsObject = mailList.getJSONArray("value");
    JSONArray messages = new JSONArray();
    for (int i = 0; i < mailsObject.length(); i++) {
      JSONObject messageObject = mailsObject.getJSONObject(i);
      if ((messageObject.getBoolean("isRead") && !(unReadStatus))
          || ((!messageObject.getBoolean("isRead")) && unReadStatus)) {
        messages.put(messageObject);
      }
    }
    if (option.equals("Sender")) {
      searchRespArray = searchMailBySender(messages, searchValue);
    } else if (option.equals("Subject")) {
      searchRespArray = searchMailBySubject(messages, searchValue);
    } else {
      searchRespArray = searchMailByContent(messages, searchValue);
    }
    return searchRespArray;

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

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      for (int i = 0; i < responseArray.length(); i++) {
        HttpPatch httpPatch = new HttpPatch(
            MICROSOFT_MESSAGE_ENDPOINT + responseArray.getJSONObject(i).getString("id"));
        httpPatch.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        httpPatch.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        JSONObject read = new JSONObject();
        read.put("isRead", true);

        StringEntity params = new StringEntity(read.toString());
        httpPatch.setEntity(params);
        JSONObject reponseJSON = client.execute(httpPatch, myResponseHandler);
        System.out.println("Respose object : " + reponseJSON);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JSONObject composeMail(String to, String subject, String contentType, String content) {

    JSONObject messageObject = new JSONObject();
    JSONObject messageContentObject = new JSONObject();

    // subject
    messageContentObject.put("subject", subject);

    // body
    JSONObject bodyObject = new JSONObject();
    bodyObject.put("contentType", contentType);
    bodyObject.put("content", content);
    messageContentObject.put("body", bodyObject);

    // to address
    JSONArray toObjects = new JSONArray();
    JSONObject toObject = new JSONObject();
    JSONObject emailAddresssObject = new JSONObject();
    emailAddresssObject.put("address", to);
    toObject.put("emailAddress", emailAddresssObject);
    toObjects.put(toObject);
    messageContentObject.put("toRecipients", toObjects);

    messageObject.put("message", messageContentObject);
    return messageObject;
  }

  String encodeFileContent(byte[] contentBytes) {

    String encodedContent = Base64.getEncoder().encodeToString(contentBytes);
    return encodedContent;
  }

  public JSONObject addAttachment(JSONObject messageObject, List<File> files, List<String> contentTypes) {

    JSONArray attachmentsObject = new JSONArray();
    JSONObject messageContentObject = (JSONObject) messageObject.remove("message");
    try {
      for (int i = 0; i < files.size(); i++) {

        String filename = files.get(i).getName();
        String path = files.get(i).getPath();

        byte[] contentBytes = null;
        contentBytes = Files.readAllBytes(Paths.get(path));

        String encodedBytes = encodeFileContent(contentBytes);

        JSONObject attachmentObject = new JSONObject();
        attachmentObject.put("@odata.type", "#microsoft.graph.fileAttachment");
        attachmentObject.put("name", filename);
        attachmentObject.put("contentType", contentTypes.get(i));
        attachmentObject.put("contentBytes", encodedBytes);
        attachmentsObject.put(attachmentObject);
        files.get(i).deleteOnExit();
      }

      messageContentObject.put("attachments", attachmentsObject);
      messageObject.put("message", messageContentObject);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return messageObject;
  }

  public void sendMail(String access_token, String to, String subject, String contentType, String content,
      boolean hasAttachment, List<File> files, List<String> contentTypes) {

    JSONObject messageObject = composeMail(to, subject, contentType, content);

    if (hasAttachment)
      messageObject = addAttachment(messageObject, files, contentTypes);

    System.out.println("Response object : " + messageObject);
    try {
      CloseableHttpClient client = HttpClients.createDefault();

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      HttpPost httpPost = new HttpPost(MICROSOFT_SEND_MESSAGE_ENDPOINT);
      httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

      StringEntity bodyContent = new StringEntity(messageObject.toString());

      httpPost.setEntity(bodyContent);
      JSONObject responseObject = client.execute(httpPost, myResponseHandler);

      System.out.println("Response json object after sending mail :" + responseObject);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}