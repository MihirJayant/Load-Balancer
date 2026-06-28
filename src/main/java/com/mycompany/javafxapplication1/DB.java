package com.mycompany.javafxapplication1;


import static com.mysql.cj.conf.PropertyKey.PASSWORD;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import static java.util.Objects.hash;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DB {
    private String fileName = "jdbc:sqlite:comp20081.db";
    private int timeout = 30;
    private String dataBaseTableName = "Users";
    private Connection connection = null;
    private String saltValue;
    private static final Logger LOGGER = LoggerManager.getLogger();
    
   
    
    // MySQL Connection Settings
    private static final String MYSQL_DB_URL = "jdbc:mysql://lamp-server:3306/load_balancer_db?useSSL=false";
    private static final String MYSQL_USER = "admin";
    private static final String MYSQL_PASSWORD = "I1TIF9z35pRm";

    
   
    public DB() {
    try {
        // Load SQLite Driver
        Class.forName("org.sqlite.JDBC");

        // Load MySQL Driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Generate Salt for Password Hashing
        File fp = new File(".salt");
        if (!fp.exists()) {
            saltValue = getSaltvalue(30);
            try (FileWriter myWriter = new FileWriter(fp)) {
                myWriter.write(saltValue);
            }
        } else {
            try (Scanner myReader = new Scanner(fp)) {
                if (myReader.hasNextLine()) {
                    saltValue = myReader.nextLine();
                }
            }
        }

        System.out.println("SQLite and MySQL Drivers Loaded Successfully!");
        
        synchronizeDatabases();

    } catch (ClassNotFoundException e) {
        System.err.println("Database Driver Not Found! Ensure SQLite & MySQL drivers are included in dependencies.");
        e.printStackTrace();
    } catch (IOException e) {
        System.err.println("Error generating or reading salt file.");
        e.printStackTrace();
    }
}

public List<String> getChunkLocations(String fileName) {
        List<String> locations = fetchChunkLocations(fileName, true); // Try MySQL first
        if (locations.isEmpty()) {
            locations = fetchChunkLocations(fileName, false); // Fallback to SQLite
        }
        return locations;
    }

    
    public void createTable() {
        createSQLiteTables();
        createMySQLTables();
    }

    private void createSQLiteTables() {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:comp20081.db");
         Statement stmt = conn.createStatement()) {

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE, " +
                "password TEXT, " +
                "role TEXT)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_metadata (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT UNIQUE, " +
                "owner TEXT, " +
                "permissions TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS SharedFiles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT, " +
                "owner TEXT, " +
                "shared_with TEXT, " +
                "permissions TEXT)");
        
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_chunks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "file_name TEXT, " +
                    "chunk_index INTEGER, " +
                    "container_name TEXT, " +
                    "encrypted INTEGER DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

           


    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    private void createMySQLTables() {
    try (Connection conn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
         Statement stmt = conn.createStatement()) {

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) UNIQUE, " +
                "password VARCHAR(255), " +
                "role VARCHAR(50))");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_metadata (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "file_name VARCHAR(255) UNIQUE, " +
                "owner VARCHAR(255), " +
                "permissions VARCHAR(50), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS SharedFiles (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "file_name VARCHAR(255), " +
                "owner VARCHAR(255), " +
                "shared_with VARCHAR(255), " +
                "permissions VARCHAR(50))");
        
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS file_chunks (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "file_name VARCHAR(255), " +
                    "chunk_index INT, " +
                    "container_name VARCHAR(255), " +
                    "encrypted BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

 public void addUser(String user, String password, String role) throws InvalidKeySpecException {
        String hashedPassword = generateSecurePassword(password);
        addUserToDatabase(fileName, user, hashedPassword, role);  // SQLite
        addUserToDatabase(MYSQL_DB_URL, user, hashedPassword, role); // MySQL
    }

    private void addUserToDatabase(String dbUrl, String user, String password, String role) {
        try (Connection conn = DriverManager.getConnection(dbUrl, MYSQL_USER, MYSQL_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Users (name, password, role) VALUES (?, ?, ?)")) {

            stmt.setString(1, user);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
            System.out.println("User added: " + user);

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("User already exists.");
            } else {
                e.printStackTrace();
            }
        }
    }

    
    public String generateSecurePassword(String password) throws InvalidKeySpecException {
    byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());
    return Base64.getEncoder().encodeToString(securePassword);
    
   
}
    
    /** Ensures the SharedFiles table exists */
    public void createSharedFilesTable() {
        try {
            connection = DriverManager.getConnection(fileName);
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS SharedFiles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "file_name TEXT, " +
                    "owner TEXT, " +
                    "shared_with TEXT, " +
                    "permissions TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }




  public void addDataToDB(String user, String password, String role) throws InvalidKeySpecException {
    try {
        connection = DriverManager.getConnection(fileName);
        String hashedPassword = generateSecurePassword(password);

        // Store username, password, and role uniquely
        String query = "INSERT INTO Users (name, password, role) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, user);
        preparedStatement.setString(2, hashedPassword);
        preparedStatement.setString(3, role);
        preparedStatement.executeUpdate();

        System.out.println("User registered: " + user + " as " + role);

    } catch (SQLException e) {
        if (e.getMessage().contains("UNIQUE constraint failed")) {
            System.out.println("Error: Username already exists for this role.");
        }
        e.printStackTrace();
    } finally {
        closeConnection();
    }
}
  
  /** Retrieves the list of files shared with a specific user */
public ObservableList<String> getSharedFilesForUser(String username) {
    ObservableList<String> sharedFiles = FXCollections.observableArrayList();
    try {
        connection = DriverManager.getConnection(this.fileName);
        String query = "SELECT file_name FROM SharedFiles WHERE shared_with = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, username);
        ResultSet rs = preparedStatement.executeQuery();

        while (rs.next()) {
            sharedFiles.add(rs.getString("file_name"));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        closeConnection();
    }
    return sharedFiles;
}



    public boolean validateUser(String user, String pass, String role) throws InvalidKeySpecException {
        boolean isValid = false;

        if (role == null || role.isEmpty()) {
            LOGGER.warning("Login Failed: Role is null during login attempt for user " + user);
            return false;
        }

        try {
            connection = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
            String query = "SELECT password FROM Users WHERE name = ? AND role = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, user);
            preparedStatement.setString(2, role);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                String storedHashedPassword = rs.getString("password");
                String inputHashedPassword = generateSecurePassword(pass);

                if (storedHashedPassword.equals(inputHashedPassword)) {
                    isValid = true;
                    LOGGER.info("Login Successful: User=" + user + ", Role=" + role + " (MySQL)");
                }
            }

            if (!isValid) {
                LOGGER.warning("User not found in MySQL. Falling back to SQLite...");
            }

        } catch (SQLException e) {
            LOGGER.severe("MySQL Connection Failed: " + e.getMessage() + ". Trying SQLite...");
        } finally {
            closeConnection();
        }

        if (!isValid) {
            try {
                connection = DriverManager.getConnection(fileName);
                String query = "SELECT password FROM Users WHERE name = ? AND role = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, user);
                preparedStatement.setString(2, role);
                ResultSet rs = preparedStatement.executeQuery();

                if (rs.next()) {
                    String storedHashedPassword = rs.getString("password");
                    String inputHashedPassword = generateSecurePassword(pass);

                    if (storedHashedPassword.equals(inputHashedPassword)) {
                        isValid = true;
                        LOGGER.info("Login Successful: User=" + user + ", Role=" + role + " (SQLite)");
                    }
                }

            } catch (SQLException e) {
                LOGGER.severe("SQLite Connection Failed: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        if (isValid) {
            LOGGER.info("Login Attempt: Username=" + user + ", Role=" + role + " -> Success");
        } else {
            LOGGER.warning("Login Attempt: Username=" + user + ", Role=" + role + " -> Failed");
        }

        return isValid;
    }


 public void addFileMetadata(String fileName, String owner, String permissions) {
    // First, add to SQLite
    addFileMetadataToDatabase(fileName, owner, permissions, false);
    // Then, add to MySQL
    addFileMetadataToDatabase(fileName, owner, permissions, true);
}

   private void addFileMetadataToDatabase(String fileName, String owner, String permissions, boolean useMySQL) {
    String dbUrl = useMySQL ? MYSQL_DB_URL : "jdbc:sqlite:comp20081.db";

    // Queries for checking, inserting, and updating metadata
    String checkSQL = "SELECT owner, permissions, updated_at FROM file_metadata WHERE file_name = ?";
    String insertSQL = "INSERT INTO file_metadata (file_name, owner, permissions, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
    String updateSQL = "UPDATE file_metadata SET owner = ?, permissions = ?, updated_at = CURRENT_TIMESTAMP WHERE file_name = ?";

    try {
        System.out.println("Connecting to database: " + dbUrl);

        try (Connection conn = useMySQL 
                ? DriverManager.getConnection(dbUrl, MYSQL_USER, MYSQL_PASSWORD) 
                : DriverManager.getConnection(dbUrl);
             PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
             PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
             PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

            // Check if the file already exists
            checkStmt.setString(1, fileName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Retrieve existing metadata
                String existingOwner = rs.getString("owner");
                String existingPermissions = rs.getString("permissions");
                Timestamp lastUpdated = rs.getTimestamp("updated_at");

                // **Only update if something has changed**
                if (!existingOwner.equals(owner) || !existingPermissions.equals(permissions)) {
                    updateStmt.setString(1, owner);
                    updateStmt.setString(2, permissions);
                    updateStmt.setString(3, fileName);
                    updateStmt.executeUpdate();
                    System.out.println("Metadata UPDATED for " + fileName + " in " + (useMySQL ? "MySQL" : "SQLite") + " | Last Updated: " + lastUpdated);
                } else {
                    System.out.println("No changes detected for " + fileName + " in " + (useMySQL ? "MySQL" : "SQLite"));
                }
            } else {
                // Insert new record
                insertStmt.setString(1, fileName);
                insertStmt.setString(2, owner);
                insertStmt.setString(3, permissions);
                insertStmt.executeUpdate();
                System.out.println("Metadata INSERTED for " + fileName + " in " + (useMySQL ? "MySQL" : "SQLite"));
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
   

    public String getFileMetadata(String fileName) {
    String metadata = getFileMetadataFromDatabase(fileName, true); // First try MySQL
    if (metadata.equals("Metadata not found.")) {
        metadata = getFileMetadataFromDatabase(fileName, false); // Then try SQLite
    }
    return metadata;
}

private String getFileMetadataFromDatabase(String fileName, boolean useMySQL) {
    String sql = "SELECT * FROM file_metadata WHERE file_name = ?";
    String dbUrl = useMySQL ? MYSQL_DB_URL : "jdbc:sqlite:comp20081.db";

    // Debugging: Print which DB is being used
    System.out.println("Connecting to database: " + dbUrl);

    try (Connection conn = DriverManager.getConnection(dbUrl, 
            useMySQL ? MYSQL_USER : "", 
            useMySQL ? MYSQL_PASSWORD : ""); 
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, fileName);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            Timestamp updated_at = rs.getTimestamp("updated_at");
            return "File: " + rs.getString("file_name") + 
                   "\nOwner: " + rs.getString("owner") + 
                   "\nPermissions: " + rs.getString("permissions") +
                   "\nCreated: " + rs.getString("created_at") +
                   "\nLast Modified: " + rs.getString("updated_at");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return "Metadata not found.";
}



    
    public boolean userExists(String user, String role) {
    boolean exists = false;
    try {
        connection = DriverManager.getConnection(fileName);
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(timeout);

        ResultSet rs = statement.executeQuery("SELECT * FROM " + dataBaseTableName +
                " WHERE name='" + user + "' AND role='" + role + "'");

        if (rs.next()) {
            exists = true;
        }

    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        closeConnection();
    }
    return exists;
}



    public void deleteUser(String username) {
        try {
            connection = DriverManager.getConnection(fileName);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            statement.executeUpdate("DELETE FROM " + dataBaseTableName + " WHERE name='" + username + "'");

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    public void updateUser(String username, String newPassword) {
        try {
            connection = DriverManager.getConnection(fileName);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            String hashedPassword = generateSecurePassword(newPassword);
            statement.executeUpdate("UPDATE " + dataBaseTableName +
                    " SET password='" + hashedPassword + "' WHERE name='" + username + "'");

        } catch (SQLException | InvalidKeySpecException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }
    
    public void shareFile(String fileName, String owner, String sharedWith, String permissions) {
    try {
        connection = DriverManager.getConnection(this.fileName);
        PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO SharedFiles (file_name, owner, shared_with, permissions) VALUES (?, ?, ?, ?)"
        );
        preparedStatement.setString(1, fileName);
        preparedStatement.setString(2, owner);
        preparedStatement.setString(3, sharedWith);
        preparedStatement.setString(4, permissions);
        preparedStatement.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        closeConnection();
    }
}

    
  public String getFilePermissions(String fileName, String username) {
    String permissions = null;
    try {
        connection = DriverManager.getConnection(this.fileName);
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT permissions FROM SharedFiles WHERE file_name = ? AND shared_with = ?"
        );
        preparedStatement.setString(1, fileName);
        preparedStatement.setString(2, username);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            permissions = rs.getString("permissions");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        closeConnection();
    }
    return permissions;
}





    public ObservableList<User> getDataFromTable() {
        ObservableList<User> result = FXCollections.observableArrayList();
        try {
            connection = DriverManager.getConnection(fileName);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("SELECT name, password FROM " + dataBaseTableName);

            while (rs.next()) {
                result.add(new User(rs.getString("name"), rs.getString("password")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return result;
    }

    private String getSaltvalue(int length) {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder salt = new StringBuilder();
        Random random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            salt.append(characters.charAt(random.nextInt(characters.length())));
        }

        return salt.toString();
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
    
    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
    PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 256);
    Arrays.fill(password, Character.MIN_VALUE);

    try {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new AssertionError("Error while hashing password: " + e.getMessage(), e);
    } finally {
        spec.clearPassword();
    }
    
    }
    
    public void deleteFileMetadata(String fileName) {
    deleteFileMetadataFromDatabase(fileName, false); // Delete from SQLite
    deleteFileMetadataFromDatabase(fileName, true);  // Delete from MySQL
}

private void deleteFileMetadataFromDatabase(String fileName, boolean useMySQL) {
    String dbUrl = useMySQL ? MYSQL_DB_URL : "jdbc:sqlite:comp20081.db";
    String sql = "DELETE FROM file_metadata WHERE file_name = ?";

    try (Connection conn = useMySQL 
            ? DriverManager.getConnection(dbUrl, MYSQL_USER, MYSQL_PASSWORD) 
            : DriverManager.getConnection(dbUrl);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, fileName);
        int rowsAffected = pstmt.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("File metadata DELETED: " + fileName + " from " + (useMySQL ? "MySQL" : "SQLite"));
        } else {
            System.out.println("No metadata found to delete for: " + fileName);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    public void synchronizeDatabases() {
    LOGGER.info("Starting database synchronization...");

    syncUsers();
    syncFileMetadata();
    syncChunkMetadata();

    LOGGER.info("Database synchronization completed.");
}

    private void syncChunkMetadata() {
    String selectSQLiteSQL = "SELECT file_name, chunk_index, container_name, encrypted, created_at FROM file_chunks";
    String insertOrUpdateSQL = "INSERT INTO file_chunks (file_name, chunk_index, container_name, encrypted, created_at) " +
                               "VALUES (?, ?, ?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE container_name = VALUES(container_name), encrypted = VALUES(encrypted), created_at = VALUES(created_at)";

    try (Connection sqliteConn = DriverManager.getConnection(fileName);
         Connection mysqlConn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
         PreparedStatement selectSQLiteStmt = sqliteConn.prepareStatement(selectSQLiteSQL);
         PreparedStatement insertOrUpdateStmt = mysqlConn.prepareStatement(insertOrUpdateSQL);
         ResultSet sqliteRs = selectSQLiteStmt.executeQuery()) {

        while (sqliteRs.next()) {
            insertOrUpdateStmt.setString(1, sqliteRs.getString("file_name"));
            insertOrUpdateStmt.setInt(2, sqliteRs.getInt("chunk_index"));
            insertOrUpdateStmt.setString(3, sqliteRs.getString("container_name"));
            insertOrUpdateStmt.setBoolean(4, sqliteRs.getBoolean("encrypted"));
            insertOrUpdateStmt.setTimestamp(5, sqliteRs.getTimestamp("created_at"));
            insertOrUpdateStmt.executeUpdate();

            System.out.println("Chunk metadata synced for " + sqliteRs.getString("file_name") + " part " + sqliteRs.getInt("chunk_index") + " in MySQL.");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}


private void syncUsers() {
    String selectSQL = "SELECT name, password, role FROM Users";
    String insertOrUpdateSQL = "INSERT INTO Users (name, password, role) VALUES (?, ?, ?) " +
                              "ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role)";

    try (Connection sqliteConn = DriverManager.getConnection("jdbc:sqlite:comp20081.db");
         Connection mysqlConn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
         PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSQL);
         PreparedStatement insertOrUpdateStmt = mysqlConn.prepareStatement(insertOrUpdateSQL);
         ResultSet rs = selectStmt.executeQuery()) {

        while (rs.next()) {
            insertOrUpdateStmt.setString(1, rs.getString("name"));
            insertOrUpdateStmt.setString(2, rs.getString("password"));
            insertOrUpdateStmt.setString(3, rs.getString("role"));
            insertOrUpdateStmt.executeUpdate();
        }
        System.out.println("User table synchronized with Admin .");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
private void syncFileMetadata() {
    String selectSQLiteSQL = "SELECT file_name, owner, permissions, updated_at FROM file_metadata";
    String selectMySQLSQL = "SELECT file_name, owner, permissions, updated_at FROM file_metadata WHERE file_name = ?";
    String insertOrUpdateSQL = "INSERT INTO file_metadata (file_name, owner, permissions, updated_at) " +
                               "VALUES (?, ?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE owner = VALUES(owner), permissions = VALUES(permissions), updated_at = VALUES(updated_at)";

    try (Connection sqliteConn = DriverManager.getConnection("jdbc:sqlite:comp20081.db");
         Connection mysqlConn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
         PreparedStatement selectSQLiteStmt = sqliteConn.prepareStatement(selectSQLiteSQL);
         PreparedStatement selectMySQLStmt = mysqlConn.prepareStatement(selectMySQLSQL);
         PreparedStatement insertOrUpdateStmt = mysqlConn.prepareStatement(insertOrUpdateSQL);
         ResultSet sqliteRs = selectSQLiteStmt.executeQuery()) {

        while (sqliteRs.next()) {
            String fileName = sqliteRs.getString("file_name");
            String owner = sqliteRs.getString("owner");
            String permissions = sqliteRs.getString("permissions");
            Timestamp sqliteTimestamp = sqliteRs.getTimestamp("updated_at");

            // Check if file exists in MySQL
            selectMySQLStmt.setString(1, fileName);
            ResultSet mysqlRs = selectMySQLStmt.executeQuery();

            boolean updateRecord = true;
            Timestamp mysqlTimestamp = null;

            if (mysqlRs.next()) {
                mysqlTimestamp = mysqlRs.getTimestamp("updated_at");

                // **Only update if SQLite timestamp is newer**
                if (mysqlTimestamp != null && !sqliteTimestamp.after(mysqlTimestamp)) {
                    updateRecord = false; 
                }
            }

            if (updateRecord) {
                insertOrUpdateStmt.setString(1, fileName);
                insertOrUpdateStmt.setString(2, owner);
                insertOrUpdateStmt.setString(3, permissions);
                insertOrUpdateStmt.setTimestamp(4, sqliteTimestamp);
                insertOrUpdateStmt.executeUpdate();
                System.out.println("Metadata SYNCED for " + fileName + " | SQLite Time: " + sqliteTimestamp + " | MySQL Time: " + mysqlTimestamp);
            } else {
                System.out.println("No sync needed for " + fileName + " | SQLite Time: " + sqliteTimestamp + " | MySQL Time: " + mysqlTimestamp);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void startAutoSync() {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(() -> {
        System.out.println("Auto-Syncing databases...");
        synchronizeDatabases();
    }, 0, 10, TimeUnit.MINUTES); // Runs every 10 minutes
}

 public void addChunkMetadata(String fileName, int chunkIndex, String container, boolean encrypted) {
    String checkSQL = "SELECT COUNT(*) FROM file_chunks WHERE file_name = ? AND chunk_index = ?";
    String insertSQL = "INSERT INTO file_chunks (file_name, chunk_index, container_name, encrypted, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
    String updateSQL = "UPDATE file_chunks SET container_name = ?, encrypted = ?, created_at = CURRENT_TIMESTAMP WHERE file_name = ? AND chunk_index = ?";

    try (Connection conn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
         PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
         PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
         PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {

        checkStmt.setString(1, fileName);
        checkStmt.setInt(2, chunkIndex);
        ResultSet rs = checkStmt.executeQuery();
        rs.next();
        int count = rs.getInt(1);

        if (count > 0) {
            updateStmt.setString(1, container);
            updateStmt.setBoolean(2, encrypted);
            updateStmt.setString(3, fileName);
            updateStmt.setInt(4, chunkIndex);
            updateStmt.executeUpdate();
            System.out.println("Chunk metadata updated in MySQL for " + fileName + " part " + chunkIndex);
        } else {
            insertStmt.setString(1, fileName);
            insertStmt.setInt(2, chunkIndex);
            insertStmt.setString(3, container);
            insertStmt.setBoolean(4, encrypted);
            insertStmt.executeUpdate();
            System.out.println("Chunk metadata inserted in MySQL for " + fileName + " part " + chunkIndex);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    private List<String> fetchChunkLocations(String fileName, boolean useMySQL) {
        List<String> locations = new ArrayList<>();
        String dbUrl = useMySQL ? MYSQL_DB_URL : "jdbc:sqlite:comp20081.db";
        String sql = "SELECT container_name FROM file_chunks WHERE file_name = ? ORDER BY chunk_index";

        try (Connection conn = DriverManager.getConnection(dbUrl, useMySQL ? MYSQL_USER : "", useMySQL ? MYSQL_PASSWORD : "");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                locations.add(rs.getString("container_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return locations;
    }
public void deleteChunkMetadata(String fileName) {
    String deleteSQL = "DELETE FROM file_chunks WHERE file_name = ?";

    try (Connection conn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

        pstmt.setString(1, fileName);
        int rowsDeleted = pstmt.executeUpdate();
        System.out.println("✅ " + rowsDeleted + " old chunk records deleted for " + fileName);

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public List<String> getActiveStorageContainers() {
    List<String> activeContainers = new ArrayList<>();
    String query = "SELECT container_name FROM storage_containers WHERE is_active = 1";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(query);
         ResultSet rs = pstmt.executeQuery()) {

        while (rs.next()) {
            activeContainers.add(rs.getString("container_name"));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return activeContainers;
}
public Connection connect() throws SQLException {
    return DriverManager.getConnection(MYSQL_DB_URL, MYSQL_USER, MYSQL_PASSWORD);
}



}
    
 


