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
@DisplayName("Complete Restaurant Flow Tests - End to End")
class RestaurantCompleteFlowTest extends BaseIntegrationTest {

    private static String adminToken;
    private static String staffToken;
    private static Long reservationId;
    private static Long billId;
    private static Long orderId;
    private static Long tableId = 1L;

    @BeforeAll
    static void setUpAll() {
        // This will be called once for the whole test class
    }

    @BeforeEach
    void setUp() {
        adminToken = loginAsAdmin();
        staffToken = loginAsStaff();
    }

    @Test
    @DisplayName("Flow 1: Complete Reservation Flow - Book -> Confirm -> Seat -> Order -> Pay")
    @Order(1)
    void testCompleteReservationFlow() {
        // Step 1: Customer calls to make a reservation
        LocalDateTime reservationTime = LocalDateTime.now().plusHours(2);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "Alice Johnson");
        reservationRequest.put("customerPhone", "0901234567");
        reservationRequest.put("customerEmail", "alice@test.com");
        reservationRequest.put("partySize", 4);
        reservationRequest.put("reservationTime", reservationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("note", "Birthday celebration");
        reservationRequest.put("tableIds", Arrays.asList(5L, 6L)); // VIP tables
        reservationRequest.put("depositRequired", false);

        reservationId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .body("data.status", equalTo("PENDING"))
                .body("data.customerName", equalTo("Alice Johnson"))
                .extract()
                .jsonPath()
                .getLong("data.id");

        System.out.println("✓ Step 1: Reservation created with ID: " + reservationId);

        // Step 2: Staff confirms the reservation
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + reservationId + "/confirm")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CONFIRMED"));

        System.out.println("✓ Step 2: Reservation confirmed");

