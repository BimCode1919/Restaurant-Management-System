package com.restaurant.qrorder.integration;

import com.restaurant.qrorder.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Authentication API Tests")
class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should register new user successfully")
    @org.junit.jupiter.api.Order(1)
    void testRegisterSuccess() {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "newuser@test.com");
        registerRequest.put("password", "password123");
        registerRequest.put("fullName", "New User");
        registerRequest.put("phone", "0987654321");

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .body("statusCode", equalTo(201))
                .body("message", equalTo("User registered successfully"))
                .body("data.token", notNullValue())
                .body("data.user.email", equalTo("newuser@test.com"))
                .body("data.user.fullName", equalTo("New User"))
                .body("data.user.role", equalTo("CUSTOMER"));
    }

    @Test
    @DisplayName("Should fail to register with duplicate email")
    @org.junit.jupiter.api.Order(2)
    void testRegisterDuplicateEmail() {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "admin@restaurant.com");
        registerRequest.put("password", "password123");
        registerRequest.put("fullName", "Duplicate User");
        registerRequest.put("phone", "0987654321");

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("Should fail to register with invalid email format")
    @org.junit.jupiter.api.Order(3)
    void testRegisterInvalidEmail() {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "invalid-email");
        registerRequest.put("password", "password123");
        registerRequest.put("fullName", "Test User");
        registerRequest.put("phone", "0987654321");

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail to register with empty password")
    @org.junit.jupiter.api.Order(4)
    void testRegisterEmptyPassword() {
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "test@test.com");
        registerRequest.put("password", "");
        registerRequest.put("fullName", "Test User");
        registerRequest.put("phone", "0987654321");

        given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should login successfully with correct credentials")
    @org.junit.jupiter.api.Order(5)
    void testLoginSuccess() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@restaurant.com");
        loginRequest.put("password", "admin123");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("statusCode", equalTo(200))
                .body("message", equalTo("Login successful"))
                .body("data.token", notNullValue())
                .body("data.user.email", equalTo("admin@restaurant.com"))
                .body("data.user.role", equalTo("ADMIN"));
    }

    @Test
    @DisplayName("Should fail to login with wrong password")
    @org.junit.jupiter.api.Order(6)
    void testLoginWrongPassword() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@restaurant.com");
        loginRequest.put("password", "wrongpassword");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should fail to login with non-existent email")
    @org.junit.jupiter.api.Order(7)
    void testLoginNonExistentEmail() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "nonexistent@test.com");
        loginRequest.put("password", "password123");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should fail to login with invalid email format")
    @org.junit.jupiter.api.Order(8)
    void testLoginInvalidEmailFormat() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "invalid-email");
        loginRequest.put("password", "password123");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    @org.junit.jupiter.api.Order(9)
    void testRefreshToken() {
        String token = loginAsAdmin();

        given()
                .header("Refresh-Token", token)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .body("data.token", notNullValue());
    }

    @Test
    @DisplayName("Should fail to refresh token without authorization")
    @org.junit.jupiter.api.Order(10)
    void testRefreshTokenUnauthorized() {
        given()
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(500); // Missing header returns 500
    }
}
