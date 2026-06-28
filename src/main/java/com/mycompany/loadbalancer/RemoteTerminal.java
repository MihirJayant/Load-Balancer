package com.mycompany.loadbalancer;

import com.jcraft.jsch.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Scanner;

public class RemoteTerminal {
    
    private static final String USER = "ntu-user"; // Change if needed
    private static final String PASSWORD = "ntu-user"; // Change if needed
    private static final int SSH_PORT = 22;

    public static void openTerminal(String containerName) {
        try {
            JSch jsch = new JSch();

            // Set known hosts to avoid authentication issues
            jsch.setKnownHosts("/home/ntu-user/.ssh/known_hosts");

            // Establish SSH session
            Session session = jsch.getSession(USER, containerName, SSH_PORT);
            session.setPassword(PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no"); // Disable host key checking
            session.connect(30000); // Timeout: 30 seconds

            // Open shell channel for interactive command execution
            ChannelShell channel = (ChannelShell) session.openChannel("shell");

            // Set up input/output streams for real-time interaction
            InputStream inputStream = System.in;
            OutputStream outputStream = System.out;
            channel.setInputStream(inputStream);
            channel.setOutputStream(outputStream);

            // Start the shell session
            channel.connect(5000); // Timeout: 5 seconds
            System.out.println("Connected to container: " + containerName);
            
            // Keep the session open until user exits
            while (!channel.isClosed()) {
                Thread.sleep(500);
            }

            // Close resources
            channel.disconnect();
            session.disconnect();
            System.out.println("Disconnected from container: " + containerName);

        } catch (Exception e) {
            System.err.println("Error opening terminal for " + containerName + ": " + e.getMessage());
        }
    }

    // Utility method to list all available containers
    public static void listAndOpenContainerTerminal() {
        LoadBalancer loadBalancer = new LoadBalancer();
        List<String> containers = loadBalancer.getStorageContainers();

        if (containers.isEmpty()) {
            System.out.println("No active containers found.");
            return;
        }

        System.out.println("Available Containers:");
        for (int i = 0; i < containers.size(); i++) {
            System.out.println((i + 1) + ". " + containers.get(i));
        }

        // Let the user choose a container
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter container number to access: ");
        int choice = scanner.nextInt();
        
        if (choice < 1 || choice > containers.size()) {
            System.out.println("Invalid choice. Exiting...");
            return;
        }

        // Open terminal for selected container
        openTerminal(containers.get(choice - 1));
    }
}

