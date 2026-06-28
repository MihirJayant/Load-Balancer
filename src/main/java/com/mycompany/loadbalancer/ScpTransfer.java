package com.mycompany.loadbalancer;

import com.jcraft.jsch.*;
import java.io.File;
import java.util.List;
import java.util.Properties;

public class ScpTransfer {

    private static final String LOAD_BALANCER_HOST = "ntu-load-balancer";  // First stop
    private static final String USERNAME = "ntu-user";
    private static final String PASSWORD = "ntu-user";
    private static final int REMOTE_PORT = 22;
    private static final int SESSION_TIMEOUT = 10000;
    private static final int CHANNEL_TIMEOUT = 5000;

    private static int roundRobinIndex = 0;  // Used for Round Robin

    private static LoadBalancer loadBalancer = new LoadBalancer(); // Use LoadBalancer to get storage containers

    public static boolean sendFile(File file, String schedulingAlgorithm) {
        // Step 1: Send File to Load Balancer
        if (!transferFile(file, LOAD_BALANCER_HOST, "/tmp/stored-files/")) {
            System.out.println("Failed to send file to Load Balancer!");
            return false;
        }

        // Step 2: Forward File from Load Balancer to the appropriate storage container
        List<String> availableContainers = loadBalancer.getStorageContainers();
        if (availableContainers.isEmpty()) {
            System.out.println("No available storage containers found.");
            return false;
        }

        String assignedContainer = getNextStorageContainer(schedulingAlgorithm, file.getName(), availableContainers);
        if (!transferFileFromLoadBalancer(file.getName(), assignedContainer, "/tmp/stored-files/", "/home/ntu-user/stored-files/")) {
            System.out.println("Failed to send file to Storage Container: " + assignedContainer);
            return false;
        }

        return true;
    }

    // Selects the next storage container based on the scheduling algorithm
    private static String getNextStorageContainer(String schedulingAlgorithm, String fileName, List<String> availableContainers) {
        switch (schedulingAlgorithm) {
            case "ROUND_ROBIN":
                return getRoundRobinContainer(availableContainers);
            case "FCFS":
                return getFCFSContainer(availableContainers);
            case "SJF":
                return getSJFContainer(fileName, availableContainers);
            default:
                return getRoundRobinContainer(availableContainers);
        }
    }

    // Round Robin Algorithm
    private static String getRoundRobinContainer(List<String> availableContainers) {
        String container = availableContainers.get(roundRobinIndex);
        roundRobinIndex = (roundRobinIndex + 1) % availableContainers.size();
        return container;
    }

    // First Come, First Serve (FCFS) - Always sends to the first container
    private static String getFCFSContainer(List<String> availableContainers) {
        return availableContainers.get(0);
    }

    // Shortest Job First (SJF) - Chooses the container based on file name length (mock logic)
    private static String getSJFContainer(String fileName, List<String> availableContainers) {
        int index = fileName.length() % availableContainers.size();  // Simple hashing for demo 
        return availableContainers.get(index);
    }

    // Transfers a file to a remote host
    private static boolean transferFile(File file, String remoteHost, String remoteDirectory) {
        Session jschSession = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            jschSession = jsch.getSession(USERNAME, remoteHost, REMOTE_PORT);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            jschSession.setConfig(config);
            jschSession.setPassword(PASSWORD);
            jschSession.connect(SESSION_TIMEOUT);

            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);
            channelSftp = (ChannelSftp) sftp;

            String remoteFilePath = remoteDirectory + file.getName();

            // Ensure the directory exists
            try {
                channelSftp.stat(remoteDirectory);
            } catch (SftpException e) {
                System.out.println("Creating directory: " + remoteDirectory);
                channelSftp.mkdir(remoteDirectory);
            }

            // Upload the file
            channelSftp.put(file.getAbsolutePath(), remoteFilePath);
            System.out.println("File uploaded to: " + remoteFilePath + " on " + remoteHost);

            channelSftp.exit();
            return true;

        } catch (JSchException | SftpException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }
    }

    // Transfers a file from Load Balancer to a storage container
    private static boolean transferFileFromLoadBalancer(String fileName, String storageHost, String loadBalancerDir, String storageDir) {
        Session jschSession = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            jschSession = jsch.getSession(USERNAME, LOAD_BALANCER_HOST, REMOTE_PORT);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            jschSession.setConfig(config);
            jschSession.setPassword(PASSWORD);
            jschSession.connect(SESSION_TIMEOUT);

            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);
            channelSftp = (ChannelSftp) sftp;

            // Temporary local file path
            String tempFilePath = "/tmp/" + fileName;

            // Download the file from Load Balancer
            channelSftp.get(loadBalancerDir + fileName, tempFilePath);
            channelSftp.exit();
            jschSession.disconnect();

            // Now upload it to the selected storage container
            return transferFile(new File(tempFilePath), storageHost, storageDir);

        } catch (JSchException | SftpException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }
    }
}
