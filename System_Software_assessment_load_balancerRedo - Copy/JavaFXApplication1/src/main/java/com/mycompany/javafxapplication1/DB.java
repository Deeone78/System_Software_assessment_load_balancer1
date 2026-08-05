package com.mycompany.javafxapplication1;
/**
 *
 * @author ntu-user
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DB {
    private String fileName = "jdbc:sqlite:" 
            + System.getProperty("user.home") + "/comp20081.db";
    private int timeout = 30;
    private String dataBaseName = "COMP20081";
    private String dataBaseTableName = "Users";
    Connection connection = null;
    private Random random = new SecureRandom();
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private int iterations = 10000;
    private int keylength = 256;
    private String saltValue;

    private String mysqlUrl = "jdbc:mysql://lbc_mysql_registry:3306/lbcsystem";
    private String mysqlUser = "root";
    private String mysqlPass = "rootpassword";
  
    public void seedAdmin() {
    try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);
        ResultSet rs = statement.executeQuery("select count(*) as cnt from " + dataBaseTableName + " where role='ADMIN'");
        if (rs.next() && rs.getInt("cnt") == 0) {
            String hashed = generateSecurePassword("admin123");
            statement.executeUpdate("insert into " + dataBaseTableName + " (name, password, role) values('admin','" + hashed + "','ADMIN')");
            System.out.println("Default admin created: username=admin password=admin123");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    } finally {
        closeConn();
    }
}
    DB() {
        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = this.getSaltvalue(30);
                FileWriter myWriter = new FileWriter(fp);
                myWriter.write(saltValue);
                myWriter.close();
            } else {
                Scanner myReader = new Scanner(fp);
                while (myReader.hasNextLine()) {
                    saltValue = myReader.nextLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTable(String tableName) throws ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("create table if not exists " + tableName + " (id integer primary key autoincrement, name string, password string, role string default 'STANDARD')");
            statement.executeUpdate("create table if not exists Files (id integer primary key autoincrement, filename text, owner text, size integer, chunks integer default 1, storage_node text)");
            statement.executeUpdate("create table if not exists ACL (id integer primary key autoincrement, filename text, shared_with text, permission text)");
            statement.executeUpdate("create table if not exists Sessions (id integer primary key autoincrement, username text, token text)");
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public void delTable(String tableName) throws ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("drop table if exists " + tableName);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public void addDataToDB(String user, String password) throws InvalidKeySpecException, ClassNotFoundException {
        addDataToDB(user, password, "STANDARD");
    }

    public void addDataToDB(String user, String password, String role) throws InvalidKeySpecException, ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            String hashed = generateSecurePassword(password);
            statement.executeUpdate("insert into " + dataBaseTableName + " (name, password, role) values('" + user + "','" + hashed + "','" + role + "')");
            logEvent("User added: " + user + " role=" + role);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public ObservableList<User> getDataFromTable() throws ClassNotFoundException {
        ObservableList<User> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select * from " + this.dataBaseTableName);
            while (rs.next()) {
                result.add(new User(rs.getString("name"), rs.getString("password"), rs.getString("role")));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
        return result;
    }

    public boolean validateUser(String user, String pass) throws InvalidKeySpecException, ClassNotFoundException {
        Boolean flag = false;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select name, password from " + this.dataBaseTableName);
            String inPass = generateSecurePassword(pass);
            while (rs.next()) {
                if (user.equals(rs.getString("name")) && rs.getString("password").equals(inPass)) {
                    flag = true;
                    break;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
        return flag;
    }

    public String getUserRole(String username) {
        String role = "STANDARD";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select role from " + dataBaseTableName + " where name='" + username + "'");
            if (rs.next()) {
                role = rs.getString("role");
            }
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
        return role;
    }

    public void promoteToAdmin(String username) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("update " + dataBaseTableName + " set role='ADMIN' where name='" + username + "'");
            logEvent("User promoted to ADMIN: " + username);
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public void deleteUser(String username) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("delete from " + dataBaseTableName + " where name='" + username + "'");
            logEvent("User deleted: " + username);
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public void saveFile(String filename, String owner, long size, int chunks, String storageNode) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("insert into Files (filename, owner, size, chunks, storage_node) values('" + filename + "','" + owner + "'," + size + "," + chunks + ",'" + storageNode + "')");
            logEvent("File saved: " + filename + " owner=" + owner);
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public ObservableList<FileItem> getFilesForUser(String username) {
        ObservableList<FileItem> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select filename, owner, size, chunks from Files where owner='" + username + "'");
            while (rs.next()) {
                result.add(new FileItem(rs.getString("filename"), rs.getString("owner"), rs.getLong("size"), rs.getInt("chunks")));
            }
            var statement2 = connection.createStatement();
            ResultSet rs2 = statement2.executeQuery("select f.filename, f.owner, f.size, f.chunks from Files f, ACL a where f.filename = a.filename and a.shared_with='" + username + "'");
            while (rs2.next()) {
                result.add(new FileItem(rs2.getString("filename"), rs2.getString("owner"), rs2.getLong("size"), rs2.getInt("chunks")));
            }
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
        return result;
    }

    public void deleteFile(String filename) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("delete from Files where filename='" + filename + "'");
            statement.executeUpdate("delete from ACL where filename='" + filename + "'");
            logEvent("File deleted: " + filename);
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public void setPermission(String filename, String sharedWith, String permission) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("insert into ACL (filename, shared_with, permission) values('" + filename + "','" + sharedWith + "','" + permission + "')");
            logEvent("Permission set: " + filename + " -> " + sharedWith + " (" + permission + ")");
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
    }

    public boolean hasPermission(String filename, String username, String permission) {
        boolean allowed = false;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet owner = statement.executeQuery("select owner from Files where filename='" + filename + "'");
            if (owner.next() && username.equals(owner.getString("owner"))) {
                allowed = true;
            }
            if (!allowed) {
                var statement2 = connection.createStatement();
                ResultSet rs = statement2.executeQuery("select permission from ACL where filename='" + filename + "' and shared_with='" + username + "'");
                while (rs.next()) {
                    String p = rs.getString("permission");
                    if (p.equals(permission) || p.equals("WRITE")) {
                        allowed = true;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConn();
        }
        return allowed;
    }

    public void logEvent(String message) {
        try {
            FileWriter fw = new FileWriter("system.log", true);
            fw.write(new java.util.Date().toString() + " - " + message + "\n");
            fw.close();
        } catch (IOException e) {
            System.err.println("Could not write log: " + e.getMessage());
        }
    }

    public void syncUserToMySQL(String username, String password, String role) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass);
            var st = conn.createStatement();
            st.executeUpdate("insert into users (username, password, role) values('" + username + "','" + password + "','" + role + "')");
            conn.close();
            logEvent("User synced to MySQL: " + username);
        } catch (Exception e) {
            System.err.println("MySQL sync (user) failed: " + e.getMessage());
        }
    }

    public void syncFileToMySQL(String filename, String owner, long size, int chunks, String storageNode) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass);
            var st = conn.createStatement();
            st.executeUpdate("insert into file_metadata (filename, owner, size, chunks, storage_node) values('" + filename + "','" + owner + "'," + size + "," + chunks + ",'" + storageNode + "')");
            conn.close();
            logEvent("File synced to MySQL: " + filename);
        } catch (Exception e) {
            System.err.println("MySQL sync (file) failed: " + e.getMessage());
        }
    }

    private void closeConn() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            finalval.append(characters.charAt(random.nextInt(characters.length())));
        }
        return new String(finalval);
    }

    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public String generateSecurePassword(String password) throws InvalidKeySpecException {
        String finalval = null;
        byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());
        finalval = Base64.getEncoder().encodeToString(securePassword);
        return finalval;
    }

    public String getTableName() {
        return this.dataBaseTableName;
    }

    public void log(String message) {
        System.out.println(message);
    }
}