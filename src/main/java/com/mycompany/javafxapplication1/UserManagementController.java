package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

public class UserManagementController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> roleChoiceBox;

    @FXML
    private Button createUserBtn;

    @FXML
    private Button deleteUserBtn;

    @FXML
    private Button updateUserBtn;

    private DB database = new DB();

    @FXML
    private void initialize() {
        roleChoiceBox.getItems().addAll("admin", "standard");
        roleChoiceBox.setValue("standard"); // Default role
    }

    @FXML
private void createUser() {
    String username = usernameField.getText();
    String password = passwordField.getText();
    String role = roleChoiceBox.getValue();

    if (username.isEmpty() || password.isEmpty()) {
        showAlert("Error", "Username and Password cannot be empty.");
        return;
    }

    try {
        database.addDataToDB(username, password, role);
        showAlert("Success", "User created successfully!");
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Error", "Could not create user.");
    }
}


    @FXML
    private void deleteUser() {
        String username = usernameField.getText();

        if (username.isEmpty()) {
            showAlert("Error", "Enter a username to delete.");
            return;
        }

        try {
            database.deleteUser(username);
            showAlert("Success", "User deleted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not delete user.");
        }
    }

    @FXML
    private void updateUserPassword() {
        String username = usernameField.getText();
        String newPassword = passwordField.getText();

        if (username.isEmpty() || newPassword.isEmpty()) {
            showAlert("Error", "Username and new password cannot be empty.");
            return;
        }

        try {
            database.updateUser(username, newPassword);
            showAlert("Success", "Password updated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not update password.");
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
