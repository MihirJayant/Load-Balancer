package com.mycompany.loadbalancer;

import java.util.concurrent.atomic.AtomicInteger;

public class FileTask implements Comparable<FileTask> {
    private static final AtomicInteger counter = new AtomicInteger(0); // Unique ID for FIFO tie-break

    private final String fileName;
    private final int fileSize;
    private int priority; // Removed 'final' to allow aging updates
    private final long uploadTime;
    private final int id; // Unique identifier to ensure FIFO for same priority
    private final String userRole;

    public FileTask(String fileName, int fileSize, int priority, long uploadTime, String userRole) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.priority = priority;
        this.uploadTime = uploadTime;
        this.userRole = userRole;
        this.id = counter.incrementAndGet();
    }

    public void ageTask() {
        this.priority += 1; // Boost priority to prevent starvation
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPriority() {
        return priority;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public String getUserRole() {
        return userRole;
    }

    @Override
    public int compareTo(FileTask other) {
        // Higher priority gets processed first
        if (this.priority != other.priority) {
            return Integer.compare(other.priority, this.priority);
        }
        // If priority is the same, prefer largerr files first
        if (this.fileSize != other.fileSize) {
            return Integer.compare(other.fileSize, this.fileSize);
        }
        // If file size is also the same, prefer older files (earlier upload time)
        return Long.compare(this.uploadTime, other.uploadTime);
    }

    @Override
    public String toString() {
        return "FileTask{name=" + fileName + ", size=" + fileSize + " bytes, priority=" + priority + ", role=" + userRole + "}";
    }
}
