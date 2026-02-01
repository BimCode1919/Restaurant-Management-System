# Restaurant QR Order System - Integration Tests

## Overview
This test suite provides comprehensive integration testing for all API endpoints and complete user flows of the Restaurant QR Order System.

## Test Structure

### Base Test Class
- **BaseIntegrationTest**: Abstract base class providing:
  - TestContainers setup for PostgreSQL
  - RestAssured configuration
  - Common authentication methods
  - Test data utilities

### Integration Test Classes

#### 1. AuthenticationIntegrationTest
Tests for authentication and authorization:
- ✅ User registration (success, validation errors, duplicate email)
- ✅ User login (success, wrong password, non-existent user)
- ✅ Token refresh
- ✅ Invalid input validation

**Test Cases**: 10
- Register with valid data
- Register with duplicate email
- Register with invalid email format
- Register with empty password
- Login with correct credentials
- Login with wrong password
- Login with non-existent email
- Login with invalid email format
- Refresh token successfully
- Refresh token without authorization

---

#### 2. CategoryIntegrationTest
Tests for category management:
- ✅ Get all categories (public access)
- ✅ Get category by ID
- ✅ Create category (admin only)
- ✅ Update category (admin only)
- ✅ Delete category (admin only)
- ✅ Role-based access control
- ✅ Validation errors

**Test Cases**: 14
- Get all categories without authentication
- Get category by ID successfully
- Return 404 for non-existent category
- Admin creates category successfully
- Fail to create category with duplicate name
- Staff cannot create category (403)
- Fail to create category without authentication
- Fail to create category with empty name
- Admin updates category successfully
- Fail to update non-existent category
- Staff cannot update category (403)
- Admin deletes category successfully
- Fail to delete non-existent category
- Staff cannot delete category (403)

---

#### 3. ItemIntegrationTest
Tests for menu item management:
- ✅ Get all items with pagination
- ✅ Filter items by category
- ✅ Get available items
- ✅ Create item (admin only)
- ✅ Update item (admin only)
- ✅ Toggle item availability
- ✅ Delete item (admin only)
- ✅ Validation (price, name, category)

**Test Cases**: 16
- Get all items with pagination
- Filter items by category
- Get item by ID
- Return 404 for non-existent item
- Get available items only
- Admin creates item successfully
- Fail to create item with invalid price
- Fail to create item without name
- Fail to create item with non-existent category
- Staff cannot create item (403)
- Admin updates item successfully
- Fail to update non-existent item
- Admin toggles item availability
- Admin deletes item successfully
- Fail to delete non-existent item
- Staff cannot delete item (403)

---

#### 4. TableIntegrationTest
Tests for table management:
- ✅ Get all tables (admin/staff only)
- ✅ Filter tables by status and capacity
- ✅ Get table by ID
- ✅ Create table (admin only)
- ✅ Update table and status
- ✅ Generate new QR code
- ✅ Delete table (admin only)
- ✅ Get available tables in time range

**Test Cases**: 21
- Admin gets all tables
- Staff gets all tables
- Customer cannot get all tables (403)
- Filter tables by status
- Filter tables by capacity
- Get table by ID
- Return 404 for non-existent table
- Admin creates table successfully
- Fail to create table with duplicate number
- Fail to create table with invalid capacity
- Staff cannot create table (403)
- Admin updates table successfully
- Update table status
- Fail to update status with invalid value
- Staff updates table status
- Customer cannot update table status (403)
- Generate new QR code for table
- Admin deletes table successfully
- Fail to delete non-existent table
- Staff cannot delete table (403)
- Get available tables in time range

---

#### 5. ReservationIntegrationTest
Tests for reservation management:
- ✅ Create reservation
- ✅ Get all reservations
- ✅ Filter by status and date
- ✅ Update reservation
- ✅ Confirm reservation
- ✅ Mark as seated
- ✅ Cancel reservation
- ✅ Mark as no-show
- ✅ Delete reservation (admin only)
- ✅ Validation (time, party size, customer info)

**Test Cases**: 18
- Create reservation successfully
- Fail to create reservation with past time
- Fail to create reservation without customer name
- Fail to create reservation with invalid party size
- Get all reservations
- Get reservation by ID
- Return 404 for non-existent reservation
- Filter reservations by status
- Filter reservations by date
- Update reservation successfully
- Fail to update non-existent reservation
- Confirm reservation
- Mark reservation as seated
- Cancel reservation
- Mark reservation as no-show
- Delete reservation
- Fail to delete non-existent reservation
- Staff cannot delete reservation (403)

---

#### 6. RestaurantCompleteFlowTest
End-to-end integration tests for complete user flows:

**Flow 1: Complete Reservation Flow**
1. Customer makes reservation
2. Staff confirms reservation
3. Customer arrives, marked as seated
4. Bill automatically created
5. Customer places order
6. Apply discount
7. Process payment
8. Close bill
9. Complete reservation

