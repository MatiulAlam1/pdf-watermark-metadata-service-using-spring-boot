
# PDF Watermark Service

## Description
The **PDF Watermark Service** is a secure RESTful web service designed to add watermarks and custom metadata to PDF files. Users can upload single or multiple PDFs, which are then processed to include the **company logo**, optional user ID/name, and other metadata. The service ensures high availability, security, and scalability.

### Key Features
* **User Authentication & Authorization:** Secure access with Spring Security, OAuth2, and JWT.
* **Dynamic Watermarking:** Automatically adds the company logo to each page.
* **Custom User Stamp:** Optional "Downloaded by [ID/Name]" watermark on each page's bottom-left.
* **Single & Multiple File Handling:** Returns a watermarked PDF for single files, and a ZIP archive for multiple files.
* **File Validation:** Accepts only PDF files, with configurable size limits (up to 800MB).
* **High Availability & Fault Tolerance:** Multi-instance deployment with load balancing via Nginx, resilience patterns using Resilience4j, retry mechanisms, and circuit breakers.
* **Custom Metadata & Keywords:** Adds download date, person ID, and system information to each PDF.

## Technology Stack
* **Backend:** Java 17+, Spring Boot
* **Web Server:** Embedded Tomcat
* **Reverse Proxy & Load Balancer:** Nginx (HTTPS, load balancing)
* **Security:** Spring Security 6, OAuth2, JWT, LDAP authentication, Spring Vault
* **Secret Management:** HashiCorp Vault (RSA key pairs, passwords)
* **PDF Manipulation:** iTextPDF 7.2.4 (`commons`, `io`, `kernel`, `layout`)
* **Build Tool:** Apache Maven
* **API Documentation:** Swagger / OpenAPI
* **Logging:** SLF4J with Logback
* **Testing:** JUnit
* **Fault Tolerance:** Resilience4j (Circuit Breaker, Retry, Bulkhead)
* **Monitoring:** Prometheus & Grafana
* **Secure Communication:** SSL/TLS with PKCS#12 keystore

## Architecture Overview
1. **Client:** Sends HTTPS requests to the service.
2. **Nginx:** Terminates SSL/TLS, reverse proxies requests, and load balances across multiple Spring Boot instances (ports e.g., 8081, 8082).
3. **Spring Boot Instances:** Each instance processes PDF uploads, applies watermarks, and responds to clients.
4. **HashiCorp Vault:** Manages sensitive data and RSA keys for JWT signing and encryption. Keys rotated monthly.
5. **Resilience4j:** Handles fault tolerance with Circuit Breaker, Retry, and Bulkhead patterns.

## Implementation Process
### Deployment & Load Balancing
* Multiple Spring Boot instances deployed on different ports to enable horizontal scaling.
* Nginx receives client requests on port 443 and distributes traffic evenly across instances.
* Ensures fault tolerance and high availability.

### Security & Authentication Flow
1. Client requests access token with credentials.
2. Nginx forwards request to Spring Boot instance.
3. Spring Security + LDAP validates credentials.
4. JWT is signed with private RSA key from Vault and returned to client (access + refresh token).
5. Client includes access token in `Authorization` header for protected endpoints.
6. JWT is verified against Vault's public key; Spring Security grants or denies access.

### Fault Tolerance
* Circuit Breaker prevents repeated failures.
* Retry automatically handles temporary failures.
* Bulkhead limits concurrent requests to ensure stability.

### SSL/TLS Configuration
* HTTPS enabled via `keystore.p12` file.
* Configured in `application.properties` for SSL/TLS support.

## REST API Endpoints
### 1. Authentication
* **POST** `/api/authenticate`
* Request Body: `{ "username": "user", "password": "pass" }`
* Returns: Access token & refresh token (JWT)

### 2. Add Watermark
* **POST** `/api/watermark`
* Headers: `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`
* Form Data:
  * `file`: PDF(s) to watermark
  * `personID` (optional)
  * `email` (optional)
  * `system` (optional)
* Response:
  * Single file → watermarked PDF (`application/pdf`)
  * Multiple files → ZIP archive (`application/zip`)

### 3. Renew Access Token
* **POST** `/api/renewToken`
* Request Body: `{ "refreshToken": "<token>" }`
* Response: New access token + original refresh token

## Business Logic & Watermark Process
1. Validate PDF file type & size.
2. Save file temporarily on server.
3. Generate "Downloaded By [ID/System]" image if provided.
4. Apply watermark logo & optional user image on all pages.
5. Add custom metadata (download date, person ID, system).
6. Return PDF or ZIP to client.
7. Delete temporary files after response.

## Watermark Settings (`watermark-settings.properties`)
* `opacity`: Watermark brightness
* `fontColor`: Watermark text color (default black)
* `xAxis`, `yAxis`: Watermark positioning
* `fontName`, `fontStyle`: Font settings for user stamp

## Security & Secret Management
* RSA key pairs are securely stored in HashiCorp Vault.
* JWT access tokens are short-lived; refresh tokens can renew access.
* Regular key rotation ensures high security.

## Monitoring & Logging
* Prometheus + Grafana for metrics and health monitoring.
* SLF4J + Logback for structured logging.
* Resilience4j metrics monitored for circuit breaker & retry events.

## Summary
This service provides a **secure, scalable, and fault-tolerant** mechanism for watermarking PDF files. It supports **user-specific tracking**, multiple file handling, and ensures **high availability** and **resilient performance**.
