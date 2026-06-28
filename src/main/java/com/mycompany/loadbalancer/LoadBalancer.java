package com.mycompany.loadbalancer;

import com.mycompany.javafxapplication1.DB;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import javafx.fxml.FXML;
import javafx.scene.control.TextInputDialog;

public class LoadBalancer {
    
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5MB per chunk
    private PriorityQueue<FileTask> processingQueue = new PriorityQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock(); // Prevents race conditions
    private final Semaphore processingSemaphore = new Semaphore(2);


    
        

    private List<String> storageContainers = new ArrayList<>();
    
    private int roundRobinIndex = 0;
    private static final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    private PriorityQueue<FileTask> waitingQueue = new PriorityQueue<>();
    private Queue<FileTask> readyQueue = new LinkedList<>();
    
   

    public LoadBalancer() {
        detectStorageContainers();
       

    storageContainers = new ArrayList<>();
    storageContainers.add("comp20081-files-container1");
    storageContainers.add("comp20081-files-container2");
    storageContainers.add("comp20081-files-container3");
    storageContainers.add("comp20081-files-container4");
    storageContainers.add("comp20081-files-container5");
    storageContainers.add("comp20081-files-container6");

    
    

}
    
    // Assigns priority based on file size, upload time, and user role
    // Assigns priority based on file size, upload time, and user role
private int calculatePriority(File file, String userRole) {
    int basePriority = 10; // Default priority
    if (file.length() < 5 * 1024 * 1024) { // Files < 5MB get higher priority
        basePriority += 5;
    }
    if (userRole.equalsIgnoreCase("admin")) { // Admin users get higher priority
        basePriority += 10;
    }
    return basePriority;
}

    
    public void addToProcessingQueue(String fileName, int fileSize, String userRole) {
    queueLock.lock(); // Ensures thread safety
    try {
        int priority = calculatePriority(new File(fileName), userRole);
        long uploadTime = System.currentTimeMillis(); // Capture current upload time

        // Updated constructor with all required parameters
        FileTask task = new FileTask(fileName, fileSize, priority, uploadTime, userRole);
        
        processingQueue.add(task);
        System.out.println("Added to Processing Queue: " + task);
    } finally {
        queueLock.unlock();
    }
}

    
    public void processFiles() {
        while (!processingQueue.isEmpty()) {
            queueLock.lock(); // Prevents race conditions
            try {
                FileTask task = processingQueue.poll();
                if (task != null) {
                    System.out.println("Processing File: " + task);
                    
                }
            } finally {
                queueLock.unlock();
            }
        }
    }


    
    
    // Enum for traffic levels
    public enum TrafficLevel {
        LOW(1.0), MEDIUM(1.5), HIGH(2.0);

        private final double multiplier;

        TrafficLevel(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    // Determines traffic level based on queue size
    public TrafficLevel getTrafficLevel() {
        int queueSize = waitingQueue.size();
        if (queueSize < 5) {
            return TrafficLevel.LOW;
        } else if (queueSize < 10) {
            return TrafficLevel.MEDIUM;
        } else {
            return TrafficLevel.HIGH;
        }
    }
     public List<File> splitFile(File file) throws IOException {
        List<File> chunkFiles = new ArrayList<>();
        byte[] buffer = new byte[CHUNK_SIZE];
        int partCounter = 1;

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) > 0) {
                File chunkFile = new File(file.getParent(), file.getName() + ".part" + partCounter);
                try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                    fos.write(buffer, 0, bytesRead);
                }
                chunkFiles.add(chunkFile);
                partCounter++;
            }
        }

        System.out.println("File split into " + (partCounter - 1) + " chunks.");
        return chunkFiles;
        
    }

