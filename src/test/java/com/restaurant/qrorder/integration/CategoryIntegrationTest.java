package com.restaurant.qrorder.integration;

import com.restaurant.qrorder.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Category API Tests")
class CategoryIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String staffToken;
    private Long createdCategoryId;

    @BeforeEach
    void setUpAuth() {
        adminToken = loginAsAdmin();
        staffToken = loginAsStaff();
    }

    @Test
    @DisplayName("Should get all categories without authentication")
    @Order(1)
    void testGetAllCategoriesNoAuth() {
        given()
                .when()
                .get("/categories")
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThanOrEqualTo(5)))
                .body("data[0].name", notNullValue());
    }

    @Test
    @DisplayName("Should get category by ID successfully")
    @Order(2)
    void testGetCategoryById() {
        given()
                .when()
                .get("/categories/1")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(1))
                .body("data.name", notNullValue())
                .body("data.description", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for non-existent category")
    @Order(3)
    void testGetCategoryByIdNotFound() {
        given()
                .when()
                .get("/categories/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Admin should create category successfully")
    @Order(4)
    void testCreateCategoryAsAdmin() {
        Map<String, String> categoryRequest = new HashMap<>();
        categoryRequest.put("name", "Test Category");
        categoryRequest.put("description", "Test Description");

        createdCategoryId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(categoryRequest)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .body("data.name", equalTo("Test Category"))
                .body("data.description", equalTo("Test Description"))
                .extract()
                .jsonPath()
                .getLong("data.id");
    }

    @Test
    @DisplayName("Should fail to create category with duplicate name")
    @Order(5)
    void testCreateCategoryDuplicateName() {
        Map<String, String> categoryRequest = new HashMap<>();
        categoryRequest.put("name", "Appetizers");
        categoryRequest.put("description", "Duplicate category");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(categoryRequest)
                .when()
                .post("/categories")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("Staff should NOT be able to create category")
    @Order(6)
    void testCreateCategoryAsStaff() {
        Map<String, String> categoryRequest = new HashMap<>();
        categoryRequest.put("name", "Staff Category");
        categoryRequest.put("description", "Should fail");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(categoryRequest)
                .when()
                .post("/categories")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Should fail to create category without authentication")
    @Order(7)
    void testCreateCategoryNoAuth() {
        Map<String, String> categoryRequest = new HashMap<>();
        categoryRequest.put("name", "Unauthorized Category");
        categoryRequest.put("description", "Should fail");

        given()
                .contentType(ContentType.JSON)
                .body(categoryRequest)
                .when()
                .post("/categories")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Should fail to create category with empty name")
    @Order(8)
    void testCreateCategoryEmptyName() {
        Map<String, String> categoryRequest = new HashMap<>();
        categoryRequest.put("name", "");
        categoryRequest.put("description", "Invalid category");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(categoryRequest)
                .when()
                .post("/categories")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Admin should update category successfully")
    @Order(9)
    void testUpdateCategoryAsAdmin() {
        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Category");
        updateRequest.put("description", "Updated Description");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/categories/1")
                .then()
                .statusCode(200)
                .body("data.name", equalTo("Updated Category"))
                .body("data.description", equalTo("Updated Description"));
    }

    @Test
    @DisplayName("Should fail to update non-existent category")
    @Order(10)
    void testUpdateCategoryNotFound() {
        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Category");
        updateRequest.put("description", "Updated Description");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/categories/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Staff should NOT be able to update category")
    @Order(11)
    void testUpdateCategoryAsStaff() {
        Map<String, String> updateRequest = new HashMap<>();
        updateRequest.put("name", "Staff Updated");
        updateRequest.put("description", "Should fail");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/categories/1")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Admin should delete category successfully")
    @Order(12)
    void testDeleteCategoryAsAdmin() {
        // Create a category to delete
        Map<String, String> categoryRequest = new HashMap<>();
        categoryRequest.put("name", "To Delete Category");
        categoryRequest.put("description", "Will be deleted");

        Long categoryId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(categoryRequest)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        // Delete the category
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/categories/" + categoryId)
                .then()
                .statusCode(200)
                .body("message", containsString("deleted successfully"));

        // Verify it's deleted
        given()
                .when()
                .get("/categories/" + categoryId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should fail to delete non-existent category")
    @Order(13)
    void testDeleteCategoryNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/categories/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Staff should NOT be able to delete category")
    @Order(14)
    void testDeleteCategoryAsStaff() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .delete("/categories/1")
                .then()
                .statusCode(500);
    }
}
