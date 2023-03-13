import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
  private static Connection connection;
  private static PreparedStatement prepStatement;
  private static ResultSet resultSet;
  public static String dbUrl = "jdbc:postgresql://localhost:5432/maildb";
  public static String username = "postgres";
  public static String password = "Rudy@1234";

  public UserDAO() {
    getConnection();
  }

  private void getConnection() {
    try {
      Class.forName("org.postgresql.Driver");
      connection = DriverManager.getConnection(dbUrl, username, password);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean isAccessTokenValid(String mail) {
    boolean flag = false;

    try {

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return flag;
  }

  public String getAccessToken(String mail) {
    String accessToken = null;
    String query;
    try {
      query = "select access_token from authinfo where mail = ? and extract(epoch from (current_timestamp - created_at)) < 3600";
      prepStatement = connection.prepareStatement(query);
      prepStatement.setString(1, mail);
      resultSet = prepStatement.executeQuery();
      if (resultSet.next())
        accessToken = resultSet.getString(1);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return accessToken;
  }

  public String getRefreshToken(String mail) {
    String refreshToken = null;
    String query;
    try {
      query = "select refresh_token from authinfo where mail = ?";
      prepStatement = connection.prepareStatement(query);
      prepStatement.setString(1, mail);
      resultSet = prepStatement.executeQuery();
      if (resultSet.next())
        refreshToken = resultSet.getString(1);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return refreshToken;
  }

  public int storeAuthInfo(String mail, String accessToken, String refreshToken) {
    String query;
    int result = 0;
    try {
      query = "insert into authinfo(mail,access_token,refresh_token) values(?,?,?)";
      prepStatement = connection.prepareStatement(query);
      prepStatement.setString(1, mail);
      prepStatement.setString(2, accessToken);
      prepStatement.setString(3, refreshToken);
      result = prepStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  public boolean userExists(String mail) {

    boolean flag = false;
    String query;
    try {
      query = "select 1 from authinfo where mail = ?";
      prepStatement = connection.prepareStatement(query);
      prepStatement.setString(1, mail);
      resultSet = prepStatement.executeQuery();
      if (resultSet.next())
        flag = true;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return flag;
  }

  public void updateAccessToken(String accessToken , String mail) {
    String query;
    try {
      query = "update authinfo set access_token = ? , created_at = current_timestamp where mail = ?";
      prepStatement = connection.prepareStatement(query);
      prepStatement.setString(1, accessToken);
      prepStatement.setString(2, mail);
      prepStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public void removeAuthInfo(String mail) {
    String query;
    try {
      query = "delete from authinfo where mail = ?";
      prepStatement = connection.prepareStatement(query);
      prepStatement.setString(1, mail);
      prepStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        prepStatement.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
