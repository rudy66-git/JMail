import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.json.JSONArray;
import org.json.JSONObject;

public class MailReader {

  private static final String fileLocation = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps\\JMail\\temp";
  private static Folder folder;
  private static Message[] unReadMessages;

  void getPart(Part part, JSONObject jsonObject) {
    try {
      if (part instanceof Message) {
        String subject = part.getHeader("subject")[0];
        String from = part.getHeader("from")[0];
        String date = part.getHeader("date")[0];

        jsonObject.put("subject", subject);
        jsonObject.put("from", from);
        jsonObject.put("date", date);

      }
      if (part.isMimeType("text/plain")) {

        String content = (String) part.getContent();
        jsonObject.put("stringContent", content);
        System.out.println("Within text /plain content");

      } else if (part.isMimeType("text/html")) {

        String content = (String) part.getContent();
        jsonObject.put("htmlContent", content);
        System.out.println("Within text /html content");

      } else if (part.isMimeType("multipart/*")) {

        System.out.println("Multi part");
        Multipart mp = (Multipart) part.getContent();
        int multipartCount = mp.getCount();
        for (int i = 0; i < multipartCount; i++)
          getPart(mp.getBodyPart(i), jsonObject);

      } else {
        Object object = part.getContent();
        if (object instanceof String) {

          System.out.println("Within string content ");

        } else if (object instanceof InputStream) {

          System.out.println("Within inputstream  ");
          String filename = part.getFileName();
          System.out.println("Filename: " + filename);
          if (filename != null) {
            try (InputStream inputStream = (InputStream) object;
                FileOutputStream fileOutputStream = new FileOutputStream(fileLocation + filename)) {
              byte[] byteRead = new byte[8 * 1024];
              while (inputStream.read(byteRead) != -1)
                fileOutputStream.write(byteRead);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JSONArray searchMessages(String searchType, String searchValue, boolean read) {
    JSONArray jsonArray = null;
    SearchTerm mainSearchTerm = null;
    try {
      folder.open(Folder.READ_ONLY);
      switch (searchType) {
        case "Content":
          mainSearchTerm = new BodyTerm(searchValue);
          break;
        case "Sender":
          mainSearchTerm = new FromStringTerm(searchValue);
          break;
        case "Subject":
          mainSearchTerm = new SubjectTerm(searchValue);
          break;
      }
      Flags seenFlag = new Flags(Flags.Flag.SEEN);
      FlagTerm flagTerm = new FlagTerm(seenFlag, !(read));
      SearchTerm searchTerm = new AndTerm(flagTerm, mainSearchTerm);
      Message[] messages = null;
      messages = folder.search(searchTerm);
      if (messages.length > 0) {
        jsonArray = new JSONArray();
        unReadMessages = messages;
        for (int i = 0; i < messages.length; i++) {
          Part part = messages[i];
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("messageId", i);
          jsonObject.put("mail", messages[i]);
          getPart(part, jsonObject);
          jsonArray.put(jsonObject);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }finally {
      try {
        folder.close();
      } catch (MessagingException e) {
        e.printStackTrace();
      }
    }
    
    return jsonArray;
  }

  public void markAllAsRead() {
    try {
      folder.open(Folder.READ_WRITE);
      folder.setFlags(unReadMessages,new Flags(Flags.Flag.SEEN), true);
    } catch (MessagingException e) {
      e.printStackTrace();
    }finally {
      try {
        folder.close();
      } catch (MessagingException e) {
        e.printStackTrace();
      }
    }
  }

  public JSONArray readMessages(String host, String username, String password, String storeType) {
    Store store = null;
    JSONArray jsonArray = null;
    try {
      Properties properties = new Properties();
      properties.put("mail.imap.host", host);
      properties.put("mail.imap.port", 993);
      properties.put("mail.store.protocol", storeType);
      properties.put("mail.imap.ssl.enable", "true");
      properties.put("mail.imap.auth.mechanisms", "XOAUTH2");

      Session session = Session.getInstance(properties);
      store = session.getStore();
      store.connect(host, username, password);

      folder = store.getFolder("inbox");
      folder.open(Folder.READ_ONLY);

      if (folder.getType() > 0) {
        Message[] messages = folder.getMessages();
        int numberOfMessges = messages.length;
        jsonArray = new JSONArray();
        for (int i = 0; i < numberOfMessges; i++) {
          Part part = messages[i];
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("messageId", i);
          getPart(part, jsonObject);
          jsonArray.put(jsonObject);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } 
    finally {
      try {
        folder.close();
      } catch (MessagingException e) {
        e.printStackTrace();
      }
    }
    return jsonArray;
  }

}