public void addToWaitingQueue(String fileName, String userRole) {
    File file = new File(fileName);
    if (!file.exists()) {
        System.out.println("File does not exist: " + fileName);
        return;
    }

    int fileSize = (int) file.length(); // Get file size
    int priority = calculatePriority(file, userRole); // Determine priority
    long uploadTime = System.currentTimeMillis();

    FileTask task = new FileTask(fileName, fileSize, priority, uploadTime, userRole);
    
    queueLock.lock(); // Ensure thread safety
    try {
        waitingQueue.add(task);
        System.out.println("Added to Waiting Queue: " + task);
    } finally {
        queueLock.unlock();
    }
}



    // Returns the processing queue
    public String getProcessingQueue() {
        return processingQueue.toString();
    }

    // Returns the ready queue
    public String getReadyQueue() {
        return readyQueue.toString();
    }

    // Returns the waiting queue
    public String getWaitingQueue() {
        return waitingQueue.toString();
    }

    // Returns available storage containers
    public List<String> getStorageContainers() {
        return storageContainers;
    }

    // Checks if a container is reachable
    public boolean isContainerHealthy(String containerName) {
        try {
            InetAddress address = InetAddress.getByName(containerName);
            return address.isReachable(3000); // 3-second timeout
        } catch (IOException e) {
            return false;
        }
    }
    private void updateTaskAging() {
    queueLock.lock(); // Ensure thread safety
    try {
        for (FileTask task : waitingQueue) {
            task.ageTask(); // Increment the age of each task
        }
    } finally {
        queueLock.unlock();
    }
}


    public void processFilesAutomatically(String schedulingAlgorithm, TrafficLevel trafficLevel) {
    int uploadedFiles = 0;
    int chunkedFiles = 0;
    long startTime = System.currentTimeMillis();
    
    PriorityQueue<FileTask> sortedQueue = new PriorityQueue<>(waitingQueue);
    waitingQueue = sortedQueue;
    

    while (!waitingQueue.isEmpty()) {
        applyAgingMechanism();
        FileTask task = waitingQueue.poll();
        processingQueue.add(task);

        System.out.println("Processing: " + task.getFileName());
    

       

        String assignedAlgorithm = determineBestAlgorithm();
        System.out.println("Selected Algorithm: " + assignedAlgorithm);

        // Determine the current traffic level
        TrafficLevel currentTrafficLevel = getTrafficLevel();
        System.out.println("Current Traffic Level: " + currentTrafficLevel);

        // Apply delay adjustment based on traffic level
        int baseDelay = ThreadLocalRandom.current().nextInt(30, 45);
        int adjustedDelay = (int) (baseDelay * currentTrafficLevel.getMultiplier());
        

        System.out.println("Delay: " + baseDelay + " seconds");
        System.out.println("Approximate Delay due to Traffic: " + adjustedDelay + " seconds");

        try {
            Thread.sleep(adjustedDelay * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get file to send
        File fileToSend = new File(task.getFileName());

        if (!fileToSend.exists()) {
            System.out.println("File not found: " + task.getFileName());
            processingQueue.remove(task);
            waitingQueue.add(task);
            continue;
        }
        

        if (fileToSend.length() <= CHUNK_SIZE) {
            // Small file, send normally
            String container = getNextStorageContainer(assignedAlgorithm, fileToSend.getName());
            boolean success = ScpTransfer.sendFile(fileToSend, container);

            if (success) {
                System.out.println("File successfully stored in " + container);
            } else {
                System.out.println("Failed to store file in " + container);
            }
        } else {
            // Large file, perform chunking
            splitAndDistributeFile(fileToSend);
            fileToSend.delete();
        }

        processingQueue.remove(task);
        readyQueue.add(task);
        System.out.println("File processed successfully: " + task.getFileName());
    } 
    long endTime = System.currentTimeMillis();
    logPerformanceMetrics(uploadedFiles, 0, chunkedFiles, endTime - startTime, 0);
}



    public String getNextStorageContainer(String schedulingAlgorithm, String fileName) {
        detectStorageContainers();
        if (storageContainers.isEmpty()) {
        System.out.println("No active storage containers detected.");
        return null;
        }
        switch (schedulingAlgorithm) {
            case "ROUND_ROBIN":
                return getRoundRobinContainer();
            case "FCFS":
                return getFCFSContainer();
            case "SJF":
                return getSJFContainer(fileName);
            default:
                return getRoundRobinContainer();
        }
    }

    private String getRoundRobinContainer() {
    if (storageContainers.isEmpty()) {
        System.out.println("No active storage containers detected.");
        return null;
    }
    String container = storageContainers.get(roundRobinIndex);
    roundRobinIndex = (roundRobinIndex + 1) % storageContainers.size();
    return container;
}


    private String getFCFSContainer() {
        return storageContainers.get(0);
    }

    private String getSJFContainer(String fileName) {
        int index = fileName.length() % storageContainers.size();
        return storageContainers.get(index);
    }

   public boolean downloadFile(String fileName) {
        DB database = new DB();
        List<String> chunkLocations = database.getChunkLocations(fileName);

        if (!chunkLocations.isEmpty()) {
            System.out.println("Chunked file detected. Retrieving all chunks...");
            return retrieveAndMergeChunks(fileName, chunkLocations);
        }

        System.out.println("Searching for normal file: " + fileName);
    for (String container : storageContainers) {
        if (checkFileExists(container, fileName)) {
            System.out.println("File found in container: " + container);
            File destination = new File("/home/ntu-user/Downloads/" + fileName);
            boolean success = retrieveFileFromContainer(fileName, container, destination);
            if (success) {
                System.out.println("File retrieved successfully: " + fileName);
                return true;
            }
        }
    }

    System.out.println("Download failed: File not found in any container.");
    return false;
}




    private boolean retrieveAndMergeChunks(String fileName, List<String> chunkLocations) {
    List<File> chunkFiles = new ArrayList<>();
    System.out.println("Starting chunk retrieval for file: " + fileName);
    
    detectStorageContainers();

    int totalChunks = chunkLocations.size();  // Total chunks expected
    int retrievedChunks = 0;  // Track successfully retrieved chunks

    for (int i = 1; i <= totalChunks; i++) { // Iterate for each chunk
        String chunkFileName = fileName + ".part" + i;
        File destination = new File("/home/ntu-user/Downloads/" + chunkFileName);
        boolean chunkRetrieved = false;

        for (String container : storageContainers) {
            System.out.println("Checking for chunk: " + chunkFileName + " in container: " + container);

            if (checkFileExists(container, chunkFileName)) {
                System.out.println("Retrieving chunk: " + chunkFileName + " from " + container);

                boolean success = retrieveFileFromContainer(chunkFileName, container, destination);
                if (success) {
                    chunkFiles.add(destination);
                    retrievedChunks++;
                    System.out.println("Chunk " + i + " retrieved successfully from " + container);
                    chunkRetrieved = true;
                    break; // Move to next chunk after retrieval
                }
            }
        }

        if (!chunkRetrieved) {
            System.err.println("⚠ Warning: Chunk " + i + " not found in any container. Will attempt to merge available chunks.");
            break; // Continue merging even if a chunk is missing
        }
    }
    if (retrievedChunks == totalChunks) {
        System.out.println("✅ All chunks retrieved successfully!");
    } else {
        System.err.println("⚠ Partial chunks retrieved! Attempting to merge available parts...");
    }

    return mergeChunks(chunkFiles, "/home/ntu-user/Downloads/" + fileName);
}

private boolean mergeChunks(List<File> chunks, String outputPath) {
    if (chunks.isEmpty()) {
        System.err.println("Merge failed: No chunks available.");
        return false;
    }

    System.out.println("Merging " + chunks.size() + " retrieved chunks into " + outputPath);
    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
        for (File chunk : chunks) {
            try (FileInputStream fis = new FileInputStream(chunk)) {
                byte[] encryptedData = fis.readAllBytes();
                
                // Decrypt chunk before writing
                byte[] decryptedData = EncryptionUtil.decrypt(encryptedData);

                fos.write(decryptedData);
            }
        }
        System.out.println("File merged successfully: " + outputPath);
        return true;
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }
}



   private boolean retrieveFileFromContainer(String fileName, String container, File destination) {
    try {
        System.out.println("Retrieving file from " + container);

        // Define the file path inside the container
        String containerFilePath = "/home/ntu-user/stored-files/" + fileName;

        // SCP Command with Debugging
        ProcessBuilder scpProcess = new ProcessBuilder(
            "scp", "-v", "-o", "StrictHostKeyChecking=no",
            "ntu-user@" + container + ":" + containerFilePath, destination.getAbsolutePath()
        );

        Process scp = scpProcess.start();
        int scpExitCode = scp.waitFor();

        if (scpExitCode == 0) {
            System.out.println("File successfully retrieved: " + fileName);
        } else {
            System.out.println("Failed to retrieve file: " + fileName);
            return false;
        }

        // Run SSH to delete the file inside the container (Optional)
        ProcessBuilder sshProcess = new ProcessBuilder(
            "ssh", "-o", "StrictHostKeyChecking=no",
            "ntu-user@" + container, "rm", "-f", containerFilePath
        );

        Process ssh = sshProcess.start();
        int sshExitCode = ssh.waitFor();

        if (sshExitCode == 0) {
            System.out.println("File moved successfully: " + fileName + " removed from " + container);
            return true;
        } else {
            System.out.println("Failed to delete file from storage container: " + fileName);
            return false;
        }

    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        return false;
    }
}



    private String getFileContainer(String fileName) {
    for (String container : storageContainers) {
        System.out.println("Checking container: " + container);
        if (checkFileExists(container, fileName)) {
            System.out.println("File found in: " + container);
            return container; // Return the container that holds the file
        }
    }
    System.out.println("File not found in any container.");
    return null; // No container found with the file
}

private boolean checkFileExists(String container, String fileName) {
    try {
        DB database = new DB();
        List<String> chunkLocations = database.getChunkLocations(fileName);

        boolean isChunked = !chunkLocations.isEmpty();
        String searchPattern = isChunked
            ? "/home/ntu-user/stored-files/" + fileName + ".*" // Check for chunks
            : "/home/ntu-user/stored-files/" + fileName;       // Check for full file

        System.out.println("Checking for " + (isChunked ? "chunks" : "full file") + " in container: " + container);

        ProcessBuilder processBuilder = new ProcessBuilder(
            "ssh", "ntu-user@" + container, "ls " + searchPattern
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = reader.readLine();

        process.waitFor();
        return output != null && !output.isEmpty();
    } catch (IOException | InterruptedException e) {
        System.err.println("Container unreachable: " + container);
        return false;
    }
}



    
    public boolean deleteFile(String fileName) {
    String container = getFileContainer(fileName);
    if (container == null) {
        System.out.println("Delete failed: File not found in any container.");
        return false;
    }

    lockFile(fileName);
    try {
        InetAddress address = InetAddress.getByName(container);
        if (!address.isReachable(3000)) {
            System.out.println("Container unreachable: " + container);
            return false;
        }

        System.out.println("Attempting to delete file: " + fileName + " from container: " + container);

        // Use SSH to delete the file from the container
        ProcessBuilder processBuilder = new ProcessBuilder(
            "ssh", "ntu-user@" + container, "rm -f /home/ntu-user/stored-files/" + fileName
        );
        Process process = processBuilder.start();
        process.waitFor();

        if (process.exitValue() == 0) {
            System.out.println("File deleted successfully from container: " + container);
            return true;
        } else {
            System.out.println("Failed to delete file from container.");
            return false;
        }
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        return false;
    } finally {
        unlockFile(fileName);
    }
}
    
   
    private void lockFile(String fileName) {
        fileLocks.putIfAbsent(fileName, new ReentrantLock());
        fileLocks.get(fileName).lock();
    }

    private void unlockFile(String fileName) {
        if (fileLocks.containsKey(fileName)) {
            fileLocks.get(fileName).unlock();
        }
    }
    
    /**
 * Returns the number of files in the waiting queue.
 */
public int getWaitingQueueSize() {
    return waitingQueue.size();
}

/**
 * Calculates the average file size in the queue (Assumes metadata is available).
 */
public long getAverageFileSize() {
    long totalSize = 0;
    int fileCount = 0;

    for (FileTask task : waitingQueue) {
        File file = new File(task.getFileName());
        if (file.exists()) {
            totalSize += file.length();
            fileCount++;
        }
    }

    return (fileCount > 0) ? totalSize / fileCount : 0; // Prevent divide by zero
}

/**
 * Determines the best scheduling algorithm based on the queue state.
 */
public String determineBestAlgorithm() {
    int waitingQueueSize = getWaitingQueueSize();
    long avgFileSize = getAverageFileSize();

    if (waitingQueueSize <= 3) {
        return "FCFS"; // Use FCFS if queue is small (less than 3 files)
    } else if (waitingQueueSize > 3 && avgFileSize < 2 * 1024 * 1024) { // Less than 2MB
        return "SJF"; // Use SJF if many small files exist
    } else if (waitingQueueSize > 7) {
        return "ROUND_ROBIN"; // High traffic, distribute evenly
    } else {
        return (ThreadLocalRandom.current().nextInt(0, 2) == 0) ? "FCFS" : "SJF"; // Randomly pick between FCFS and SJF if queue is moderate
    }
}




private void detectStorageContainers() {
    storageContainers.clear();
    DB database = new DB(); // Database instance to fetch container details

    try {
        // Fetch active containers from the database
        List<String> dbContainers = database.getActiveStorageContainers();
        storageContainers.addAll(dbContainers);

        // Ensure manually defined containers are also included (if any exist)
        int maxContainers = 10; // Maintain scalability limits
        for (int i = 1; i <= maxContainers; i++) {
            String manualContainerName = "comp20081-files-container" + i;
            if (!storageContainers.contains(manualContainerName) && pingContainer(manualContainerName)) {
                storageContainers.add(manualContainerName);
            }
        }

        System.out.println("Active Storage Containers: " + storageContainers);

    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Failed to detect storage containers from the database.");
    }
}


// Check if container is reachable
private boolean pingContainer(String container) {
    try {
        Process process = new ProcessBuilder("docker", "inspect", "--format", "{{.State.Running}}", container)
                .start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = reader.readLine();
        return "true".equals(output);
    } catch (IOException e) {
        return false;
    }
}

private void splitAndDistributeFile(File file) {
    byte[] buffer = new byte[CHUNK_SIZE];
    int chunkIndex = 1;
    DB database = new DB();
    
    detectStorageContainers();

    // Ensure no old chunk metadata exists
    database.deleteChunkMetadata(file.getName());

    try (FileInputStream fis = new FileInputStream(file)) {
        int bytesRead;
        
        while ((bytesRead = fis.read(buffer)) > 0) {
            File chunkFile = new File(file.getParent(), file.getName() + ".part" + chunkIndex);
            
            byte[] encryptedData = EncryptionUtil.encrypt(buffer, bytesRead);
            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                fos.write(encryptedData);
            }

            
            String assignedContainer = storageContainers.isEmpty()
        ? getNextStorageContainer("ROUND_ROBIN", chunkFile.getName()) // Fallback to default logic
        : storageContainers.get(roundRobinIndex % storageContainers.size()); // Use detected active containers

roundRobinIndex = (roundRobinIndex + 1) % storageContainers.size();
            boolean success = ScpTransfer.sendFile(chunkFile, assignedContainer);

            // Store encrypted flag in DB
            database.addChunkMetadata(file.getName(), chunkIndex, assignedContainer, true);

            if (success) {
                System.out.println("Chunk " + chunkIndex + " stored securely in " + assignedContainer);
            } else {
                System.out.println("Failed to store chunk " + chunkIndex);
            }

            chunkFile.delete();
            chunkIndex++;
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}


private void logPerformanceMetrics(int uploadedFiles, int downloadedFiles, int chunkedFiles, long processingTime, int failedTransfers) {
    String filePath = "performance_metrics.txt";  // Save in project directory

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) { // Overwrite the file
        writer.write("=== Load Balancer Performance Metrics ===\n");
        writer.write("Last Updated: " + LocalDateTime.now() + "\n\n");

        writer.write("Total Files Uploaded: " + uploadedFiles + "\n");
        writer.write("Total Files Downloaded: " + downloadedFiles + "\n");
        writer.write("Total Chunked Files Processed: " + chunkedFiles + "\n");
        writer.write("Average Processing Time per File: " + processingTime + " ms\n");
        writer.write("Failed Transfers: " + failedTransfers + "\n\n");

        writer.write("Queue Sizes:\n");
        writer.write("   - Waiting Queue: " + waitingQueue.size() + "\n");
        writer.write("   - Processing Queue: " + processingQueue.size() + "\n");
        writer.write("   - Ready Queue: " + readyQueue.size() + "\n\n");

        writer.write("Storage Container Health:\n");
        for (String container : storageContainers) {
            boolean isHealthy = isContainerHealthy(container);
            writer.write("   - " + container + ": " + (isHealthy ? "Healthy" : "Unreachable") + "\n");
        }
        
        writer.write("\n=======================================\n");
        System.out.println("Performance metrics updated in performance_metrics.txt");

    } catch (IOException e) {
        e.printStackTrace();
    }
}
public void applyAgingMechanism() {
    queueLock.lock();
    try {
        PriorityQueue<FileTask> updatedQueue = new PriorityQueue<>();
        long currentTime = System.currentTimeMillis(); // Get current time

        while (!processingQueue.isEmpty()) {
            FileTask task = processingQueue.poll();
            long elapsedTime = (currentTime - task.getUploadTime()) / 30000; // Convert ms to minutes

            int newPriority = task.getPriority() + (int) elapsedTime; // Increase priority based on time
            FileTask updatedTask = new FileTask(task.getFileName(), task.getFileSize(), newPriority, task.getUploadTime(), task.getUserRole());

            updatedQueue.add(updatedTask);
        }

        processingQueue = updatedQueue;
        System.out.println("Aging applied based on time. Updated queue: " + processingQueue);
    } finally {
        queueLock.unlock();
    }
}








public void processFilesWithSemaphore() {
    while (!processingQueue.isEmpty()) {
        try {
            processingSemaphore.acquire(); // Wait for a processing slot
            queueLock.lock();
            FileTask task = processingQueue.poll();
            queueLock.unlock();

            if (task != null) {
                System.out.println("Processing File: " + task);
                Thread.sleep(30000); // Simulating file processing delay
                System.out.println("Processing complete: " + task.getFileName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            processingSemaphore.release(); // Release processing slot
        }
    }
}





}
