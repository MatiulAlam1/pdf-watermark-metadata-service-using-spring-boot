# Add Watermark to the PDF Service

## Description

The Service is a RESTful web service designed to add some custom metadata and a Company logo as a watermark to PDF files. Authenticated and authorized users can upload single or multiple PDF documents. The service also provides an option to include a user's ID number or name as an additional watermark on the bottom-left of each page, indicating who downloaded the file.

When a single PDF is uploaded, the service returns the watermarked file directly. For multiple file uploads, the service packages the watermarked PDFs into a single ZIP file for convenient download. The service is built with security and resilience in mind, enforcing file type and size restrictions and ensuring high availability.

## Features

*   **User Authentication and Authorization:** Secure access using Spring Security, OAuth2, and JWT.
*   **Dynamic Watermarking:** Automatically adds the Valmet logo to each page of the uploaded PDF files.
*   **Customizable User Stamp:** Optionally adds a "Downloaded by [ID number/Name]" watermark.
*   **Single and Multiple File Handling:**
    *   Returns a watermarked PDF for single file uploads.
    *   Returns a ZIP archive containing all watermarked PDFs for multiple file uploads.
*   **File Validation:** Accepts only PDF files and enforces a configurable file size limit.
*   **High Availability:** Deployed as multiple instances with load balancing to ensure service uptime.
*   **Fault Tolerance:** Built-in resilience patterns to handle API failures and transient errors gracefully.

## Technology Stack

*   **Backend:** Java 17+, Spring Boot
*   **Web Server:** Spring Boot Embedded Tomcat
*   **Reverse Proxy & Load Balancer:** Nginx
*   **Security:**
    *   Spring Security 6
    *   OAuth2 with JSON Web Tokens (JWT)
    *   Spring LDAP for user authentication
    *   Spring Vault for secret management
*   **Secret Management:** HashiCorp Vault
*   **PDF Manipulation:** iTextPDF 7.2.4 (`commons`, `io`, `kernel`, `layout`)
*   **Build Tool:** Apache Maven
*   **API Documentation:** Swagger / OpenAPI
*   **Logging:** SLF4J with Logback
*   **Testing:** JUnit
*   **Fault Tolerance:** Resilience4j (Circuit Breaker, Retry, Bulkhead)
*   **Monitoring:** Prometheus and Grafana
*   **Secure Communication:** SSL/TLS with a PKCS#12 keystore

## Architecture

The architecture is designed for scalability, security, and resilience.

1.  **Client:** The user interacts with the service via HTTPS requests.
2.  **Nginx:** Acts as a reverse proxy and load balancer. It terminates the SSL/TLS connection on port 443 and distributes incoming requests across the available Spring Boot application instances using load balancing algorithms. This enhances security and ensures no single instance is overwhelmed.
3.  **Spring Boot Applications:** Multiple instances of the application run on different ports (e.g., 8081, 8082). Each instance is a self-contained web service capable of handling file uploads, processing PDFs, and applying watermarks. This multi-instance setup provides horizontal scalability and high availability.
4.  **HashiCorp Vault:** A dedicated service for managing secrets like passwords and RSA key pairs for JWT signing. It is running on the same machine and integrates with the Spring Boot applications via Spring Vault. RSA keys are rotated monthly as a security best practice.

## Implementation Process

### Application Deployment
The core logic is built using Spring Boot. The application is deployed in multiple instances on the same server, each listening on a different internal port (e.g., 8081, 8082). This strategy allows for effective load balancing and ensures that the failure of one instance does not bring down the entire service.

### Nginx Configuration
Nginx is configured to listen for HTTPS traffic on port 443. It acts as the single entry point for all client requests. Its key responsibilities include:
*   **SSL/TLS Termination:** Manages the SSL handshake and decrypts incoming HTTPS traffic before forwarding it to the backend.
*   **Reverse Proxy:** Forwards requests to the upstream Spring Boot application instances.
*   **Load Balancing:** Distributes the traffic evenly across the instances to optimize resource usage and prevent overloads.

### Security and Authentication Flow
1.  **Access Token Request:** The client initiates the authentication process by sending its credentials (username and password) to the service.
2.  **Authentication:** Nginx forwards the request to a Spring Boot instance. The application uses Spring Security and Spring LDAP to authenticate the user against a directory service.
3.  **JWT Issuance:** Upon successful authentication, the service generates a JWT. The token is signed using a private RSA key securely stored in HashiCorp Vault.
4.  **Authenticated Requests:** The client includes the JWT in the `Authorization` header for all subsequent requests to protected endpoints (e.g., file upload). The service validates the JWT signature using the corresponding public key to authorize the request.

