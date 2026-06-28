package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;

public class RegisterController {

    @FXML
    private TextField userTextField;

    @FXML
    private PasswordField passPasswordField, rePassPasswordField;

    @FXML
    private Button registerBtn, backLoginBtn;

    private DB database = new DB();
    private String userRole; // Store the selected role

    // Set the user role from PrimaryController
    public void setUserRole(String role) {
        this.userRole = role;
        System.out.println("Registering as: " + role);
    }

    @FXML
private void registerBtnHandler() {
    String username = userTextField.getText();
    String password = passPasswordField.getText();
    String confirmPassword = rePassPasswordField.getText();

    if (username.isEmpty() || password.isEmpty()) {
        showAlert("Error", "Username and Password cannot be empty.");
        return;
    }

    if (!password.equals(confirmPassword)) {
        showAlert("Error", "Passwords do not match.");
        return;
    }

    if (userRole == null || userRole.isEmpty()) {
        showAlert("Error", "User role is not set. Please restart.");
        return;
    }

    try {
        if (database.userExists(username, userRole)) { // Ensure unique usernames per role
            showAlert("Error", "User with this role already exists!");
            return;
        }

        database.addDataToDB(username, password, userRole); // Store user in DB
        showAlert("Success", "Account created successfully! Redirecting to Role Selection.");

        navigateToRoleSelection(); // Redirect to role selection

    } catch (InvalidKeySpecException e) {
        e.printStackTrace();
        showAlert("Error", "Error while hashing password.");
    }
}


    @FXML
    private void backLoginBtnHandler() {
        try {
            // Load Role Selection Page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/role_selection.fxml"));
            Parent root = loader.load();

            // Get current stage from the button
            Stage stage = (Stage) backLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Select User Role");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void navigateToLogin() {
        try {
            Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root, 640, 480));
            primaryStage.setTitle("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void navigateToRoleSelection() {
    try {
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("role_selection.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 640, 480));
        primaryStage.setTitle("Select Role");
    } catch (IOException e) {
        e.printStackTrace();
    }
}

}