**Flow 2: Walk-in Customer Flow**
1. Find available table
2. Create bill for walk-in
3. Mark table as occupied
4. Place initial order
5. Place additional order
6. Get final bill
7. Process MoMo payment
8. Close bill
9. Release table

**Flow 3: Reservation Cancellation Flow**
1. Create reservation with deposit
2. Confirm reservation
3. Customer cancels
4. Verify tables released

**Flow 4: No-Show Management Flow**
1. Create reservation (past time)
2. Confirm reservation
3. Mark as no-show after grace period
4. Verify tables released

**Flow 5: Discount Application Scenarios**
1. Create bill and place order
2. Get bill total before discount
3. Apply percentage discount
4. Try to apply second discount (should fail)
5. Remove discount

**Test Cases**: 5 complete flows

---

## Running Tests

### Prerequisites
- Java 21
- Maven 3.8+
- Docker (for TestContainers)

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=AuthenticationIntegrationTest
mvn test -Dtest=CategoryIntegrationTest
mvn test -Dtest=ItemIntegrationTest
mvn test -Dtest=TableIntegrationTest
mvn test -Dtest=ReservationIntegrationTest
mvn test -Dtest=RestaurantCompleteFlowTest
```

### Run with Coverage
```bash
mvn clean test jacoco:report
```

## Test Coverage Summary

| Module | Test Classes | Test Cases | Coverage |
|--------|-------------|------------|----------|
| Authentication | 1 | 10 | ✅ Complete |
| Category Management | 1 | 14 | ✅ Complete |
| Item Management | 1 | 16 | ✅ Complete |
| Table Management | 1 | 21 | ✅ Complete |
| Reservation Management | 1 | 18 | ✅ Complete |
| Complete Flows | 1 | 5 | ✅ Complete |
| **Total** | **6** | **84** | **✅ Complete** |

## Test Categories

### 1. Happy Path Tests
- Valid inputs and expected successful responses
- Standard business flows

### 2. Negative Tests
- Invalid inputs
- Missing required fields
- Duplicate data
- Non-existent resources (404)

### 3. Authorization Tests
- Role-based access control (ADMIN, STAFF, CUSTOMER)
- Unauthorized access (401)
- Forbidden access (403)

### 4. Validation Tests
- Input validation
- Business rule validation
- Data integrity checks

### 5. Edge Cases
- Past dates for reservations
- Multiple orders on same bill
- Discount limitations
- Table availability conflicts

### 6. Integration Flows
- Complete end-to-end scenarios
- Multi-step processes
- State transitions
- Real-world use cases

## Key Features Tested

✅ **Authentication & Authorization**
- JWT token generation and validation
- Role-based access control
- Password encryption

✅ **Category Management**
- CRUD operations
- Unique constraint validation

✅ **Item Management**
- Pagination and filtering
- Availability toggle
- Category association

✅ **Table Management**
- Status management
- QR code generation
- Capacity and location tracking

✅ **Reservation System**
- Time slot booking
- Status workflow (PENDING → CONFIRMED → SEATED → COMPLETED)
- Cancellation and no-show handling
- Grace period management

✅ **Order Management**
- Multiple orders per bill
- Order item details
- Real-time price calculation

✅ **Discount System**
- Discount code application
- One discount per bill rule
- Discount removal

✅ **Payment Processing**
- Multiple payment methods (CASH, MOMO, etc.)
- Payment status tracking

✅ **Bill Management**
- Total calculation
- Discount application
- Bill closing

## Test Data

Tests use the initial database seeding:
- **Users**: admin@restaurant.com, staff@restaurant.com, chef@restaurant.com
- **Categories**: 5 categories (Appetizers, Main Course, Beverages, Desserts, Soup)
- **Items**: 16 menu items
- **Tables**: 10 tables with varying capacities
- **Discounts**: 12 discount codes

## Notes

1. **TestContainers**: Tests use PostgreSQL TestContainers for isolated database testing
2. **Parallel Execution**: Tests are ordered to ensure data consistency
3. **Authentication**: Each test class authenticates as needed (admin, staff, customer)
4. **Data Cleanup**: TestContainers automatically cleans up after tests
5. **Assertions**: Uses Hamcrest matchers for readable assertions

## Future Enhancements

- [ ] Payment gateway integration tests
- [ ] WebSocket notification tests
- [ ] Performance tests
- [ ] Load tests
- [ ] Security penetration tests
- [ ] API documentation tests (Swagger/OpenAPI validation)

## Contributing

When adding new tests:
1. Follow existing naming conventions
2. Add descriptive @DisplayName annotations
3. Use @Order for dependent tests
4. Include both positive and negative test cases
5. Test all authorization scenarios
6. Update this README with new test cases
