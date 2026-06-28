package com.mycompany.javafxapplication1;

import com.mycompany.loadbalancer.LoadBalancer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SecondaryController {
    
    private LoadBalancer loadBalancer = new LoadBalancer();
  

    @FXML
    private TextField userTextField;
    
    @FXML
    private PasswordField newPasswordField;

    @FXML
    private TableView<User> dataTableView;

    @FXML
    private TableColumn<User, String> userColumn;

    @FXML
    private TableColumn<User, String> passColumn;

    @FXML
    private Button updateUserBtn, deleteUserBtn, logoutBtn;
    
    @FXML
    private Button selectFileBtn;
    

    private DB database = new DB();
   
public void initialize() {
    String username = App.getLoggedInUser(); // Get the currently logged-in user
    String role = App.getUserRole(); // Get the user's role

    if (username == null || role == null) {
        showAlert("Error", "No active session found. Please log in again.");
        navigateToRoleSelection();
        return;
    }

    userTextField.setText(username);
    userTextField.setEditable(false); // Prevent username editing

    userColumn.setCellValueFactory(cellData -> cellData.getValue().userProperty());
    passColumn.setCellValueFactory(cellData -> cellData.getValue().passProperty());

    loadUserData(username, role);

    // Apply role-based restrictions
    if (role.equalsIgnoreCase("standard")) {
        deleteUserBtn.setDisable(true); // Standard users cannot delete other users
    } else if (role.equalsIgnoreCase("admin")) {
        deleteUserBtn.setDisable(false); // Admins can delete users
    }

    System.out.println("Logged in as: " + username + " | Role: " + role);
}








    private void loadUserData(String username, String role) {
    ObservableList<User> allUsers = database.getDataFromTable();
    ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    if (role.equalsIgnoreCase("admin")) {
        // Admins see all users
        filteredUsers.addAll(allUsers);
    } else {
        // Standard users see only their own account
        for (User user : allUsers) {
            if (user.getUser().equals(username)) {
                filteredUsers.add(user);
            }
        }
    }

    dataTableView.setItems(filteredUsers);
}







    @FXML
private void handleUpdateUser() {
    String newPassword = newPasswordField.getText();
    String loggedInUser = App.getLoggedInUser(); // Get logged-in user
    String userRole = App.getUserRole(); // Get user role

    if (newPassword.isEmpty()) {
        showAlert("Error", "New password cannot be empty.");
        return;
    }

    try {
        database.updateUser(loggedInUser, newPassword);
        showAlert("Success", "Password updated successfully!");
        loadUserData(loggedInUser, userRole); // Pass both username & role
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Error", "Could not update password.");
    }
}



@FXML
private void handleDeleteUser() {
    String loggedInUser = App.getLoggedInUser();
    String userRole = App.getUserRole();
    User selectedUser = dataTableView.getSelectionModel().getSelectedItem();

    if (selectedUser == null) {
        showAlert("Error", "Please select a user to delete.");
        return;
    }

    if (userRole.equalsIgnoreCase("standard") && !selectedUser.getUser().equals(loggedInUser)) {
        showAlert("Permission Denied", "Standard users can only delete their own account.");
        return;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
        "Are you sure you want to delete user: " + selectedUser.getUser() + "?",
        ButtonType.YES, ButtonType.NO);
    alert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.YES) {
            database.deleteUser(selectedUser.getUser());
            showAlert("Success", "User deleted successfully.");
            loadUserData(loggedInUser, userRole);
        }
    });
}




    @FXML
private void handleLogout() {
    App.clearSession(); // Clear the stored session data
    navigateToRoleSelection(); // Redirect to role selection
}

private void navigateToRoleSelection() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("role_selection.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Select Role");
    } catch (IOException e) {
        e.printStackTrace();
    }
}





    private void navigateToLogin() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Login");
    } catch (IOException e) {
        e.printStackTrace();
    }
}


    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


public void setUserData(String username, String role) {
    userTextField.setText(username);
    userTextField.setEditable(false); // Prevent editing username

    userColumn.setCellValueFactory(cellData -> cellData.getValue().userProperty());
    passColumn.setCellValueFactory(cellData -> cellData.getValue().passProperty());

    loadUserData(username, role);

    // Restrict features based on role
    if (role.equalsIgnoreCase("standard")) {
        deleteUserBtn.setDisable(true);  // Standard users cannot delete other users
    } else if (role.equalsIgnoreCase("admin")) {
        deleteUserBtn.setDisable(false); // Admins can delete users
    }

    System.out.println("Logged in as: " + username + " | Role: " + role);
}

@FXML
private void handleSelectFile() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("filemanagement.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) selectFileBtn.getScene().getWindow();
        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("File Management");
        stage.show();

        // ✅ Show Load Balancer Queue Info
        String queueStatus = "Waiting Queue: " + loadBalancer.getWaitingQueue() + "\n" +
                             "Processing Queue: " + loadBalancer.getProcessingQueue() + "\n" +
                             "Ready Queue: " + loadBalancer.getReadyQueue();
        System.out.println(queueStatus); // Debugging
        
    } catch (IOException e) {
        e.printStackTrace();
    }
}



}