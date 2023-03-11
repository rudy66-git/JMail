import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetMailUsingRest extends HttpServlet {

  private static final String GET_MESSAGE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    JSONArray messages = null;
    try {

      HttpSession session = req.getSession(false);
      String access_token = (String) session.getAttribute("access_token");

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(GET_MESSAGE_ENDPOINT);

      httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access_token);

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

      JSONObject mailList = client.execute(httpGet, responseHandler);
      System.out.println("Response : " + mailList.toString());

      JSONArray responseMessage = (JSONArray) mailList.get("messages");

      messages = new JSONArray();
      for (int i = 0; i < responseMessage.length(); i++) {
        
        JSONObject message = new JSONObject();
        httpGet = new HttpGet(GET_MESSAGE_ENDPOINT + responseMessage.getJSONObject(i).getString("id"));
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

        String base64Content = body.getString("data");

        Base64.Decoder decoder = Base64.getUrlDecoder();
        String content = new String(decoder.decode(base64Content));
        System.out.println("decodedmail : " + content);
        message.put("stringContent", content);
        message.put("messageId", i);
        messages.put(message);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    resp.setContentType("application/json");
    resp.getWriter().print(messages);
  }
}