# 🍽️ Restaurant QR Order System

Hệ thống quản lý nhà hàng với đặt món qua QR code.

## Tính năng

- Quản lý bàn với QR code
- Đặt món qua QR
- Quản lý menu, nguyên liệu
- Xử lý đơn hàng, hóa đơn
- Phân quyền Admin/Staff
- JWT Authentication

## Tech Stack

Java 21 • Spring Boot 3.4 • PostgreSQL • Redis • JWT • Swagger

## Cài đặt

```bash
git clone <repository-url>
cd swd

docker-compose up -d

mvn spring-boot:run
```

Swagger UI: **http://localhost:8080/api/swagger-ui.html**

## Tài khoản test

Admin: `admin@restaurant.com` / `admin123`  
Staff: `staff@restaurant.com` / `staff123`

Xem chi tiết: [QUICK_START.md](QUICK_START.md)

# Kiểm tra trạng thái
docker-compose ps

# Xem logs nếu cần
docker-compose logs -f
```

**Thông tin kết nối:**
- PostgreSQL: `localhost:5432`
  - Database: `restaurant_qr_db`
  - User: `restaurant_user`
  - Password: `restaurant_pass`
Xem chi tiết: [QUICK_START.md](QUICK_START.md)

## Cấu trúc

```
src/main/java/com/restaurant/qrorder/
├── config/         # Cấu hình
├── controller/     # REST API
├── domain/         # Entity, DTO
├── service/        # Business Logic
├── repository/     # Database
└── mapper/         # MapStruct
```

## License

MIT
