import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

public class GMailReader extends MailReader {
  private static final String GMAIL_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
  private static final String GMAIL_SEND_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
  private static JSONArray responseArray;

  public JSONArray readMail(JSONObject messageListObject, String access_token) {

    JSONArray responseMessage = (JSONArray) messageListObject.get("messages");

    try {

      CloseableHttpClient client = HttpClients.createDefault();

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();
      responseArray = new JSONArray();
      for (int i = 0; i < 1; i++) {

        JSONObject message = new JSONObject();
        HttpGet httpGet = new HttpGet(GMAIL_MESSAGE_ENDPOINT + responseMessage.getJSONObject(i).getString("id"));
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

        JSONObject messageObject = client.execute(httpGet, myResponseHandler);

        System.out.println();
        System.out.println("Message object " + i + " : " + messageObject);
        System.out.println();

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

        if (body.has("data")) {
          String base64Content = body.getString("data");
          Base64.Decoder decoder = Base64.getUrlDecoder();
          String content = new String(decoder.decode(base64Content));
          message.put("stringContent", content);
        } else {
          message.put("stringContent", "No content in the mail body!");
        }

        message.put("messageId", i);
        responseArray.put(message);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return responseArray;
  }

  public void markAllAsRead(String access_token) {

    CloseableHttpClient client = HttpClients.createDefault();
    HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();
    try {
      for (int i = 0; i < responseArray.length(); i++) {
        HttpPost httpPost = new HttpPost(
            GMAIL_MESSAGE_ENDPOINT + responseArray.getJSONObject(i).getString("id") + "/modify");
        httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        JSONArray unreadJson = new JSONArray();
        unreadJson.put("UNREAD");
        params.add(new BasicNameValuePair("removeLabelIds", unreadJson.toString()));
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        client.execute(httpPost, myResponseHandler);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JSONObject getMessageObject(JSONObject messageObject, int messageId) {

    JSONObject message = new JSONObject();

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
    String base64Content = body.getString("data");

    Base64.Decoder decoder = Base64.getUrlDecoder();
    String content = new String(decoder.decode(base64Content));
    System.out.println("decodedmail : " + content);
    message.put("stringContent", content);
    message.put("messageId", messageId);
    message.put("id", messageObject.getString("threadId"));
    return message;
  }

  public JSONArray searchMail(JSONObject mailList, String access_token, String option, String searchValue,
      boolean unReadStatus) {

    JSONArray searchRespArray = null;

    JSONArray responseMessage = (JSONArray) mailList.get("messages");
    JSONArray gmailMessages = new JSONArray();

    try {

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = null;

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      for (int i = 0; i < responseMessage.length(); i++) {
        httpGet = new HttpGet(GMAIL_MESSAGE_ENDPOINT + responseMessage.getJSONObject(i).getString("id"));
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        JSONObject messageObject = client.execute(httpGet, myResponseHandler);
        gmailMessages.put(messageObject);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (option.equals("Sender"))
      searchRespArray = searchMailBySender(gmailMessages, searchValue, unReadStatus);
    else if (option.equals("Subject"))
      searchRespArray = searchMailBySubject(gmailMessages, searchValue, unReadStatus);
    else
      searchRespArray = searchMailByContent(gmailMessages, searchValue, unReadStatus);

    return searchRespArray;
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
          responseArray.put(getMessageObject(messageObject, k++));
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
          responseArray.put(getMessageObject(messageObject, k++));
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
      if (content.contains(searchValue) && readOrNot == unReadStatus) {
        responseArray.put(getMessageObject(messageObject, k++));
      }
    }
    return responseArray;
  }

  public void sendMail(String access_token, String mail, String to, String subject, String contentType, String content,
      boolean hasAttachment, List<File> files, List<String> contentTypes) {

    String rawMessageString = null;
    if (hasAttachment) {
      rawMessageString = addAttachments(rawMessageString, to, subject, contentType, content, files, contentTypes);
    } else {
      rawMessageString = composeRawMessage(to, subject, contentType, content);
    }

    System.out.println("Encoded message : " + rawMessageString);

    try {

      CloseableHttpClient client = HttpClients.createDefault();

      HttpPost httpPost = new HttpPost(GMAIL_SEND_MESSAGE_ENDPOINT);
      httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("raw", rawMessageString);
      System.out.println("Json string : " + jsonObject.toString());
      StringEntity requestEntity = new StringEntity(jsonObject.toString());

      List<NameValuePair> params = new ArrayList<NameValuePair>();

      params.add(new BasicNameValuePair("raw", rawMessageString));
      httpPost.setEntity(requestEntity);

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      JSONObject responseObject = client.execute(httpPost, myResponseHandler);
      System.out.println("Response json : " + responseObject);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String addAttachments(String rawMessageString, String to, String subject, String contentType, String content,
      List<File> files, List<String> contentTypes) {

    String encodedContent = Base64.getEncoder().encodeToString(content.getBytes());
    rawMessageString = "Subject: " + subject + "\nTo: " + to
        + "\nContent-Type: multipart/mixed; boundary=\"multipart-content\"\n\n--multipart-content\nContent-Type: multipart/alternative; boundary=\"multipart-alternative\"\n\n--multipart-alternative\nContent-Type: text/plain; charset=\"UTF-8\"\nContent-Transfer-Encoding: base64\n\n"
        + encodedContent + "\n--multipart-alternative--";

    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      String filename = file.getName();
      String path = file.getPath();
      byte[] contentBytes = null;
      try {
        contentBytes = Files.readAllBytes(Paths.get(path));
      } catch (Exception e) {
        e.printStackTrace();
      }

      String encodedBytes = Base64.getEncoder().encodeToString(contentBytes);
      rawMessageString += "\n\n--multipart-content\nContent-Type: text/plain; name=\"" + filename
          + "\"\nContent-Disposition: attachment; filename=\"" + filename + "\"\nContent-Transfer-Encoding: base64\n\n"
          + encodedBytes;
    }
    rawMessageString += "\n--multipart-content--";
    rawMessageString = Base64.getEncoder().encodeToString(rawMessageString.getBytes());

    return rawMessageString;
  }

  private String composeRawMessage(String to, String subject, String contentType, String content) {
    String rawMessage = null;

    content = Base64.getEncoder().encodeToString(content.getBytes());
    rawMessage = "Subject: " + subject + "\nTo: " + to + "\nContent-Type: " + contentType
        + "\nContent-Transfer-Encoding: base64\n\n" + content;

    rawMessage = Base64.getEncoder().encodeToString(rawMessage.getBytes());

    return rawMessage;
  }

}