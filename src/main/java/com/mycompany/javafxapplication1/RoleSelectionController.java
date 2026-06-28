package com.mycompany.javafxapplication1;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class RoleSelectionController {

    @FXML
    private Button standardUserBtn, adminUserBtn;

    @FXML
    private void handleStandardUser() {
        openLoginPage("Standard");
    }

    @FXML
    private void handleAdminUser() {
        openLoginPage("Admin");
    }

    private void openLoginPage(String role) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
        Parent root = loader.load();

        // Get controller instance and pass role info
        PrimaryController controller = loader.getController();
        controller.setUserRole(role);

        Stage loginStage = new Stage();
        loginStage.setScene(new Scene(root, 640, 480));
        loginStage.setTitle("Login - " + role.toUpperCase());
        loginStage.show();

        // Close the role selection window
        Stage currentStage = (Stage) standardUserBtn.getScene().getWindow();
        currentStage.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

}

