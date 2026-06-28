package com.mycompany.javafxapplication1;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.mycompany.loadbalancer.TerminalController;
import com.jcraft.jsch.*;
import com.mycompany.loadbalancer.LoadBalancer;
import com.mycompany.loadbalancer.RemoteTerminal;
import com.mycompany.loadbalancer.ScpTransfer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.collections.ObservableList;

public class FileManagementController {
    
     private LoadBalancer loadBalancer = new LoadBalancer();
     private static final Logger LOGGER = Logger.getLogger(FileManagementController.class.getName());


    @FXML
    private TextField selectedFileTextField;
    
    @FXML
private ListView<String> selectedFilesListView;
    
    private List<File> selectedFiles = new ArrayList<>();

    
    @FXML
private Button viewSharedFilesBtn;
    
  

    
    


    
    

    
    @FXML
private Button shareFileBtn;


    @FXML
    private Button editFileBtn, deleteFileBtn, uploadFileBtn, downloadFileBtn, startProcessingBtn, checkHealthBtn, openTerminalBtn;


    private File selectedFile;
    
    @FXML
private Button backBtn;


@FXML
private void handleSelectFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select Files");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

    String loggedInUser = App.getLoggedInUser();
    String userRole = App.getUserRole();

    List<File> files = fileChooser.showOpenMultipleDialog(null); // Allow multiple file selection
    

    if (files != null && !files.isEmpty()) {
        selectedFiles.clear(); // Clear previous selections
        selectedFiles.addAll(files);
        selectedFilesListView.getItems().clear(); // Clear UI ListView before adding new selections
         
        selectedFile = files.get(0);
        DB database = new DB();

        for (File file : files) {
            selectedFilesListView.getItems().add(file.getName()); // Display in ListView
            
            String metadata = database.getFileMetadata(file.getName());
            System.out.println("Metadata Retrieved: " + metadata);

            // Admins always have full access
            if (userRole.equalsIgnoreCase("admin")) {
                editFileBtn.setDisable(false);
                deleteFileBtn.setDisable(false);
                continue; // Skip other checks for admin
            }

            // Check if the file belongs to the logged-in user
            if (file.getName().startsWith(loggedInUser + "_")) {
                editFileBtn.setDisable(false);
                deleteFileBtn.setDisable(false);
                continue;
            }

            // Check if the file is shared with the user
            String permissions = database.getFilePermissions(file.getName(), loggedInUser);

            if (permissions == null) {
                showAlert("Access Denied", "You do not have permission to access: " + file.getName());
                selectedFiles.remove(file); // Remove from selected list
                continue;
            }

            boolean canWrite = permissions.equalsIgnoreCase("Write") || permissions.equalsIgnoreCase("Read & Write");

            if (!canWrite) {
                editFileBtn.setDisable(true);
                deleteFileBtn.setDisable(true);
                showAlert("Read-Only Access", "You can only read: " + file.getName());
            } else {
                editFileBtn.setDisable(false);
                deleteFileBtn.setDisable(false);
            }
        }
    }
}




