package com.restaurant.qrorder;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_restaurant_qr_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("init-db.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    protected String loginAsAdmin() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@restaurant.com");
        loginRequest.put("password", "admin123");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("data.token");
    }

    protected String loginAsStaff() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "staff@restaurant.com");
        loginRequest.put("password", "admin123");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("data.token");
    }

    protected String registerAndLoginAsCustomer(String email, String password) {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", email);
        registerRequest.put("password", password);
        registerRequest.put("fullName", "Test Customer");
        registerRequest.put("phone", "0123456789");

        // Try to register - accept both 201 (created) and 409 (already exists)
        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(equalTo(201), equalTo(409)));

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("data.token");
    }
}
