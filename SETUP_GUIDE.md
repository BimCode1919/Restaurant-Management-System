# 🚀 HƯỚNG DẪN SETUP & CHẠY PROJECT

## 📋 Yêu cầu hệ thống

- **Java**: JDK 21+
- **Maven**: 3.8+
- **Docker**: Latest version
- **Docker Compose**: Latest version
- **IDE**: IntelliJ IDEA / VS Code (với Java Extension Pack)

## 🔧 Các bước setup

### 1. Clone & Open Project

```bash
cd f:\dev\java\sp26\swd
```

Mở project trong IDE của bạn.

### 2. Start Docker Services

```bash
docker-compose up -d
```

Services sẽ start:
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`

Kiểm tra containers:
```bash
docker ps
```

### 3. Build Project

```bash
mvn clean install
```

### 4. Run Application

**Option 1 - Maven:**
```bash
mvn spring-boot:run
```

**Option 2 - IDE:**
- Right click on `RestaurantQROrderApplication.java`
- Click "Run"

### 5. Verify Application

Application chạy tại: `http://localhost:8080/api`

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

## 📊 Database Schema

Database được tự động tạo khi app start (JPA `ddl-auto: update`).

Tables được tạo:
- users
- roles  
- categories
- items
- tables
- bills
- bill_tables
- orders
- order_details
- ingredients

## 👤 Default Accounts

### Admin Account
```
Email: admin@restaurant.com
Password: admin123
```

### Staff Account
```
Email: staff@restaurant.com  
Password: staff123
```

## 🧪 Test API với Swagger

1. Mở Swagger UI: http://localhost:8080/api/swagger-ui.html

2. **Login để lấy token:**
   - Expand `Authentication` -> `POST /auth/login`
   - Click "Try it out"
   - Body:
   ```json
   {
     "email": "admin@restaurant.com",
     "password": "admin123"
   }
   ```
   - Click "Execute"
   - Copy `token` từ response

3. **Authorize:**
   - Click nút "Authorize" 🔒 (góc phải trên)
   - Nhập: `Bearer YOUR_TOKEN_HERE`
   - Click "Authorize"

4. **Test các API:**
   - Categories: GET /api/categories
   - Items: GET /api/items/available
   - Tables: POST /api/tables
   - Orders: POST /api/orders
   - Bills: GET /api/bills/open

## 📝 Workflow thực tế

### 1. Tạo Category & Items (Admin/Staff)

```bash
# Create Category
POST /api/categories
{
  "name": "Vietnamese Food",
  "description": "Traditional Vietnamese dishes"
}

# Create Item
POST /api/items
{
  "name": "Pho Bo",
  "price": 50000,
  "unit": "bowl",
  "categoryId": 1,
  "description": "Beef noodle soup"
}
```

### 2. Tạo Table (Admin/Staff)

```bash
POST /api/tables
{
  "tableNumber": "T01"
}
```

Response sẽ có `qrCode` ID. QR image được generate tại folder `./qr-codes/`

### 3. Customer Order qua QR Code

```bash
# Customer scan QR -> Get table info
GET /api/tables/qr/{qrCode}

# Customer đặt món
POST /api/orders
{
  "tableId": 1,
  "items": [
    {
      "itemId": 1,
      "quantity": 2,
      "note": "Extra spicy"
    },
    {
      "itemId": 3,
      "quantity": 1
    }
  ]
}
```

### 4. Kitchen nhận order (Staff)

```bash
# View pending orders
GET /api/orders/pending

# Update order detail status
PATCH /api/orders/details/{orderDetailId}/status?status=PREPARING
PATCH /api/orders/details/{orderDetailId}/status?status=READY
PATCH /api/orders/details/{orderDetailId}/status?status=SERVED
```

### 5. Thanh toán (Staff/Admin)

```bash
# Get bill for table
GET /api/bills/table/{tableId}

# Close bill (payment)
POST /api/bills/{billId}/close
```

## 🏗️ Project Structure

```
com.restaurant.qrorder/
├── config/                    # Security, Redis, OpenAPI configs
├── controller/                # REST API endpoints
│   ├── AuthController
│   ├── CategoryController
│   ├── ItemController
│   ├── TableController
│   ├── OrderController
│   └── BillController
├── service/                   # Business logic
│   ├── AuthService
│   ├── CategoryService
│   ├── ItemService
│   ├── TableService
│   ├── OrderService
│   └── BillService
├── repository/                # Data access (JPA)
├── domain/
│   ├── entity/               # JPA Entities
│   ├── dto/
│   │   ├── request/          # Request DTOs
│   │   └── response/         # Response DTOs
│   └── common/               # Enums
├── mapper/                    # MapStruct mappers
├── exception/
│   ├── custom/               # Custom exceptions
│   └── handler/              # Global exception handler
└── util/                     # JWT, QR Code utilities
```

## 🔐 Security

- JWT authentication
- Role-based authorization (ADMIN, STAFF, CUSTOMER)
- Password encryption (BCrypt)
- CORS enabled

### Authorization Rules:

- **Public**: `/auth/**`, `/swagger-ui/**`
- **Authenticated**: All other endpoints
- **ADMIN**: Delete operations, cancel bills
- **ADMIN + STAFF**: Create/Update categories, items, tables, manage bills
- **CUSTOMER**: Create orders, view own data

## 🛠️ Development Tips

### Hot Reload

Spring Boot DevTools được enable. Code changes sẽ tự động reload.

### Profiles

```bash
# Development
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Database Reset

```bash
# Stop app
# In application-dev.yml: ddl-auto: create-drop
# Restart app -> database sẽ được tạo lại
```

### View Logs

```bash
# In application.yml, set log level:
logging:
  level:
    com.restaurant.qrorder: DEBUG
```

## 📦 Build for Production

```bash
# Package
mvn clean package -DskipTests

# Run JAR
java -jar target/qrorder-1.0.0.jar --spring.profiles.active=prod
```

## 🐛 Troubleshooting

### Port 8080 already in use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Change port in application.yml
server.port: 8081
```

### Docker containers not starting
```bash
docker-compose down
docker-compose up -d
```

### Database connection failed
- Check PostgreSQL container: `docker logs restaurant_postgres`
- Verify credentials in application.yml

### JWT token expired
- Get new token from `/auth/refresh` with refresh token
- Or login again

## 📚 API Documentation

Swagger UI cung cấp:
- ✅ Tất cả endpoints
- ✅ Request/Response schemas
- ✅ Try it out functionality
- ✅ Authorization

## 🎯 Next Steps

1. ✅ Frontend integration
2. ✅ WebSocket for real-time kitchen updates
3. ✅ Payment gateway integration (MoMo/VNPay)
4. ✅ File upload for item images
5. ✅ Reports & Analytics
6. ✅ Unit & Integration tests

## 💡 Design Patterns Được Sử Dụng

Theo `DESIGN_PATTERNS_GUIDE.md`:

1. ✅ **Layered Architecture** - Controller/Service/Repository
2. ✅ **Dependency Injection** - Constructor injection với Lombok
3. ✅ **Repository Pattern** - Spring Data JPA
4. ✅ **DTO Pattern** - Request/Response separation
5. ✅ **Mapper Pattern** - MapStruct
6. ✅ **Builder Pattern** - Lombok @Builder
7. ✅ **Exception Handling** - Global @RestControllerAdvice
8. ✅ **Response Wrapper** - Uniform ApiResponse
9. ✅ **Authentication** - JWT with Spring Security

## 📞 Support

Nếu gặp issues, check:
1. Logs trong console
2. Swagger UI error responses
3. Database connection
4. Docker containers status

Happy Coding! 🎉
