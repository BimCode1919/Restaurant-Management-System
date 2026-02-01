package com.restaurant.qrorder.integration;

import com.restaurant.qrorder.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Table API Tests")
class TableIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUpAuth() {
        adminToken = loginAsAdmin();
        staffToken = loginAsStaff();
        customerToken = registerAndLoginAsCustomer("customer@test.com", "password123");
    }

    @Test
    @DisplayName("Admin should get all tables")
    @Order(1)
    void testGetAllTablesAsAdmin() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/tables")
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThanOrEqualTo(10)))
                .body("data[0].tableNumber", notNullValue())
                .body("data[0].qrCode", notNullValue())
                .body("data[0].capacity", notNullValue());
    }

    @Test
    @DisplayName("Staff should get all tables")
    @Order(2)
    void testGetAllTablesAsStaff() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables")
                .then()
                .statusCode(200)
                .body("data", not(empty()));
    }

    @Test
    @DisplayName("Customer should NOT get all tables")
    @Order(3)
    void testGetAllTablesAsCustomer() {
        given()
                .header("Authorization", "Bearer " + customerToken)
                .when()
                .get("/tables")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Should filter tables by status")
    @Order(4)
    void testGetTablesByStatus() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "AVAILABLE")
                .when()
                .get("/tables")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data[0].status", equalTo("AVAILABLE"));
    }

    @Test
    @DisplayName("Should filter tables by capacity")
    @Order(5)
    void testGetTablesByCapacity() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("minCapacity", 4)
                .when()
                .get("/tables")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data[0].capacity", greaterThanOrEqualTo(4));
    }

    @Test
    @DisplayName("Should get table by ID")
    @Order(6)
    void testGetTableById() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables/1")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(1))
                .body("data.tableNumber", notNullValue())
                .body("data.status", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for non-existent table")
    @Order(7)
    void testGetTableByIdNotFound() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Admin should create table successfully")
    @Order(8)
    void testCreateTableAsAdmin() {
        Map<String, Object> tableRequest = new HashMap<>();
        tableRequest.put("tableNumber", "99");
        tableRequest.put("capacity", 4);
        tableRequest.put("location", "Test Area");
        tableRequest.put("status", "AVAILABLE");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(tableRequest)
                .when()
                .post("/tables")
                .then()
                .statusCode(201)
                .body("data.tableNumber", equalTo("99"))
                .body("data.capacity", equalTo(4))
                .body("data.qrCode", notNullValue())
                .body("data.status", equalTo("AVAILABLE"));
    }

    @Test
    @DisplayName("Should fail to create table with duplicate number")
    @Order(9)
    void testCreateTableDuplicateNumber() {
        Map<String, Object> tableRequest = new HashMap<>();
        tableRequest.put("tableNumber", "01");
        tableRequest.put("capacity", 2);
        tableRequest.put("location", "Test");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(tableRequest)
                .when()
                .post("/tables")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail to create table with invalid capacity")
    @Order(10)
    void testCreateTableInvalidCapacity() {
        Map<String, Object> tableRequest = new HashMap<>();
        tableRequest.put("tableNumber", "100");
        tableRequest.put("capacity", 0);
        tableRequest.put("location", "Test");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(tableRequest)
                .when()
                .post("/tables")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Staff should NOT be able to create table")
    @Order(11)
    void testCreateTableAsStaff() {
        Map<String, Object> tableRequest = new HashMap<>();
        tableRequest.put("tableNumber", "101");
        tableRequest.put("capacity", 4);

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(tableRequest)
                .when()
                .post("/tables")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Admin should update table successfully")
    @Order(12)
    void testUpdateTableAsAdmin() {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("tableNumber", "01-Updated");
        updateRequest.put("capacity", 3);
        updateRequest.put("location", "Updated Location");
        updateRequest.put("status", "RESERVED");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/tables/1")
                .then()
                .statusCode(200)
                .body("data.tableNumber", equalTo("01-Updated"))
                .body("data.capacity", equalTo(3))
                .body("data.location", equalTo("Updated Location"));
    }

    @Test
    @DisplayName("Should update table status")
    @Order(13)
    void testUpdateTableStatus() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "OCCUPIED")
                .when()
                .patch("/tables/2/status")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("OCCUPIED"));

        // Set back to available
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "AVAILABLE")
                .when()
                .patch("/tables/2/status")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("AVAILABLE"));
    }

    @Test
    @DisplayName("Should fail to update status with invalid value")
    @Order(14)
    void testUpdateTableStatusInvalid() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "INVALID_STATUS")
                .when()
                .patch("/tables/2/status")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Staff should update table status")
    @Order(15)
    void testUpdateTableStatusAsStaff() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "CLEANING")
                .when()
                .patch("/tables/3/status")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CLEANING"));
    }

    @Test
    @DisplayName("Customer should NOT be able to update table status")
    @Order(16)
    void testUpdateTableStatusAsCustomer() {
        given()
                .header("Authorization", "Bearer " + customerToken)
                .queryParam("status", "AVAILABLE")
                .when()
                .patch("/tables/3/status")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Should generate new QR code for table")
    @Order(17)
    void testGenerateNewQRCode() {
        String oldQrCode = given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables/4")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("data.qrCode");

        String newQrCode = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .patch("/tables/4/regenerate-qr")
                .then()
                .statusCode(200)
                .body("data.qrCode", notNullValue())
                .extract()
                .jsonPath()
                .getString("data.qrCode");

        // QR codes should be different
        assert !oldQrCode.equals(newQrCode);
    }

    @Test
    @DisplayName("Admin should delete table successfully")
    @Order(18)
    void testDeleteTableAsAdmin() {
        // Create a table to delete
        Map<String, Object> tableRequest = new HashMap<>();
        tableRequest.put("tableNumber", "TO-DELETE");
        tableRequest.put("capacity", 2);

        Long tableId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(tableRequest)
                .when()
                .post("/tables")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        // Delete
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/tables/" + tableId)
                .then()
                .statusCode(200);

        // Verify deleted
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables/" + tableId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should fail to delete non-existent table")
    @Order(19)
    void testDeleteTableNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/tables/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Staff should NOT be able to delete table")
    @Order(20)
    void testDeleteTableAsStaff() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .delete("/tables/5")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Should get available tables in time range")
    @Order(21)
    void testGetAvailableTablesInTimeRange() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("startTime", "2026-02-01T18:00:00")
                .queryParam("endTime", "2026-02-01T20:00:00")
                .queryParam("partySize", 4)
                .when()
                .get("/tables/available")
                .then()
                .statusCode(200)
                .body("data", notNullValue());
    }
}
