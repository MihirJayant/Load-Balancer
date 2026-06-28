package com.mycompany.loadbalancer;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TerminalController {
    
 

    @FXML
    private TextField commandInput;

    @FXML
    private TextArea commandOutput;

    @FXML
    public void handleRunCommand() {
        String command = commandInput.getText().trim();

        if (command.isEmpty()) {
            commandOutput.setText("⚠️ Please enter a command.");
            return;
        }

        try {
            // Execute command inside LoadBalancer container
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
            commandOutput.setText(output.toString());

        } catch (Exception e) {
            commandOutput.setText("❌ Error executing command: " + e.getMessage());
        }
    }
    public void appendMessage(String message) {
    commandOutput.appendText(message + "\n");
    }
}
