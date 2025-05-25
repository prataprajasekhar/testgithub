# Timesheet Application Backend

## Project Description

This is the backend for a Timesheet Management Application. It provides RESTful APIs for managing users (employees, managers, super admins), timesheets, and authentication. The application uses Spring Boot, Spring Security for authentication and authorization with JWT, Spring Data JPA for database interactions, and H2 as an in-memory database.

## Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Building the Application
To build the application, navigate to the `timesheet-app-backend` directory and run:
```bash
mvn clean install
```
This will compile the code, run tests, and package the application into a JAR file located in the `target` directory.

### Running the Application
Once the application is built, you can run it using:
```bash
mvn spring-boot:run
```
Alternatively, you can run the JAR file directly:
```bash
java -jar target/timesheet-app-backend-0.0.1-SNAPSHOT.jar
```
The application will start on the default port `8080`.

## H2 Database Console

The application uses an in-memory H2 database. You can access its console via your web browser:

-   **URL**: `http://localhost:8080/h2-console`
-   **JDBC URL**: `jdbc:h2:mem:timesheetdb` (This was set in `application.properties`)
-   **Username**: `sa`
-   **Password**: `password` (This was set in `application.properties`)

Make sure to use the correct JDBC URL when connecting.

## Seeded Users

Upon application startup, the following users are seeded into the database with the default password `password` for all:

1.  **Super Admin**:
    *   Username: `superadmin`
    *   Password: `password`
    *   Role: `ROLE_SUPER_ADMIN`
    *   Email: `superadmin@example.com`

2.  **Manager**:
    *   Username: `manager`
    *   Password: `password`
    *   Role: `ROLE_MANAGER`
    *   Email: `manager@example.com`

3.  **Employee**:
    *   Username: `employee`
    *   Password: `password`
    *   Role: `ROLE_EMPLOYEE`
    *   Email: `employee@example.com`
    *   Managed by: `manager`

These users can be used for testing different functionalities and roles within the application.

## API Documentation (Swagger/OpenAPI)

API documentation is generated using Springdoc OpenAPI and is accessible via Swagger UI.

-   **Swagger UI URL**: `http://localhost:8080/swagger-ui.html`
-   **OpenAPI Spec (JSON)**: `http://localhost:8080/api-docs`

The Swagger UI provides an interactive way to explore the API endpoints, view request/response schemas, and test the API calls directly from the browser.

### API Structure Overview

The API is structured based on user roles and functionalities:

-   `/api/auth/**`: Authentication endpoints (login, register).
-   `/api/admin/**`: Endpoints for Super Admin users (e.g., managing users, assigning managers).
-   `/api/manager/**`: Endpoints for Manager users (e.g., viewing pending timesheets for their team, approving/rejecting timesheets, viewing managed employees).
-   `/api/employee/**`: Endpoints for Employee users (e.g., creating, viewing, updating, deleting their own timesheets).

Please refer to the Swagger UI for detailed information on each endpoint, including request parameters, request bodies, and response schemas.
