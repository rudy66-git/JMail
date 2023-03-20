import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;


public class ZOHOMailReader extends MailReader {

  private static final String ZOHO_MESSAGE_ENDPOINT = "https://mail.zoho.in/api/accounts";
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
      
      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();
      JSONArray dataArray = userInfoObject.getJSONArray("data");
      JSONObject dataObject = dataArray.getJSONObject(0);
      accountId = dataObject.getString("accountId");

      HttpGet httpGet = new HttpGet(ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages/view");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      JSONObject mailList = client.execute(httpGet, myResponseHandler);

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
        JSONObject mailContentObject = client.execute(httpGet, myResponseHandler);
        JSONObject contentObject = mailContentObject.getJSONObject("data");
        String content = contentObject.getString("content");

        responseObject.put("stringContent", content);
        responseObject.put("messageId", i);
        responseObject.put("from", From);
        responseObject.put("date", stringdate.toString());
        responseObject.put("subject", subject);

        responseArray.put(responseObject);
        System.out.println("Message status : " + message.getString("status"));

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
      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      String messageId = message.getString("messageId");
      String folderId = message.getString("folderId");
      String subject = message.getString("subject");
      String From = message.getString("sender");
      long date = Long.parseLong(message.getString("receivedTime"));
      Date stringdate = new Date(date);

      HttpGet httpGet = new HttpGet(
          ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/folders/" + folderId + "/messages/" + messageId + "/content");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      JSONObject mailContentObject = client.execute(httpGet, myResponseHandler);
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

  public JSONArray searchMail(JSONObject mailList , String access_token,String option , String searchValue , boolean unReadStatus) {

    JSONArray searchRespArray = null;

    try {
      
      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = null;

      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

        httpGet = new HttpGet( ZOHO_MESSAGE_ENDPOINT+ "/" + accountId + "/messages/view");
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
        JSONObject zohoMailList = client.execute(httpGet, myResponseHandler);
        JSONArray mailsObject = zohoMailList.getJSONArray("data");
        JSONArray messages = new JSONArray();
        for (int i = 0; i < mailsObject.length(); i++) {
          JSONObject messageObject = mailsObject.getJSONObject(i);
          if ((messageObject.getString("status").equals("1") && !(unReadStatus))
              || (messageObject.getString("status").equals("0") && unReadStatus)) {
            messages.put(messageObject);
          }
        }

        if (option.equals("Sender")) {
          searchRespArray = searchMailBySender(messages, searchValue, access_token);
        } else if (option.equals("Subject")) {
          searchRespArray = searchMailBySubject(messages, searchValue, access_token);
        } else
        searchRespArray = searchMailByContent(messages, searchValue, access_token);


    } catch (Exception e) {
      e.printStackTrace();
    }


    return searchRespArray;

  }

  public JSONArray searchMailBySender(JSONArray messages, String searchValue, String access_token) {
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

  public JSONArray searchMailBySubject(JSONArray messages, String searchValue, String access_token) {
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

  public JSONArray searchMailByContent(JSONArray messages, String searchValue, String access_token) {
    responseArray = new JSONArray();
    int k = 0;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      HttpGet httpGet = new HttpGet(
          ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages/search");
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      List<NameValuePair> nameValuePairs = new ArrayList<>();
      if (searchValue == "") {
        searchValue = "\0";
      }
      nameValuePairs.add(new BasicNameValuePair("searchKey", "content:" + searchValue));
      JSONObject mailContentObject = client.execute(httpGet, myResponseHandler);

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
      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      HttpPut httpPut = new HttpPut(
          ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/updatemessage");
      httpPut.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

      JSONArray messageIdArray = new JSONArray();
      for (int i = 0; i < responseArray.length(); i++) {
        messageIdArray.put(responseArray.getJSONObject(i).getString("id"));
      }
      JSONObject requestJSON = new JSONObject();
      requestJSON.put("mode", "markAsRead");
      requestJSON.put("messageId", messageIdArray);

      StringEntity requestBody = new StringEntity(requestJSON.toString());
      httpPut.setEntity(requestBody);
      JSONObject responseObject = client.execute(httpPut, myResponseHandler);
      System.out.println("Mark as read response : " + responseObject);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JSONObject composeMail(String from, String to, String subject, String contentType, String content) {
    JSONObject messageObject = new JSONObject();

    // subject
    messageObject.put("subject", subject);

    System.out.println("From address : " + from);

    // from
    from = from.split("@")[0] + "@zohomail.in";
    System.out.println("From address : " + from);
    messageObject.put("fromAddress", from);

    // body
    messageObject.put("content", content);

    // to address
    messageObject.put("toAddress", to);
    return messageObject;
  }

  public void sendMail(String access_token, String from, String to, String subject, String contentType, String content,
      boolean hasAttachment, List<File> files, List<String> contentTypes) {

    JSONObject messageObject = composeMail(from, to, subject, contentType, content);
    JSONObject attachmentObject = null;
    if (hasAttachment) {
      attachmentObject = uploadAttachment(access_token,files, contentTypes);
    }

    System.out.println("Attachment object : "+attachmentObject);

    

    try {
      CloseableHttpClient client = HttpClients.createDefault();
    HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();
   

      HttpPost httpPost = new HttpPost(ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages");
      httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

      StringEntity bodyContent = new StringEntity(messageObject.toString());

      httpPost.setEntity(bodyContent);
      JSONObject responseObject = client.execute(httpPost, myResponseHandler);

      System.out.println("Response : " + responseObject);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JSONObject uploadAttachment(String access_token , List<File> files, List<String> contentTypes) {
    JSONObject responseObject = null;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpClientResponseHandler<JSONObject> myResponseHandler = new MyResponseHandler();

      HttpPost httpPost = new HttpPost(ZOHO_MESSAGE_ENDPOINT + "/" + accountId + "/messages/attachments");
      httpPost.addHeader(HttpHeaders.AUTHORIZATION , "Bearer " + access_token);

      MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
      
      for(int i = 0;i<files.size(); i++) {
        File file = files.get(i);
        multipartEntity = multipartEntity.addPart("file",new FileBody(file, ContentType.DEFAULT_BINARY, file.getName()));
      }

      HttpEntity fileEntity = multipartEntity.build();
      httpPost.setEntity(fileEntity);

      httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");


      responseObject = client.execute( httpPost , myResponseHandler);
      System.out.println("Response Object : "+responseObject);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return responseObject;
  }

}
