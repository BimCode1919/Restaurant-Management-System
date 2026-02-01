package com.restaurant.qrorder.integration;

import com.restaurant.qrorder.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Reservation API Tests")
class ReservationIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String staffToken;
    private static Long createdReservationId;

    @BeforeEach
    void setUpAuth() {
        adminToken = loginAsAdmin();
        staffToken = loginAsStaff();
    }

    @Test
    @DisplayName("Should create reservation successfully")
    @Order(1)
    void testCreateReservationSuccess() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(2);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "John Doe");
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("customerEmail", "john@test.com");
        reservationRequest.put("partySize", 4);
        reservationRequest.put("reservationTime", futureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("note", "Window seat preferred");
        reservationRequest.put("tableIds", Arrays.asList(1L, 2L));

        createdReservationId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .body("data.customerName", equalTo("John Doe"))
                .body("data.customerPhone", equalTo("0123456789"))
                .body("data.partySize", equalTo(4))
                .body("data.status", equalTo("PENDING"))
                .extract()
                .jsonPath()
                .getLong("data.id");
    }

    @Test
    @DisplayName("Should fail to create reservation with past time")
    @Order(2)
    void testCreateReservationPastTime() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(2);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "John Doe");
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("partySize", 4);
        reservationRequest.put("reservationTime", pastTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("tableIds", Arrays.asList(1L));

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail to create reservation without customer name")
    @Order(3)
    void testCreateReservationWithoutName() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(2);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("partySize", 4);
        reservationRequest.put("reservationTime", futureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should fail to create reservation with invalid party size")
    @Order(4)
    void testCreateReservationInvalidPartySize() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(2);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "John Doe");
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("partySize", 0);
        reservationRequest.put("reservationTime", futureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should get all reservations")
    @Order(5)
    void testGetAllReservations() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/reservations")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data[0].id", notNullValue());
    }

    @Test
    @DisplayName("Should get reservation by ID")
    @Order(6)
    void testGetReservationById() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/reservations/" + createdReservationId)
                .then()
                .statusCode(200)
                .body("data.id", equalTo(createdReservationId.intValue()))
                .body("data.customerName", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for non-existent reservation")
    @Order(7)
    void testGetReservationByIdNotFound() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/reservations/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should filter reservations by status")
    @Order(8)
    void testGetReservationsByStatus() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "PENDING")
                .when()
                .get("/reservations")
                .then()
                .statusCode(200)
                .body("data", not(empty()));
    }

    @Test
    @DisplayName("Should filter reservations by date")
    @Order(9)
    void testGetReservationsByDate() {
        LocalDateTime today = LocalDateTime.now();
        
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("date", today.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .when()
                .get("/reservations")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should update reservation successfully")
    @Order(10)
    void testUpdateReservation() {
        LocalDateTime newTime = LocalDateTime.now().plusHours(3);
        
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("customerName", "John Updated");
        updateRequest.put("customerPhone", "0987654321");
        updateRequest.put("partySize", 6);
        updateRequest.put("reservationTime", newTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        updateRequest.put("note", "Updated note");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/reservations/" + createdReservationId)
                .then()
                .statusCode(200)
                .body("data.customerName", equalTo("John Updated"))
                .body("data.partySize", equalTo(6));
    }

    @Test
    @DisplayName("Should fail to update non-existent reservation")
    @Order(11)
    void testUpdateReservationNotFound() {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("customerName", "John Updated");
        updateRequest.put("partySize", 6);

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/reservations/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should confirm reservation")
    @Order(12)
    void testConfirmReservation() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + createdReservationId + "/confirm")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CONFIRMED"));
    }

    @Test
    @DisplayName("Should mark reservation as seated")
    @Order(13)
    void testMarkReservationAsSeated() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + createdReservationId + "/seated")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("SEATED"))
                .body("data.arrivalTime", notNullValue());
    }

    @Test
    @DisplayName("Should cancel reservation")
    @Order(14)
    void testCancelReservation() {
        // Create a new reservation to cancel
        LocalDateTime futureTime = LocalDateTime.now().plusHours(4);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "To Cancel");
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("partySize", 2);
        reservationRequest.put("reservationTime", futureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("tableIds", Arrays.asList(3L));

        Long reservationId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        Map<String, String> cancelRequest = new HashMap<>();
        cancelRequest.put("reason", "Customer changed plans");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(cancelRequest)
                .when()
                .patch("/reservations/" + reservationId + "/cancel")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CANCELLED"))
                .body("data.cancellationReason", equalTo("Customer changed plans"));
    }

    @Test
    @DisplayName("Should mark reservation as no-show")
    @Order(15)
    void testMarkReservationAsNoShow() {
        // Create a reservation with past time
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "No Show Customer");
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("partySize", 2);
        reservationRequest.put("reservationTime", pastTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("tableIds", Arrays.asList(4L));

        Long reservationId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        // Confirm first
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + reservationId + "/confirm")
                .then()
                .statusCode(200);

        // Mark as no-show
        Map<String, String> noShowRequest = new HashMap<>();
        noShowRequest.put("reason", "Customer did not arrive");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(noShowRequest)
                .when()
                .patch("/reservations/" + reservationId + "/no-show")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("NO_SHOW"));
    }

    @Test
    @DisplayName("Should delete reservation")
    @Order(16)
    void testDeleteReservation() {
        // Create a reservation to delete
        LocalDateTime futureTime = LocalDateTime.now().plusHours(5);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "To Delete");
        reservationRequest.put("customerPhone", "0123456789");
        reservationRequest.put("partySize", 2);
        reservationRequest.put("reservationTime", futureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("tableIds", Arrays.asList(5L));

        Long reservationId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        // Delete
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/reservations/" + reservationId)
                .then()
                .statusCode(200);

        // Verify deleted
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/reservations/" + reservationId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should fail to delete non-existent reservation")
    @Order(17)
    void testDeleteReservationNotFound() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/reservations/9999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Staff should NOT be able to delete reservation")
    @Order(18)
    void testDeleteReservationAsStaff() {
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .delete("/reservations/" + createdReservationId)
                .then()
                .statusCode(403);
    }
}