### Fault Tolerance with Resilience4J
*   **Circuit Breaker:** If a downstream service fails, the circuit breaker pattern prevents repeated failing requests. It triggers a fallback method which returns the original file without a watermark, ensuring the user still receives a response.
*   **Retry:** Automatically re-attempts failed operations (like network timeouts) a configured number of times to recover from temporary issues.
*   **Bulkhead:** Limits the number of concurrent requests to the API, preventing resource exhaustion and ensuring the service remains stable under high load.

### SSL/TLS Configuration
Secure communication is enabled via HTTPS. A `keystore.p12` file containing the private key and certificate is included in the project's resources. The `application.properties` file is configured with the keystore type, password, and key alias to enable SSL/TLS within the Spring Boot application.

## API Endpoints

### 1. Authentication

This endpoint is used to authenticate a user and receive an access token and a refresh token.

*   **URL:** `https://watermark-service.valmet.com/api/authenticate`
*   **Method:** `POST`
*   **Request Body:** JSON object with `username` and `password`. Valmet AD credentials are also supported.

    ```json
    {
      "username": "valmet_watermark_generator",
      "password": "Valmet@231"
    }
    ```
*   **Success Response (200 OK):**

    ```json
    {
      "type": "RESULT",
      "message": ["OK"],
      "result": {
        "accessToken": "eyJraWQiOiI3MGIxOGNjZC0xZTM1LTQ3YTItYmYwNy0xZDU0YzI2MGY3ZjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ3YXRlcm1hcmstZ2VuZXJhdG9yIiwic2NvcGUiOiIiLCJpc3MiOiJzZWxmIiwiZXhwIjoxNzMyNzkxNDc3LCJ0b2tlbl90eXBlIjoiQUNDRVNTIiwiaWF0IjoxNzMyNzA1MDc3fQ.lyp4-9q9MLMpa20-TAYUOUP6DrsSufnjAWnUT9SQn9XUaLEXeNjFnxC63IXojYthY0zrPIHZjMv5fE9UpFG0QmZm9kQSyBbFwHTHdySNaf5vwn9STOagn_STOfcCD0a2n7O_Frobyd8nXqaFXhZtTeZGPBabd3h6IQjKBNogX00OUdDxhUe2Szo70J7Z_RZvQKLP1NtyfmAi0yiPLWtKBZhqe_j-e7FNDeUXMxpVbYA9njn6ge9A88jiuJLYM1m2inrGItP5dbMvkgdFl09O9rY61wjaMX0JNkDooMHe1pWAQJBzhIzq9IudAmEFoFuGxf12weROE6_hefNdbYsGKg",
        "refreshToken": "eyJraWQiOiI3MGIxOGNjZC0xZTM1LTQ3YTItYmYwNy0xZDU0YzI2MGY3ZjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ3YXRlcm1hcmstZ2VuZXJhdG9yIiwic2NvcGUiOiIiLCJpc3MiOiJzZWxmIiwiZXhwIjoxNzY0MjQxMDc3LCJ0b2tlbl90eXBlIjoiUkVGUkVTSCIsImlhdCI6MTczMjcwNTA3N30.ef6zHn7PNViCZDqAFCqMLkqZIx6I-IHNeWbFjPFOqoeQX8mGmacOqmTp5ostEFZZaexmo6xw7Zh1xbS81XFLmu61Tr9wc94c2B9QWG_sqMGUEkFFg8FrWZltvytjT3ZyniIfhGbb8z_7bvrLCtSHKvwGqpInbZIwAvO-PlZqUe2yx_JUdV4yA3mP15sGset5Q5m2DWaKuIHFMk91ysQKBLqEGSfWlV6o0v0csljLkUPFAr9XEkCsUiz6JZU0Lfz_T2vovzBEsCaYeCf6PyejNz0xQHZFOrbzq52XcImI3BNyWB0Cl1BvH_xMJ0bw3xp0KqS--XnyKOy-Qd9BgB2pdg"
      },
      "code": "200"
    }
    ```

### 2. Add Watermark

This endpoint processes single or multiple PDF files to add watermarks.

*   **URL:** `https://watermark-service.valmet.com/api/watermark`
*   **Method:** `POST`
*   **Headers:**
    *   `Authorization`: `Bearer <accessToken>`
    *   `Content-Type`: `multipart/form-data`
*   **Form Data:**
    *   `file`: The PDF file(s) to be watermarked.
    *   `personID` (optional): The ID or name to be added as a watermark.
    *   `email` (optional): If `personID` is not provided, the system uses this email to look up the user in Valmet AD and adds their ID as the watermark.
    *   `system` (optional): The name of the source system (e.g., Sovelia, PDM).
