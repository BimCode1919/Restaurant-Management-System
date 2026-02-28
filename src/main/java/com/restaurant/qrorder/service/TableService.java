package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateTableRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateTableRequest;
import com.restaurant.qrorder.domain.dto.response.TableResponse;
import com.restaurant.qrorder.domain.entity.Reservation;
import com.restaurant.qrorder.domain.entity.RestaurantTable;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.repository.ReservationRepository;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Get all tables
     */
    @Transactional(readOnly = true)
        public List<TableResponse> getAllTables() {
        log.debug("Fetching all tables");
        return tableRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TableResponse> getAllTablesByDate(LocalDate date) {
        log.debug("Fetching all tables");

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        Set<Long> reservedTableIds = tableRepository
                .findReservedTablesForTimePeriod(startOfDay, endOfDay)
                .stream()
                .map(RestaurantTable::getId)
                .collect(Collectors.toSet());



        return tableRepository.findByStatus(TableStatus.AVAILABLE).stream()
                .map(table -> {
                    TableStatus dynamicStatus = reservedTableIds.contains(table.getId())
                            ? TableStatus.RESERVED
                            : table.getStatus();
                    return mapToResponseTable(table, dynamicStatus);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get table by ID
     */
    @Transactional(readOnly = true)
    public TableResponse getTableById(Long id) {
        log.debug("Fetching table with ID: {}", id);
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id));
        return mapToResponse(table);
    }

    /**
     * Get available tables
     */
    @Transactional(readOnly = true)
    public List<TableResponse> getAvailableTables() {
        log.debug("Fetching available tables");
        return tableRepository.findByStatus(TableStatus.AVAILABLE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get table by QR code
     */
    @Transactional(readOnly = true)
    public TableResponse getTableByQRCode(String qrCode) {
        log.debug("Fetching table with QR code: {}", qrCode);
        RestaurantTable table = tableRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with QR code: " + qrCode));
        return mapToResponse(table);
    }

    /**
     * Get table by table number
     */
    @Transactional(readOnly = true)
    public TableResponse getTableByNumber(String tableNumber) {
        log.debug("Fetching table with number: {}", tableNumber);
        RestaurantTable table = tableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with number: " + tableNumber));
        return mapToResponse(table);
    }

    /**
     * Create new table
     */
    @Transactional
    public TableResponse createTable(CreateTableRequest request) {
        log.info("Creating new table with number: {}", request.getTableNumber());
        
        // Check if table number already exists
        if (tableRepository.findByTableNumber(request.getTableNumber()).isPresent()) {
            throw new DuplicateResourceException("Table with number " + request.getTableNumber() + " already exists");
        }
        
        // Validate capacity
        if (request.getCapacity() < 1 || request.getCapacity() > 20) {
            throw new InvalidOperationException("Table capacity must be between 1 and 20");
        }
        
        RestaurantTable table = RestaurantTable.builder()
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : TableStatus.AVAILABLE)
                .qrCode(generateQRCode())
                .build();
        
        RestaurantTable saved = tableRepository.save(table);
        log.info("Table created successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Update existing table
     */
    @Transactional
    public TableResponse updateTable(Long id, UpdateTableRequest request) {
        log.info("Updating table with ID: {}", id);
        
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id));
        
        // Check if new table number conflicts with existing
        if (request.getTableNumber() != null && !request.getTableNumber().equals(table.getTableNumber())) {
            if (tableRepository.findByTableNumber(request.getTableNumber()).isPresent()) {
                throw new DuplicateResourceException("Table with number " + request.getTableNumber() + " already exists");
            }
            table.setTableNumber(request.getTableNumber());
        }
        
        // Validate capacity if provided
        if (request.getCapacity() != null) {
            if (request.getCapacity() < 1 || request.getCapacity() > 20) {
                throw new InvalidOperationException("Table capacity must be between 1 and 20");
            }
            table.setCapacity(request.getCapacity());
        }
        
        if (request.getLocation() != null) {
            table.setLocation(request.getLocation());
        }
        
        if (request.getStatus() != null) {
            table.setStatus(request.getStatus());
        }
        
        RestaurantTable updated = tableRepository.save(table);
        log.info("Table {} updated successfully", id);
        return mapToResponse(updated);
    }

    /**
     * Update table status
     */
    @Transactional
    public TableResponse updateTableStatus(Long id, TableStatus status) {
        log.info("Updating table {} status to {}", id, status);
        
        if (status == null) {
            throw new InvalidOperationException("Status cannot be null");
        }
        
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id));
        
        table.setStatus(status);
        RestaurantTable updated = tableRepository.save(table);
        
        log.info("Table {} status updated to {}", id, status);
        return mapToResponse(updated);
    }

    /**
     * Regenerate QR code for table
     */
    @Transactional
    public TableResponse regenerateQRCode(Long id) {
        log.info("Regenerating QR code for table {}", id);
        
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id));
        
        table.setQrCode(generateQRCode());
        RestaurantTable updated = tableRepository.save(table);
        
        log.info("QR code regenerated for table {}", id);
        return mapToResponse(updated);
    }

    /**
     * Delete table
     */
    @Transactional
    public void deleteTable(Long id) {
        log.info("Deleting table with ID: {}", id);
        
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id));
        
        tableRepository.delete(table);
        log.info("Table {} deleted successfully", id);
    }

    /**
     * Generate unique QR code
     */
    private String generateQRCode() {
        return "TABLE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Map entity to response DTO
     */
    private TableResponse mapToResponse(RestaurantTable table) {
        return TableResponse.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .location(table.getLocation())
                .qrCode(table.getQrCode())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }

    private TableResponse mapToResponseTable(RestaurantTable table, TableStatus dynamicStatus) {
        return TableResponse.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .status(dynamicStatus)  // overrides DB status
                .capacity(table.getCapacity())
                .location(table.getLocation())
                .qrCode(table.getQrCode())
                .build();
    }
}
