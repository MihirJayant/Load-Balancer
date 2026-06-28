package com.mycompany.loadbalancer;

import com.mycompany.loadbalancer.LoadBalancer.TrafficLevel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class LoadBalancerController {

    @FXML
    private ComboBox<String> algorithmComboBox;

    @FXML
    private Button uploadFileBtn, processNextBtn, openTerminalBtn, checkHealthBtn, deleteFileBtn, downloadFileBtn;

    @FXML
    private TextArea queueStatusText;

    private LoadBalancer loadBalancer = new LoadBalancer();

    @FXML
    public void initialize() {
        algorithmComboBox.getItems().addAll("ROUND_ROBIN", "FCFS", "SJF");
        algorithmComboBox.setValue("ROUND_ROBIN");
    }

    @FXML
    public void handleUploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Upload");
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            showAlert("Error", "No files selected.");
            return;
        }

        ChoiceDialog<String> roleDialog = new ChoiceDialog<>("user", "user", "admin");
    roleDialog.setTitle("Select User Role");
    roleDialog.setHeaderText("Choose the role for this upload:");
    roleDialog.setContentText("Role:");

    roleDialog.showAndWait().ifPresent(userRole -> {
        for (File file : selectedFiles) {
            loadBalancer.addToWaitingQueue(file.getAbsolutePath(), userRole); // Pass userRole
        }
        updateQueueStatus();
        showAlert("Success", "Files added to Waiting Queue.");
    });
       
    }
    
    

    @FXML
public void handleProcessNextFile() {
    String selectedAlgorithm = algorithmComboBox.getValue();
    TrafficLevel trafficLevel = loadBalancer.getTrafficLevel();
    
    String algorithmToUse = loadBalancer.determineBestAlgorithm();

    // Call LoadBalancer method to process files
    loadBalancer.processFilesAutomatically(algorithmToUse, trafficLevel);
    loadBalancer.applyAgingMechanism();

    
    loadBalancer.processFilesWithSemaphore();
    updateQueueStatus();

    if (loadBalancer.getReadyQueue().isEmpty()) {
        showAlert("File Transfer Failed", "Some files failed to transfer.");
    } else {
       showAlert("Success", "Files transferred successfully using " + algorithmToUse + "!");
    }
}


    @FXML
    public void handleCheckHealth() {
        StringBuilder status = new StringBuilder("Container Health Status:\n");

        for (String container : loadBalancer.getStorageContainers()) {
            boolean healthy = loadBalancer.isContainerHealthy(container);
            status.append(container).append(": ").append(healthy ? "Healthy" : "Down").append("\n");
        }

        showAlert("Container Health Check", status.toString());
    }

    @FXML
    public void handleOpenTerminal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/loadbalancer/terminal.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Terminal Emulator");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateQueueStatus() {
        String status = "Waiting Queue: " + loadBalancer.getWaitingQueue() + "\n" +
                        "Processing Queue: " + loadBalancer.getProcessingQueue() + "\n" +
                        "Ready Queue: " + loadBalancer.getReadyQueue();
        queueStatusText.setText(status);
    }

    @FXML
    public void handleDownloadFile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Download File");
        dialog.setHeaderText("Enter the file name to download:");
        dialog.setContentText("Filename:");

        dialog.showAndWait().ifPresent(fileName -> {
            boolean success = loadBalancer.downloadFile(fileName);
            if (success) {
                showAlert("Success", "File downloaded successfully!");
            } else {
                showAlert("Error", "File not found in any container.");
            }
        });
    }

    @FXML
    public void handleDeleteFile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Delete File");
        dialog.setHeaderText("Enter the file name to delete:");
        dialog.setContentText("Filename:");

        dialog.showAndWait().ifPresent(fileName -> {
            boolean success = loadBalancer.deleteFile(fileName);
            if (success) {
                showAlert("Success", "File deleted successfully!");
            } else {
                showAlert("Error", "File not found or deletion failed.");
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
}