*   **Response:**
    *   For a single file upload, the response is the watermarked PDF file (`application/pdf`).
    *   For multiple file uploads, the response is a ZIP file (`application/zip`) containing all the watermarked PDFs.

### 3. Renew Access Token

This endpoint is used to get a new access token using a valid refresh token when the original access token has expired.

*   **URL:** `https://watermark-service.valmet.com/api/renewToken`
*   **Method:** `POST`
*   **Request Body:** JSON object containing the `refreshToken`.

    ```json
    {
      "refreshToken": "eyJraWQiOiI3MGIxOGNjZC0xZTM1LTQ3YTItYmYwNy0xZDU0YzI2MGY3ZjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ3YXRlcm1hcmstZ2VuZXJhdG9yIiwic2NvcGUiOiIiLCJpc3MiOiJzZWxmIiwiZXhwIjoxNzY0MjQxNDQ2LCJ0b2tlbl90eXBlIjoiUkVGUkVTSCIsImlhdCI6MTczMjcwNTQ0Nn0.bAumWnHcD5i6qrvkrsuSIJ25mSdYM3Diw45PohfoOVDtWnebcVzmmOcDMIi12viVHNt6YUuRAA6wbzs4wzKpxaHXV4Qr2IJsK2EOREoeOq9uiJZP4uRdOVCNaTZ45S2a1XHkRS60EOXaXWBgppDkJrqpY6sNxkcvAAlPyloF7POGjZIJIyN0S8FjNvElm3ljMMFrJ6_iVBz_wBsoyc7zTOMpqk5zp_6WefN0PSS50FPGhUVW3C0YzFZf_1FRCGOH4ydXQjIh1oPSRif1hYLH1cEZRI3vXzuuS-CZyxVbafg1L9KIM5zMeEvtDEHJvocPRaraH_ViuaUNeaHD5CkBSg"
    }
    ```
*   **Success Response (200 OK):** The response contains a new `accessToken` and the same `refreshToken`.

    ```json
    {
      "type": "RESULT",
      "message": ["OK"],
      "result": {
        "accessToken": "eyJraWQiOiI3MGIxOGNjZC0xZTM1LTQ3YTItYmYwNy0xZDU0YzI2MGY3ZjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ3YXRlcm1hcmstZ2VuZXJhdG9yIiwic2NvcGUiOiIiLCJpc3MiOiJzZWxmIiwiZXhwIjoxNzMyNzkxNDc3LCJ0b2tlbl90eXBlIjoiQUNDRVNTIiwiaWF0IjoxNzMyNzA1MDc3fQ.lyp4-9q9MLMpa20-TAYUOUP6DrsSufnjAWnUT9SQn9XUaLEXeNjFnxC63IXojYthY0zrPIHZjMv5fE9UpFG0QmZm9kQSyBbFwHTHdySNaf5vwn9STOagn_STOfcCD0a2n7O_Frobyd8nXqaFXhZtTeZGPBabd3h6IQjKBNogX00OUdDxhUe2Szo70J7Z_RZvQKLP1NtyfmAi0yiPLWtKBZhqe_j-e7FNDeUXMxpVbYA9njn6ge9A88jiuJLYM1m2inrGItP5dbMvkgdFl09O9rY61wjaMX0JNkDooMHe1pWAQJBzhIzq9IudAmEFoFuGxf12weROE6_hefNdbYsGKg",
        "refreshToken": "eyJraWQiOiI3MGIxOGNjZC0xZTM1LTQ3YTItYmYwNy0xZDU0YzI2MGY3ZjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ3YXRlcm1hcmstZ2VuZXJhdG9yIiwic2NvcGUiOiIiLCJpc3MiOiJzZWxmIiwiZXhwIjoxNzY0MjQxMDc3LCJ0b2tlbl90eXBlIjoiUkVGUkVTSCIsImlhdCI6MTczMjcwNTA3N30.ef6zHn7PNViCZDqAFCqMLkqZIx6I-IHNeWbFjPFOqoeQX8mGmacOqmTp5ostEFZZaexmo6xw7Zh1xbS81XFLmu61Tr9wc94c2B9QWG_sqMGUEkFFg8FrWZltvytjT3ZyniIfhGbb8z_7bvrLCtSHKvwGqpInbZIwAvO-PlZqUe2yx_JUdV4yA3mP15sGset5Q5m2DWaKuIHFMk91ysQKBLqEGSfWlV6o0v0csljLkUPFAr9XEkCsUiz6JZU0Lfz_T2vovzBEsCaYeCf6PyejNz0xQHZFOrbzq52XcImI3BNyWB0Cl1BvH_xMJ0bw3xp0KqS--XnyKOy-Qd9BgB2pdg"
      },
      "code": "200"
    }
    ```
