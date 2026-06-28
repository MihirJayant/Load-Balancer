package com.mycompany.javafxapplication1;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;

public class PrimaryController {
   

    @FXML
    private TextField userTextField;
    
    @FXML
private PasswordField masterPasswordField;


    @FXML
    private PasswordField passPasswordField;

    @FXML
    private Button loginBtn;

    @FXML
    private Button registerBtn;

    @FXML
    private Label roleLabel; // Displays user role

    private String userRole; // Stores the selected role

    private DB database = new DB();

    public void setUserRole(String role) {
    this.userRole = role;
    
    // Show Master Password only for Admins
    if (role.equalsIgnoreCase("admin")) {
        masterPasswordField.setVisible(true);
    } else {
        masterPasswordField.setVisible(false);
    }
}


@FXML
private void switchToSecondary() {
    Stage secondaryStage = new Stage();
    Stage primaryStage = (Stage) loginBtn.getScene().getWindow();
    try {
        String username = userTextField.getText();
        String password = passPasswordField.getText();
        String masterPassword = masterPasswordField.getText(); // Get master password input

        if (userRole == null || userRole.isEmpty()) {
            showAlert("Error", "Please select a role before logging in.");
            return;
        }

        // Admin Login: Require Master Password
        if (userRole.equalsIgnoreCase("admin")) {
            if (!App.verifyMasterPassword(masterPassword)) {
                showAlert("Login Failed", "Invalid Master Password for Admin access.");
                return;
            }
        }

        if (database.validateUser(username, password, userRole)) { // Validate user with role
            App.setLoggedInUser(username, userRole);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.setUserData(username, userRole);

            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("User Dashboard - " + userRole.toUpperCase());

            secondaryStage.show();
            primaryStage.close();
        } else {
            showAlert("Login Failed", "Invalid username, password, or role.");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}










@FXML
private void registerBtnHandler() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("register.fxml"));
        Parent root = loader.load();

        // Pass the role to RegisterController
        RegisterController controller = loader.getController();
        controller.setUserRole(userRole); // Assign role

        Stage registerStage = new Stage();
        registerStage.setScene(new Scene(root, 640, 480));
        registerStage.setTitle("Register - " + userRole.toUpperCase());
        registerStage.show();

        // Close the current login window
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        primaryStage.close();
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Error", "Failed to open registration page.");
    }
}


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
