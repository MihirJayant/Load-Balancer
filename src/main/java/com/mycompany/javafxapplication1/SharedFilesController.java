package com.mycompany.javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.*;
import java.sql.*;
import java.util.List;

public class SharedFilesController {

    @FXML
    private ListView<String> sharedFilesListView;
    
    @FXML
    private Button editSharedFileBtn, backToFileManagementBtn;

    private DB database = new DB();
    private String loggedInUser;

    @FXML
    public void initialize() {
        loggedInUser = App.getLoggedInUser();
        loadSharedFiles();
    }

    private void loadSharedFiles() {
        ObservableList<String> sharedFiles = database.getSharedFilesForUser(loggedInUser);
        sharedFilesListView.setItems(sharedFiles);
    }

    @FXML
private void handleOpenSharedFile() {
    String selectedFileName = sharedFilesListView.getSelectionModel().getSelectedItem();
    if (selectedFileName == null) {
        showAlert("Error", "No shared file selected.");
        return;
    }

    String loggedInUser = App.getLoggedInUser();
    DB database = new DB();
    String permissions = database.getFilePermissions(selectedFileName, loggedInUser);

    if (permissions == null) {
        showAlert("Access Denied", "You do not have permission to access this file.");
        return;
    }

    File file = new File("storage/" + selectedFileName);

    // If permission is "Read", enforce read-only mode
    boolean readOnly = permissions.equalsIgnoreCase("Read");

    // Open file with correct permissions
    showFileContent(file, readOnly);
}

private void showFileContent(File file, boolean readOnly) {
    TextArea textArea = new TextArea();
    textArea.setWrapText(true);
    textArea.setPrefSize(400, 300);
    textArea.setEditable(!readOnly); // Enforce Read-Only Mode

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        textArea.setText(content.toString());
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Error", "Could not read the file.");
    }

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle(readOnly ? "View Shared File (Read-Only)" : "Edit Shared File");
    dialog.getDialogPane().setContent(textArea);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    dialog.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK && !readOnly) { // Prevent Editing if Read-Only
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
                showAlert("Success", "File updated successfully!");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Could not update file.");
            }
        }
    });
}



    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("filemanagement.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backToFileManagementBtn.getScene().getWindow();
            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle("File Management");
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
}
