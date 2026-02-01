package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateTableRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateTableRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.TableResponse;
import com.restaurant.qrorder.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Tag(name = "Table Management", description = "APIs for managing restaurant tables")
@SecurityRequirement(name = "bearerAuth")
public class TableController {

    TableService tableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all tables", description = "Retrieve all restaurant tables (Admin/Staff only)")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getAllTables() {
        return ResponseEntity.ok(
                ApiResponse.<List<TableResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Tables retrieved successfully")
                        .data(tableService.getAllTables())
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get table by ID", description = "Retrieve a specific table by its ID (Admin/Staff only)")
    public ResponseEntity<ApiResponse<TableResponse>> getTableById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<TableResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Table retrieved successfully")
                        .data(tableService.getTableById(id))
                        .build());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get available tables", description = "Retrieve all currently available tables")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getAvailableTables() {
        return ResponseEntity.ok(
                ApiResponse.<List<TableResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Available tables retrieved successfully")
                        .data(tableService.getAvailableTables())
                        .build());
    }

    @GetMapping("/qr/{qrCode}")
    @Operation(summary = "Get table by QR code", description = "Retrieve table information by scanning QR code")
    public ResponseEntity<ApiResponse<TableResponse>> getTableByQRCode(@PathVariable String qrCode) {
        return ResponseEntity.ok(
                ApiResponse.<TableResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Table retrieved successfully")
                        .data(tableService.getTableByQRCode(qrCode))
                        .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new table", description = "Create a new restaurant table (Admin only)")
    public ResponseEntity<ApiResponse<TableResponse>> createTable(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<TableResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Table created successfully")
                        .data(tableService.createTable(request))
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update table", description = "Update an existing table (Admin only)")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<TableResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Table updated successfully")
                        .data(tableService.updateTable(id, request))
                        .build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update table status", description = "Update the status of a table (Admin/Staff only)")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableStatus(
            @PathVariable Long id,
            @RequestParam TableStatus status) {
        return ResponseEntity.ok(
                ApiResponse.<TableResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Table status updated successfully")
                        .data(tableService.updateTableStatus(id, status))
                        .build());
    }

    @PatchMapping("/{id}/regenerate-qr")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerate QR code", description = "Generate a new QR code for a table (Admin only)")
    public ResponseEntity<ApiResponse<TableResponse>> regenerateQRCode(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<TableResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("QR code regenerated successfully")
                        .data(tableService.regenerateQRCode(id))
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete table", description = "Delete a table (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Table deleted successfully")
                        .build());
    }
}