private void showFileContent(File file, boolean readOnly) {
    TextArea textArea = new TextArea();
    textArea.setWrapText(true);
    textArea.setPrefSize(400, 300);
    textArea.setEditable(!readOnly); // Disable editing if read-only

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
    dialog.setTitle(readOnly ? "View File (Read-Only)" : "Edit File");
    dialog.getDialogPane().setContent(textArea);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    dialog.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK && !readOnly) {
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
private void handleCreateFile() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Create New File");
    dialog.setHeaderText("Enter the name of the new file:");
    dialog.setContentText("Filename:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(filename -> {
        File directory = new File("storage");
        if (!directory.exists()) {
            directory.mkdir(); // Ensure storage directory exists
        }

        String userRole = App.getUserRole();
        String loggedInUser = App.getLoggedInUser();

        // Prefix filename with the username for standard users
        if (userRole.equalsIgnoreCase("standard")) {
            filename = loggedInUser + "_" + filename;
        }

        File newFile = new File(directory, filename + ".txt");
        try {
            if (newFile.createNewFile()) {
                showAlert("Success", "File created: " + newFile.getAbsolutePath());
            } else {
                showAlert("Error", "File already exists.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not create file.");
        }
    });




}


    @FXML
    private void handleEditFile() {
        if (selectedFile == null) {
            showAlert("Error", "No file selected.");
            return;
        }
        
        if (!selectedFile.exists()) {
        showAlert("Error", "Selected file does not exist.");
        return;
    }

        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefSize(400, 300);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            textArea.setText(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit File");
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
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
private void handleDeleteFile() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Delete File");
    dialog.setHeaderText("Enter the file name to delete:");
    dialog.setContentText("Filename:");

    DB database = new DB();

    dialog.showAndWait().ifPresent(fileName -> {
        LOGGER.info("User requested to delete file: " + fileName);

        boolean success = loadBalancer.deleteFile(fileName);
        if (success) {
            LOGGER.info("File successfully deleted from Load Balancer: " + fileName);

            try {
                database.deleteFileMetadata(fileName);
                LOGGER.info("File metadata deleted from databases: " + fileName);

                database.synchronizeDatabases();
                LOGGER.info("Databases synchronized successfully after file deletion.");
                
                showAlert("Success", "File deleted and databases synchronized.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting file metadata or synchronizing databases.", e);
                showAlert("Error", "File deleted, but database synchronization failed.");
            }

        } else {
            LOGGER.warning("File deletion failed: " + fileName);
            showAlert("Error", "File not found or deletion failed.");
        }
    });
}


    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backBtn.getScene().getWindow();
            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle("User Dashboard");
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




@FXML
private void handleShareFile() {
    if (selectedFile == null) {
        showAlert("Error", "No file selected.");
        return;
    }

    String loggedInUser = App.getLoggedInUser();
    String fileName = selectedFile.getName();
    String userRole = App.getUserRole();

    TextInputDialog userDialog = new TextInputDialog();
    userDialog.setTitle("Share File");
    userDialog.setHeaderText("Enter the username of the user to share with:");
    Optional<String> userResult = userDialog.showAndWait();

    if (userResult.isEmpty()) {
        showAlert("Error", "No user entered.");
        return;
    }

    String sharedWithUser = userResult.get();
    DB database = new DB();
    String permissions;

    // Standard Users always share with Read & Write access
    if (userRole.equalsIgnoreCase("standard")) {
        permissions = "Read & Write";
    } else { 
        // Admins can now only choose "Read" or "Read & Write"
        ChoiceDialog<String> permissionDialog = new ChoiceDialog<>("Read", "Read", "Read & Write");
        permissionDialog.setTitle("Set Permissions");
        permissionDialog.setHeaderText("Choose permissions for the shared user:");
        Optional<String> permissionResult = permissionDialog.showAndWait();

        if (permissionResult.isEmpty()) {
            showAlert("Error", "No permission selected.");
            return;
        }
        permissions = permissionResult.get();
    }

    // Store shared file in DB
    database.shareFile(fileName, loggedInUser, sharedWithUser, permissions);

    showAlert("Success", "File shared with " + sharedWithUser + " with " + permissions + " permissions.");

    // Ensure the owner keeps full access
    editFileBtn.setDisable(false);
    deleteFileBtn.setDisable(false);
}

@FXML
private void handleViewSharedFiles() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sharedfiles.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) viewSharedFilesBtn.getScene().getWindow();
        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Shared Files");
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Error", "Failed to open Shared Files page.");
    }
}

@FXML
private void handleUploadFile() {
    if (selectedFiles.isEmpty()) {
        showAlert("Error", "No file selected.");
        LOGGER.warning("Upload Attempt Failed: No file selected.");
        return;
    }

    DB database = new DB();
    String owner = App.getLoggedInUser();
    String permissions = "Read & Write"; // Default permissions
    String userRole = App.getUserRole();
    

    for (File file : selectedFiles) {
        String filePath = file.getAbsolutePath();
        String fileName = file.getName();

        // Log file selection
        LOGGER.info("File selected for upload: " + fileName + " | Path: " + filePath);

        try {
            // Dynamically choose a storage container
            String assignedContainer = loadBalancer.getNextStorageContainer(loadBalancer.determineBestAlgorithm(), fileName);
            if (assignedContainer == null) {
                showAlert("Error", "No available storage containers.");
                return;
            }

            // Add file to Load Balancer's queue and assign a container
            loadBalancer.addToWaitingQueue(filePath, userRole);
            LOGGER.info("File assigned to container: " + assignedContainer);

            // Store metadata in database
            database.addFileMetadata(fileName, owner, permissions);
            LOGGER.info("File metadata stored: " + fileName + " | Owner: " + owner + " | Permissions: " + permissions);

            sendToTerminal("Uploading file: " + fileName);
        } catch (Exception e) {
            LOGGER.severe("Error while uploading file " + fileName + ": " + e.getMessage());
        }
    }

    try {
        database.synchronizeDatabases();
        LOGGER.info("Databases synchronized successfully after file upload.");
    } catch (Exception e) {
        LOGGER.severe("Database synchronization failed after file upload: " + e.getMessage());
    }

    showAlert("Success", "Files uploaded and databases synchronized.");
}


