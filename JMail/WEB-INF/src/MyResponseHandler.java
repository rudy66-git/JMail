import java.io.IOException;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;

public class MyResponseHandler implements HttpClientResponseHandler<JSONObject> {

  @Override
  public JSONObject handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
    int statusCode = response.getCode();
    HttpEntity responseEntity = response.getEntity(); 
    if(statusCode >= 300) {
      throw new HttpResponseException(statusCode , response.getReasonPhrase());
    }
    if(responseEntity == null) {
      throw new ClientProtocolException("No content in mail");
    }

    // String contentTypeString = responseEntity.getContentType();
    // ContentType contentType = ContentType.create(contentTypeString);
    // Charset charSet = contentType.getCharset();
    String json = EntityUtils.toString(responseEntity);
    JSONObject jsonObject = new JSONObject(json);
    return jsonObject;
  }
  
}