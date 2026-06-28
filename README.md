# Load Balancer & Secure File Transfer

A JavaFX desktop app that handles intelligent load balancing and secure file transfers. Perfect for distributing work across multiple servers and managing files with encryption.

## What's Inside 🎯

### Load Balancing
- Spreads tasks across multiple servers
- Smart priority system (small files first, admin users prioritized)
- Concurrent processing with 6 storage containers
- Round-robin distribution to prevent bottlenecks

### Secure File Transfer
- End-to-end encryption for all files
- User authentication with role-based access
- Chunked file transfer (5MB per chunk)
- Remote terminal access via SSH

## Features 🚀

✅ **Smart File Distribution** - Uses priority queues to handle files efficiently  
✅ **Encryption** - AES encryption for all transferred data  
✅ **User Management** - Admins and regular users with different permissions  
✅ **Remote Access** - SSH terminal for remote server management  
✅ **Concurrent Processing** - Thread-safe operations with semaphores and locks  
✅ **Database Support** - SQLite for local testing, MySQL for production  

## Quick Start

### Prerequisites
- Java 11 or higher
- Maven
- Git

### Build & Run
```bash
git clone https://github.com/MihirJayant/Load-Balancer.git
cd Load-Balancer
mvn clean install
mvn javafx:run
```

## How It Works

**Uploading a File:**
1. Select file in the GUI
2. System calculates priority (smaller files get processed faster)
3. File gets encrypted
4. Split into 5MB chunks
5. Distributed across storage containers
6. Database tracks everything

**Downloading a File:**
1. Pick file from shared files list
2. Chunks are retrieved and reassembled
3. Data gets decrypted
4. Safely transferred to your machine

## Project Structure ├── src/main/java/com/mycompany/

│   ├── loadbalancer/          # Core balancing & transfer logic

│   │   ├── LoadBalancer.java  # Main orchestrator

│   │   ├── EncryptionUtil.java

│   │   ├── ScpTransfer.java

│   │   └── RemoteTerminal.java

│   └── javafxapplication1/    # UI & Database

│       ├── DB.java

│       ├── UserManagementController.java

│       └── FileManagementController.java

├── pom.xml

└── README.md   ## Tech Stack
- **UI:** JavaFX
- **Build:** Maven
- **Database:** SQLite / MySQL
- **Encryption:** AES
- **File Transfer:** SSH/SCP

## Security Notes
- Files are encrypted before transfer
- User passwords are hashed in database
- SSH-based remote access
- Role-based access control
- Thread-safe concurrent operations

## Future Ideas
- Cloud storage integration
- Web dashboard
- Real-time monitoring
- Mobile app companion
- Advanced load balancing algorithms

---

Made with ☕ and Java. Questions? Open an issue!
