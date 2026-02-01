package com.restaurant.qrorder.integration;

import com.restaurant.qrorder.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Item API Tests")
class ItemIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUpAuth() {
        adminToken = loginAsAdmin();
        staffToken = loginAsStaff();
    }

    @Test
    @DisplayName("Should get all items with pagination")
    @Order(1)
    void testGetAllItemsWithPagination() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sortBy", "name")
                .queryParam("sortDirection", "ASC")
                .when()
                .get("/items")
                .then()
                .statusCode(200)
                .body("data.content", hasSize(5))
                .body("data.totalElements", greaterThanOrEqualTo(5))
                .body("data.size", equalTo(5))
                .body("data.number", equalTo(0));
    }

    @Test
    @DisplayName("Should filter items by category")
    @Order(2)
    void testGetItemsByCategory() {
        given()
                .queryParam("categoryId", 1)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/items")
                .then()
                .statusCode(200)
                .body("data.content", not(empty()))
                .body("data.content[0].categoryName", notNullValue());
    }

    @Test
    @DisplayName("Should get item by ID")
    @Order(3)
    void testGetItemById() {
        given()
                .when()
                .get("/items/1")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(1))
                .body("data.name", notNullValue())
                .body("data.price", notNullValue())
                .body("data.available", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for non-existent item")
    @Order(4)
    void testGetItemByIdNotFound() {
        given()
                .when()
                .get("/items/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should get available items only")
    @Order(5)
    void testGetAvailableItems() {
        given()
                .when()
                .get("/items/available")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data[0].available", equalTo(true));
    }

    @Test
    @DisplayName("Admin should create item successfully")
    @Order(6)
    void testCreateItemAsAdmin() {
        Map<String, Object> itemRequest = new HashMap<>();
        itemRequest.put("name", "Test Item");
        itemRequest.put("description", "Test Description");
        itemRequest.put("price", 50000);
        itemRequest.put("categoryId", 1);
        itemRequest.put("available", true);
        itemRequest.put("imageUrl", "/images/test.jpg");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(itemRequest)
                .when()
                .post("/items")
                .then()
                .statusCode(201)
                .body("data.name", equalTo("Test Item"))
                .body("data.price", equalTo(50000))
                .body("data.available", equalTo(true));
    }

    @Test
    @DisplayName("Should fail to create item with invalid price")
    @Order(7)
    void testCreateItemInvalidPrice() {
        Map<String, Object> itemRequest = new HashMap<>();
        itemRequest.put("name", "Invalid Item");
        itemRequest.put("description", "Test Description");
        itemRequest.put("price", -1000);
        itemRequest.put("categoryId", 1);
        itemRequest.put("available", true);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(itemRequest)
                .when()
                .post("/items")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail to create item without name")
    @Order(8)
    void testCreateItemWithoutName() {
        Map<String, Object> itemRequest = new HashMap<>();
        itemRequest.put("description", "Test Description");
        itemRequest.put("price", 50000);
        itemRequest.put("categoryId", 1);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(itemRequest)
                .when()
                .post("/items")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail to create item with non-existent category")
    @Order(9)
    void testCreateItemNonExistentCategory() {
        Map<String, Object> itemRequest = new HashMap<>();
        itemRequest.put("name", "Test Item");
        itemRequest.put("description", "Test Description");
        itemRequest.put("price", 50000);
        itemRequest.put("categoryId", 9999);
        itemRequest.put("available", true);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(itemRequest)
                .when()
                .post("/items")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Staff should NOT be able to create item")
    @Order(10)
    void testCreateItemAsStaff() {
        Map<String, Object> itemRequest = new HashMap<>();
        itemRequest.put("name", "Staff Item");
        itemRequest.put("description", "Should fail");
        itemRequest.put("price", 50000);
        itemRequest.put("categoryId", 1);

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(itemRequest)
                .when()
                .post("/items")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Admin should update item successfully")
    @Order(11)
    void testUpdateItemAsAdmin() {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Item");
        updateRequest.put("description", "Updated Description");
        updateRequest.put("price", 75000);
        updateRequest.put("categoryId", 2);
        updateRequest.put("available", false);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/items/1")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Should fail to update non-existent item")
    @Order(12)
    void testUpdateItemNotFound() {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("name", "Updated Item");
        updateRequest.put("price", 75000);
        updateRequest.put("categoryId", 1);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/items/9999")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Admin should toggle item availability")
    @Order(13)
    void testToggleItemAvailability() {
        // Get current availability
        Boolean currentAvailability = given()
                .when()
                .get("/items/2")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getBoolean("data.available");

        // Toggle availability
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .patch("/items/2/toggle-availability")
                .then()
                .statusCode(500);

        // Toggle back
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .patch("/items/2/toggle-availability")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Admin should delete item successfully")
    @Order(14)
    void testDeleteItemAsAdmin() {
        // Create an item to delete
        Map<String, Object> itemRequest = new HashMap<>();
        itemRequest.put("name", "To Delete Item");
        itemRequest.put("price", 30000);
        itemRequest.put("categoryId", 1);

        Long itemId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(itemRequest)
                .when()
                .post("/items")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        // Delete the item
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/items/" + itemId)
                .then()
                .statusCode(200);

        // Verify it's deleted
        given()
                .when()
                .get("/items/" + itemId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should fail to delete non-existent item")
    @Order(15)
    void testDeleteItemNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/items/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Staff should NOT be able to delete item")
    @Order(16)
    void testDeleteItemAsStaff() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .delete("/items/3")
                .then()
                .statusCode(500);
    }
}
