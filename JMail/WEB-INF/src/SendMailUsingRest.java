import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class SendMailUsingRest extends HttpServlet {
  private final static String UPLOAD_DIRECTORY = "upload";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String to = null;
    String subject = null;
    String content = null;
    String filename = null;
    List<File> files = null;
    List<String> contentTypes = null; 

    String path = getServletContext().getRealPath("/");
    System.out.println("Context path : " + path);

    boolean isMultipart = ServletFileUpload.isMultipartContent(req);
    System.out.println("Is multipart : " + isMultipart);
    boolean hasAttachment = false;
    if (ServletFileUpload.isMultipartContent(req)) {
      
      try {
        List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
        int attachmentIndex = multiparts.size() - 1;
        
        to = (String) multiparts.get(0).getString();
        subject = (String) multiparts.get(1).getString();
        content = (String) multiparts.get(2).getString();
        hasAttachment = Boolean.parseBoolean(multiparts.get(attachmentIndex).getString());

        if (hasAttachment) {
          files = new ArrayList<File>();
          contentTypes = new ArrayList<>();
          for (int i = 3; i < attachmentIndex; i++) {
            FileItem listItem = multiparts.get(i);
            System.out.print("File item name : " + listItem.getFieldName() + i);
            System.out.println(" file name : " + listItem.getName());
            filename = new File(listItem.getName()).getName();
            String contentType = listItem.getContentType();
            contentTypes.add(contentType);
            System.out.println("Content type : " + contentType);
            File file = new File(path + UPLOAD_DIRECTORY + File.separator + filename);
            listItem.write(file);
            files.add(file);
          }
        }

      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    HttpSession session = req.getSession(false);
    String mail = (String) session.getAttribute("mail");
    UserDAO userDAO = new UserDAO();
    String access_token = userDAO.getAccessToken(mail);

    if (mail.endsWith("outlook.com")) {
      MicrosoftMailReader microsoftMailReader = new MicrosoftMailReader();
      microsoftMailReader.sendMail(access_token, to, subject, "text", content, hasAttachment, files,contentTypes);

    } else if (mail.endsWith("zohotest.com")) {
      ZOHOMailReader zohoMailReader = new ZOHOMailReader();
      zohoMailReader.sendMail(access_token, mail, to, subject, "text", content);
    }

  }
}