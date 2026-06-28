package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.*;

public class App extends Application {

    private static String loggedInUser = null;
    private static String userRole = null; // Stores user role
    private static final String SESSION_FILE = "session.txt";

    public static void setLoggedInUser(String username, String role) {
        loggedInUser = username;
        userRole = role;
        saveSession();
    }

    public static String getLoggedInUser() {
        loadSession();
        return loggedInUser;
    }

    public static String getUserRole() {
        loadSession();
        return userRole;
    }

    public static void clearSession() {
        loggedInUser = null;
        userRole = null;
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

  private static void saveSession() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(SESSION_FILE))) {
        writer.write(loggedInUser + "\n" + userRole); // Store both username and role
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private static void loadSession() {
    try (BufferedReader reader = new BufferedReader(new FileReader(SESSION_FILE))) {
        loggedInUser = reader.readLine();
        userRole = reader.readLine();
    } catch (IOException e) {
        loggedInUser = null;
        userRole = null;
    }
}



@Override
public void start(Stage stage) throws IOException {
    DB database = new DB();
    database.synchronizeDatabases();
    database.startAutoSync();
    
    // Ensure required tables exist
    database.createTable();           // Creates Users table if not exists
    database.createSharedFilesTable(); // Creates SharedFiles table if not exists

    loadSession();
    FXMLLoader loader;

    if (loggedInUser != null && !loggedInUser.isEmpty()) {
        System.out.println("Session Found: Redirecting to Secondary View");
        loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
    } else {
        System.out.println("No Active Session: Redirecting to Role Selection");
        loader = new FXMLLoader(getClass().getResource("role_selection.fxml"));
    }

    Parent root = loader.load();
    Scene scene = new Scene(root, 640, 480);
    stage.setScene(scene);
    stage.setTitle("User System");
    stage.show();
}

    
    private static final String MASTER_PASSWORD = "Admin@123"; // Change this to your actual master password

public static boolean verifyMasterPassword(String inputPassword) {
    return MASTER_PASSWORD.equals(inputPassword);
}


    public static void main(String[] args) {
        launch();
    }
}
