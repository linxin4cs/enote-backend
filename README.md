# ENote-Backend
English | [中文](./README.zh-CN.md) 

This code repository contains the backend source code for the Cloud Note Platform project, developed as part of an undergraduate thesis. The backend service handles the core business logic, data persistence, and API provisioning for the cloud note-taking application.

## Project Overview

The backend is built with Java and Spring Boot 3, adopting a frontend-backend separation architecture. It handles all business logic, including user authentication, note management, data storage and retrieval, and exposes RESTful API endpoints for the frontend application. The system uses multiple databases (MySQL, MongoDB, Redis) to optimize storage and access efficiency for different types of data.

## Key Features

* **User Authentication & Authorization:**
  * APIs for user registration, login, and logout;
  * Password encryption and verification;
  * "Remember Me" functionality (via Spring Security `persistent_logins`);
  * Password reset flow (including email code sending and validation);
  * Role-based access control (Regular User, Admin, Super Admin);
  * Account enable/disable status management;
  * Security protections based on Spring Security.

* **Note Management:**
  * CRUD operations for notes;
  * Support for rich-text content (stored in MongoDB as both plain text and HTML);
  * Auto/manual save logic for notes;
  * Paginated note list retrieval with filtering by folder, tags, favorites, and keywords;
  * Case-insensitive fuzzy search by keyword;
  * APIs for favoriting/unfavoriting, moving, tagging, and untagging notes.

* **Folder & Tag Management:**
  * CRUD APIs for folders and tags;
  * API to move folders.

* **File Management:**
  * APIs for uploading and accessing user avatars and embedded media files (images/videos/audio) in notes;
  * File access control;
  * Auto-cleanup logic for files linked to note content.

* **User Management (Admin Features):**
  * Admin APIs for querying, editing (permissions, status), and deleting users;
  * Permission validation to prevent lower-privilege admins from modifying higher-privilege accounts.

* **Data Statistics & Analysis (Admin Features):**
  * APIs for statistics on users, notes, and files (including totals, growth, activity, and storage usage).

* **Platform Data Maintenance (Super Admin Features):**
  * Basic APIs for data backup and restore (It's simplified in implementation).

* **General Features:**
  * Global exception handling;
  * Unified API response format;
  * Database operations simplified with MyBatis-Plus;
  * Email service (for verification codes and notifications);
  * Use of Redis for storing verification codes and session/token data.

## Technology Stack

* **Core Framework:** Spring Boot 3.x  
* **Language:** Java 17+  
* **Security:** Spring Boot Security  
* **Database Layer:** MyBatis-Plus  
* **Databases Used:**  
  * MySQL: Stores structured data (users, folders, tags, note metadata, file metadata)  
  * MongoDB: Stores document-style data (note content, search history)  
  * Redis: Stores key-value cache (verification codes, session info)  
* **Email Service:** Spring Boot Mail  
* **Build Tool:** Maven / Gradle  
* **API Style:** RESTful  
* **Common Dependencies:** Lombok (to reduce boilerplate), etc.

## Architecture Overview

The project follows a classic three-layer architecture (Controller, Service, Mapper/DAO):

* **Controller Layer:** Handles HTTP requests, calls the Service layer, and returns JSON responses;
* **Service Layer:** Implements core business logic, performs validation and transaction control, and calls the Mapper layer for DB access;
* **Mapper/DAO Layer:** Uses MyBatis-Plus for MySQL operations, and provides interfaces for MongoDB and Redis (via Spring Data).

Database responsibilities:

* **MySQL:** For structured data with transaction consistency;
* **MongoDB:** For rich note content and search history with document flexibility;
* **Redis:** For temporary data (e.g., verification codes) to improve access efficiency and reduce main database load.

Deployment is recommended with Nginx as a reverse proxy. Databases and Redis can be managed using Docker.

## Project Structure

```
enote-backend/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/yourpackage/enote/ # Root package
│       │       ├── EnoteBackendApplication.java # Main application class
│       │       ├── config/ # Configurations (security, database, web, etc.)
│       │       ├── controller/ # Controllers
│       │       ├── domain/ # Entity classes
│       │       ├── dto/ # Request/response DTOs
│       │       ├── interceptor/ # Interceptors/filters
│       │       ├── mapper/ # MyBatis Mapper interfaces
│       │       ├── repository/ # MongoDB repositories
│       │       └── service/ # Service interfaces
│       │           └── impl/ # Service implementations
│       └── resources/
│           ├── application.yaml # Configuration file
│           └── mapper/ # MyBatis XML mapping files
├── pom.xml # Maven config file
└── ... # Other files (e.g., .gitignore)
```

## Running Locally

**Prerequisites:**

* JDK 17+
* Maven 3.6+ or Gradle 7.x+
* MySQL server (e.g., 8.0)
* MongoDB server (e.g., 5.x or 6.x)
* Redis server (e.g., 6.x)
* Optional: Docker and Docker Compose (for quick setup of databases)

**Installation and Startup Steps:**

1. **Clone the repository:**
    ```bash
    git clone https://github.com/linxin4cs/enote-backend
    cd enote-backend
    ```

2. **Database Setup:**
    * Ensure MySQL, MongoDB, and Redis services are running;
    * **MySQL:** Create a database `enote`, and execute any provided SQL scripts to initialize tables, or enable auto table creation with MyBatis-Plus (not recommended for production);
    * **MongoDB:** No need to manually create collections; they are created on first write;
    * **Redis:** Ensure the service is accessible.

3. **Application Configuration:**
    * Edit the `application.yml` file:
      ```yaml
      server:
        port: 8080

      spring:
        datasource:
          url: jdbc:mysql://localhost:3306/enote?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: your_mysql_user
          password: your_mysql_password
          driver-class-name: com.mysql.cj.jdbc.Driver

        data:
          mongodb:
            uri: mongodb://localhost:27017/enote_notes
          redis:
            host: localhost
            port: 6379
            # password: your_redis_password

        mail:
          host: smtp.example.com
          port: 587
          username: your_email@example.com
          password: your_email_password_or_app_code
          properties:
            mail:
              smtp:
                auth: true
                starttls:
                  enable: true
      ```

4. **Build the project (optional):**
    ```bash
    mvn clean package -DskipTests
    # Or with Gradle:
    # ./gradlew build -x test
    ```

5. **Run the project:**
    ```bash
    mvn spring-boot:run
    # Or with the packaged JAR:
    # java -jar target/enotebackend-0.0.1-SNAPSHOT.jar
    ```
    Default service URL: `http://localhost:8080`

## API Documentation

The API follows RESTful conventions, and all available endpoints are defined in the `src/main/java/.../controller/` directory. You can use tools like Postman or Apifox for testing.

Frontend project repository: [ENote-Frontend](https://github.com/linxin4cs/enote-frontend)