private void handleFileRequest(File file) {
    if (file == null) {
        showAlert("Error", "No file selected.");
        return;
    }

    System.out.println("Saving " + file.getName() + " locally...");

    // Define the directory where files should be stored
    File destinationDir = new File("local_storage"); 
    if (!destinationDir.exists()) {
        destinationDir.mkdirs(); // Create the directory if it doesn't exist
    }

    // Copy the file to the destination folder
    File destinationFile = new File(destinationDir, file.getName());
    try (InputStream in = new FileInputStream(file);
         OutputStream out = new FileOutputStream(destinationFile)) {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        showAlert("Success", "File saved successfully to: " + destinationFile.getAbsolutePath());

    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Error", "Failed to save file.");
    }
}





private void sendFileRequest(String storageURL, String fileName) {
    try {
        System.out.println("Sending request to: " + storageURL + fileName);
        java.net.URL url = new java.net.URL(storageURL + fileName);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Error", "Failed to send request to Load Balancer.");
    }
}

@FXML
private void handleDownloadFile() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Download File");
    dialog.setHeaderText("Enter the file name to download:");
    dialog.setContentText("Filename:");

    dialog.showAndWait().ifPresent(fileName -> {
        long startTime = System.currentTimeMillis();
        boolean success = loadBalancer.downloadFile(fileName);
        long endTime = System.currentTimeMillis();
        if (success) {
            sendToTerminal("Downloaded file: " + fileName);
            LOGGER.info("File downloaded successfully: " + fileName);
            showAlert("Success", "File downloaded successfully!");
        } else {
            LOGGER.severe("File download failed: " + fileName);
            showAlert("Error", "File not found in any container.");
        }
    });
}


    @FXML
private void handleStartProcessing() {
    String schedulingAlgorithm = "ROUND_ROBIN"; // You can modify this if needed
    LoadBalancer.TrafficLevel trafficLevel = loadBalancer.getTrafficLevel();
    
    loadBalancer.applyAgingMechanism();
    loadBalancer.processFilesWithSemaphore();


    loadBalancer.processFilesAutomatically(schedulingAlgorithm, trafficLevel);
    
    showAlert("Processing Started", "Files are being processed using " + schedulingAlgorithm);
}


@FXML
private void handleCheckHealth() {
    StringBuilder healthStatus = new StringBuilder("Container Health Status:\n");

    for (String container : loadBalancer.getStorageContainers()) {
        boolean healthy = loadBalancer.isContainerHealthy(container);
        healthStatus.append(container).append(": ").append(healthy ? "Healthy ✅" : "Down ❌").append("\n");
    }

    showAlert("Health Check", healthStatus.toString());
}

@FXML
private void handleOpenTerminal() {
    RemoteTerminal.listAndOpenContainerTerminal();
    try {
       FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("com/mycompany/loadbalancer/terminal.fxml"));

        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Terminal Emulator");
        stage.setScene(new Scene(root));
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Error", "Failed to open terminal.");
    }
}

private void sendToTerminal(String message) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("com/mycompany/loadbalancer/terminal.fxml"));
        if (loader.getLocation() == null) {
            System.out.println("Done.");
            return;
        }

        Parent root = loader.load();
        TerminalController terminalController = loader.getController();

        if (terminalController != null) {
            terminalController.appendMessage(message); // Send message to Terminal
        } else {
            System.out.println("TerminalController is null. Message not sent.");
        }
    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("IOException while loading terminal.fxml: " + e.getMessage());
    }
}

@FXML
public void initialize() {
    // Ensure the ListView updates the selectedFile when an item is clicked
    selectedFilesListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null) {
            for (File file : selectedFiles) {
                if (file.getName().equals(newValue)) {
                    selectedFile = file;
                    System.out.println("Selected file updated: " + selectedFile.getAbsolutePath());
                    return;
                }
            }
        }
    });
}


}





    