        // Step 3: Customer arrives and staff marks them as seated
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + reservationId + "/seated")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("SEATED"))
                .body("data.arrivalTime", notNullValue());

        System.out.println("✓ Step 3: Customer seated");

        // Step 4: Get the bill created for the reservation
        billId = given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/reservations/" + reservationId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("data.bill.id");

        System.out.println("✓ Step 4: Bill created with ID: " + billId);

        // Step 5: Customer places order
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("billId", billId);
        orderRequest.put("orderType", "AT_TABLE");
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("itemId", 4); // Fried Rice with Chicken
        item1.put("quantity", 2);
        item1.put("note", "No onions");
        items.add(item1);
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("itemId", 8); // Vietnamese Coffee
        item2.put("quantity", 4);
        items.add(item2);

        Map<String, Object> item3 = new HashMap<>();
        item3.put("itemId", 12); // Tiramisu
        item3.put("quantity", 1);
        items.add(item3);

        orderRequest.put("items", items);

        orderId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(orderRequest)
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .body("data.orderDetails", hasSize(3))
                .extract()
                .jsonPath()
                .getLong("data.id");

        System.out.println("✓ Step 5: Order placed with ID: " + orderId);

        // Step 6: Check bill total
        Map<String, Object> billData = given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/bills/" + billId)
                .then()
                .statusCode(200)
                .body("data.totalPrice", greaterThan(0f))
                .body("data.status", equalTo("OPEN"))
                .extract()
                .jsonPath()
                .getMap("data");

        System.out.println("✓ Step 6: Bill total: " + billData.get("totalPrice"));

        // Step 7: Apply discount if applicable
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("discountCode", "WELCOME10")
                .when()
                .patch("/bills/" + billId + "/apply-discount")
                .then()
                .statusCode(200)
                .body("data.discountAmount", greaterThan(0f))
                .body("data.finalPrice", lessThan((Float) billData.get("totalPrice")));

        System.out.println("✓ Step 7: Discount applied");

        // Step 8: Process payment
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("billId", billId);
        paymentRequest.put("paymentMethod", "CASH");
        paymentRequest.put("amount", billData.get("finalPrice"));

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(paymentRequest)
                .when()
                .post("/payments")
                .then()
                .statusCode(201)
                .body("data.status", equalTo("COMPLETED"))
                .body("data.method", equalTo("CASH"));

        System.out.println("✓ Step 8: Payment completed");

        // Step 9: Close the bill
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/bills/" + billId + "/close")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CLOSED"))
                .body("data.closedAt", notNullValue());

        System.out.println("✓ Step 9: Bill closed");

        // Step 10: Mark reservation as completed
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + reservationId + "/complete")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("COMPLETED"));

        System.out.println("✓ Step 10: Reservation completed");
        System.out.println("✅ Complete reservation flow finished successfully!");
    }

    @Test
    @DisplayName("Flow 2: Walk-in Customer Flow - Assign Table -> Order -> Pay")
    @Order(2)
    void testWalkInCustomerFlow() {
        // Step 1: Get available table
        List<Map<String, Object>> availableTables = given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "AVAILABLE")
                .when()
                .get("/tables")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .extract()
                .jsonPath()
                .getList("data");

        Long walkInTableId = ((Number) availableTables.get(0).get("id")).longValue();
        System.out.println("✓ Step 1: Available table found: " + walkInTableId);

        // Step 2: Create bill for walk-in customer
        Map<String, Object> billRequest = new HashMap<>();
        billRequest.put("tableIds", Arrays.asList(walkInTableId));
        billRequest.put("partySize", 2);

        Long walkInBillId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(billRequest)
                .when()
                .post("/bills")
                .then()
                .statusCode(201)
                .body("data.status", equalTo("OPEN"))
                .extract()
                .jsonPath()
                .getLong("data.id");

        System.out.println("✓ Step 2: Bill created for walk-in customer: " + walkInBillId);

        // Step 3: Update table status to OCCUPIED
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "OCCUPIED")
                .when()
                .patch("/tables/" + walkInTableId + "/status")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("OCCUPIED"));

        System.out.println("✓ Step 3: Table marked as occupied");

        // Step 4: Place initial order
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("billId", walkInBillId);
        orderRequest.put("orderType", "AT_TABLE");
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("itemId", 1); // Spring Rolls
        item1.put("quantity", 1);
        items.add(item1);
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("itemId", 10); // Soft Drink
        item2.put("quantity", 2);
        items.add(item2);

        orderRequest.put("items", items);

        Long walkInOrderId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(orderRequest)
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        System.out.println("✓ Step 4: Initial order placed: " + walkInOrderId);

        // Step 5: Place additional order (customer orders more)
        Map<String, Object> additionalOrder = new HashMap<>();
        additionalOrder.put("billId", walkInBillId);
        additionalOrder.put("orderType", "AT_TABLE");
        
        List<Map<String, Object>> moreItems = new ArrayList<>();
        Map<String, Object> item3 = new HashMap<>();
        item3.put("itemId", 5); // Grilled Beef Steak
        item3.put("quantity", 2);
        moreItems.add(item3);

        additionalOrder.put("items", moreItems);

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(additionalOrder)
                .when()
                .post("/orders")
                .then()
                .statusCode(201);

        System.out.println("✓ Step 5: Additional order placed");

        // Step 6: Get final bill
        Map<String, Object> finalBill = given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/bills/" + walkInBillId)
                .then()
                .statusCode(200)
                .body("data.orders", hasSize(2)) // 2 orders
                .extract()
                .jsonPath()
                .getMap("data");

        System.out.println("✓ Step 6: Final bill amount: " + finalBill.get("totalPrice"));

        // Step 7: Process MoMo payment
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("billId", walkInBillId);
        paymentRequest.put("paymentMethod", "MOMO");
        paymentRequest.put("amount", finalBill.get("totalPrice"));

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(paymentRequest)
                .when()
                .post("/payments")
                .then()
                .statusCode(201)
                .body("data.method", equalTo("MOMO"));

        System.out.println("✓ Step 7: MoMo payment initiated");

        // Step 8: Close bill
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/bills/" + walkInBillId + "/close")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CLOSED"));

        System.out.println("✓ Step 8: Bill closed");

        // Step 9: Set table back to available
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("status", "AVAILABLE")
                .when()
                .patch("/tables/" + walkInTableId + "/status")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("AVAILABLE"));

        System.out.println("✓ Step 9: Table marked as available");
        System.out.println("✅ Walk-in customer flow finished successfully!");
    }

    @Test
    @DisplayName("Flow 3: Reservation Cancellation Flow")
    @Order(3)
    void testReservationCancellationFlow() {
        // Step 1: Create reservation
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "Bob Smith");
        reservationRequest.put("customerPhone", "0912345678");
        reservationRequest.put("partySize", 6);
        reservationRequest.put("reservationTime", futureTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("tableIds", Arrays.asList(9L)); // Party room
        reservationRequest.put("depositRequired", true);
        reservationRequest.put("depositAmount", 500000);

        Long cancellableReservationId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .body("data.depositRequired", equalTo(true))
                .extract()
                .jsonPath()
                .getLong("data.id");

        System.out.println("✓ Step 1: Reservation created with deposit: " + cancellableReservationId);

        // Step 2: Confirm reservation
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + cancellableReservationId + "/confirm")
                .then()
                .statusCode(200);

        System.out.println("✓ Step 2: Reservation confirmed");

        // Step 3: Customer calls to cancel
        Map<String, String> cancelRequest = new HashMap<>();
        cancelRequest.put("reason", "Family emergency");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(cancelRequest)
                .when()
                .patch("/reservations/" + cancellableReservationId + "/cancel")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CANCELLED"))
                .body("data.cancellationReason", equalTo("Family emergency"))
                .body("data.cancelledAt", notNullValue());

        System.out.println("✓ Step 3: Reservation cancelled");

        // Step 4: Check that tables are available again
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables/9")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("AVAILABLE"));

        System.out.println("✓ Step 4: Tables released and available");
        System.out.println("✅ Cancellation flow finished successfully!");
    }

    @Test
    @DisplayName("Flow 4: No-Show Management Flow")
    @Order(4)
    void testNoShowManagementFlow() {
        // Create reservation with past time for no-show scenario
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(30);
        
        Map<String, Object> reservationRequest = new HashMap<>();
        reservationRequest.put("customerName", "Charlie Brown");
        reservationRequest.put("customerPhone", "0923456789");
        reservationRequest.put("partySize", 3);
        reservationRequest.put("reservationTime", pastTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reservationRequest.put("tableIds", Arrays.asList(7L));
        reservationRequest.put("gracePeriodMinutes", 15);

        Long noShowReservationId = given()
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

        System.out.println("✓ Step 1: Reservation created (past time): " + noShowReservationId);

        // Confirm the reservation
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/reservations/" + noShowReservationId + "/confirm")
                .then()
                .statusCode(200);

        System.out.println("✓ Step 2: Reservation confirmed");

        // Staff marks as no-show after grace period
        Map<String, String> noShowRequest = new HashMap<>();
        noShowRequest.put("reason", "Customer did not arrive within grace period");

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(noShowRequest)
                .when()
                .patch("/reservations/" + noShowReservationId + "/no-show")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("NO_SHOW"))
                .body("data.cancelledAt", notNullValue());

        System.out.println("✓ Step 3: Marked as no-show");

        // Verify tables are released
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/tables/7")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("AVAILABLE"));

        System.out.println("✓ Step 4: Tables released");
        System.out.println("✅ No-show management flow finished successfully!");
    }

    @Test
    @DisplayName("Flow 5: Discount Application Scenarios")
    @Order(5)
    void testDiscountApplicationFlow() {
        // Create a bill
        Map<String, Object> billRequest = new HashMap<>();
        billRequest.put("tableIds", Arrays.asList(3L));
        billRequest.put("partySize", 4);

        Long discountBillId = given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(billRequest)
                .when()
                .post("/bills")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("data.id");

        // Place order
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("billId", discountBillId);
        orderRequest.put("orderType", "AT_TABLE");
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", 6); // Shrimp Pasta - 95000
        item.put("quantity", 3);
        items.add(item);
        
        orderRequest.put("items", items);

        given()
                .header("Authorization", "Bearer " + staffToken)
                .contentType(ContentType.JSON)
                .body(orderRequest)
                .when()
                .post("/orders")
                .then()
                .statusCode(201);

        System.out.println("✓ Step 1: Order placed");

        // Get bill total before discount
        Float totalBeforeDiscount = given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .get("/bills/" + discountBillId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getFloat("data.totalPrice");

        System.out.println("✓ Step 2: Bill total before discount: " + totalBeforeDiscount);

        // Apply percentage discount
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("discountCode", "WELCOME10")
                .when()
                .patch("/bills/" + discountBillId + "/apply-discount")
                .then()
                .statusCode(200)
                .body("data.discountAmount", greaterThan(0f))
                .body("data.finalPrice", lessThan(totalBeforeDiscount));

        System.out.println("✓ Step 3: 10% discount applied");

        // Try to apply another discount (should fail - only one discount per bill)
        given()
                .header("Authorization", "Bearer " + staffToken)
                .queryParam("discountCode", "LUNCH20")
                .when()
                .patch("/bills/" + discountBillId + "/apply-discount")
                .then()
                .statusCode(400);

        System.out.println("✓ Step 4: Second discount rejected (as expected)");

        // Remove discount
        given()
                .header("Authorization", "Bearer " + staffToken)
                .when()
                .patch("/bills/" + discountBillId + "/remove-discount")
                .then()
                .statusCode(200)
                .body("data.discountAmount", equalTo(0f))
                .body("data.finalPrice", equalTo(totalBeforeDiscount));

        System.out.println("✓ Step 5: Discount removed");
        System.out.println("✅ Discount application flow finished successfully!");
    }
}
