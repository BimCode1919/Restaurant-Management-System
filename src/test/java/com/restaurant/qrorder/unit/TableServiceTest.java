package com.restaurant.qrorder.unit;

import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateTableRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateTableRequest;
import com.restaurant.qrorder.domain.dto.response.TableResponse;
import com.restaurant.qrorder.domain.entity.RestaurantTable;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import com.restaurant.qrorder.service.TableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableService Unit Tests")
class TableServiceTest {

    @Mock private RestaurantTableRepository tableRepository;

    @InjectMocks private TableService tableService;

    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = RestaurantTable.builder()
                .id(1L)
                .tableNumber("T01")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .location("Ground Floor")
                .qrCode("TABLE_ABC123456789")
                .build();
    }

    @Nested
    @DisplayName("getAllTables()")
    class GetAllTables {

        @Test
        @DisplayName("returns mapped list of all tables")
        void returnsAllTables() {
            when(tableRepository.findAll()).thenReturn(List.of(table));

            List<TableResponse> result = tableService.getAllTables();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTableNumber()).isEqualTo("T01");
        }

        @Test
        @DisplayName("returns empty list when no tables exist")
        void returnsEmptyList() {
            when(tableRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(tableService.getAllTables()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTableById()")
    class GetTableById {

        @Test
        @DisplayName("table found → returns response")
        void found_returnsResponse() {
            when(tableRepository.findById(1L)).thenReturn(Optional.of(table));

            TableResponse result = tableService.getTableById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("table NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(tableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.getTableById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with ID: 99");
        }
    }

    @Nested
    @DisplayName("getAvailableTables()")
    class GetAvailableTables {

        @Test
        @DisplayName("returns only AVAILABLE tables")
        void returnsAvailableTables() {
            when(tableRepository.findByStatus(TableStatus.AVAILABLE)).thenReturn(List.of(table));

            List<TableResponse> result = tableService.getAvailableTables();

            assertThat(result).hasSize(1);
            verify(tableRepository).findByStatus(TableStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("getTableByQRCode()")
    class GetTableByQRCode {

        @Test
        @DisplayName("QR code found → returns response")
        void found_returnsResponse() {
            when(tableRepository.findByQrCode("TABLE_ABC123456789")).thenReturn(Optional.of(table));

            TableResponse result = tableService.getTableByQRCode("TABLE_ABC123456789");

            assertThat(result.getQrCode()).isEqualTo("TABLE_ABC123456789");
        }

        @Test
        @DisplayName("QR code NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(tableRepository.findByQrCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.getTableByQRCode("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with QR code: INVALID");
        }
    }

    @Nested
    @DisplayName("getTableByNumber()")
    class GetTableByNumber {

        @Test
        @DisplayName("table number found → returns response")
        void found_returnsResponse() {
            when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(table));

            TableResponse result = tableService.getTableByNumber("T01");

            assertThat(result.getTableNumber()).isEqualTo("T01");
        }

        @Test
        @DisplayName("table number NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(tableRepository.findByTableNumber("T99")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.getTableByNumber("T99"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with number: T99");
        }
    }

    @Nested
    @DisplayName("createTable()")
    class CreateTable {

        private CreateTableRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateTableRequest();
            request.setTableNumber("T02");
            request.setCapacity(4);
            request.setLocation("First Floor");
            request.setStatus(null);
        }

        @Test
        @DisplayName("duplicate table number → throws DuplicateResourceException")
        void duplicateNumber_throws() {
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.of(table));

            assertThatThrownBy(() -> tableService.createTable(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("T02");

            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("capacity < 1 → throws InvalidOperationException")
        void capacityTooLow_throws() {
            request.setCapacity(0);
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.createTable(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("capacity must be between 1 and 20");
        }

        @Test
        @DisplayName("capacity > 20 → throws InvalidOperationException")
        void capacityTooHigh_throws() {
            request.setCapacity(21);
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.createTable(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("capacity must be between 1 and 20");
        }

        @Test
        @DisplayName("status null → defaults to AVAILABLE")
        void nullStatus_defaultsToAvailable() {
            request.setStatus(null);
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            tableService.createTable(request);

            verify(tableRepository).save(argThat(t -> t.getStatus() == TableStatus.AVAILABLE));
        }

        @Test
        @DisplayName("status provided → uses provided status")
        void statusProvided_usesProvidedStatus() {
            request.setStatus(TableStatus.RESERVED);
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            tableService.createTable(request);

            verify(tableRepository).save(argThat(t -> t.getStatus() == TableStatus.RESERVED));
        }

        @Test
        @DisplayName("valid request → saves and returns table response")
        void validRequest_savesAndReturns() {
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            TableResponse result = tableService.createTable(request);

            assertThat(result).isNotNull();
            verify(tableRepository).save(any(RestaurantTable.class));
        }

        @Test
        @DisplayName("capacity exactly 1 → valid (boundary)")
        void capacityOne_valid() {
            request.setCapacity(1);
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            assertThatNoException().isThrownBy(() -> tableService.createTable(request));
        }

        @Test
        @DisplayName("capacity exactly 20 → valid (boundary)")
        void capacityTwenty_valid() {
            request.setCapacity(20);
            when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            assertThatNoException().isThrownBy(() -> tableService.createTable(request));
        }
    }

    @Nested
    @DisplayName("updateTable()")
    class UpdateTable {

        private UpdateTableRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateTableRequest();
            when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
            when(tableRepository.save(table)).thenReturn(table);
        }

        @Test
        @DisplayName("table NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(tableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.updateTable(99L, new UpdateTableRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with ID: 99");
        }

        @Test
        @DisplayName("tableNumber null → table number NOT updated")
        void nullTableNumber_notUpdated() {
            request.setTableNumber(null);

            tableService.updateTable(1L, request);

            assertThat(table.getTableNumber()).isEqualTo("T01");
            verify(tableRepository, never()).findByTableNumber(any());
        }

        @Test
        @DisplayName("tableNumber same as current → no conflict check, no update")
        void sameTableNumber_skipsConflictCheck() {
            request.setTableNumber("T01");

            tableService.updateTable(1L, request);

            verify(tableRepository, never()).findByTableNumber(any());
            assertThat(table.getTableNumber()).isEqualTo("T01");
        }

        @Test
        @DisplayName("tableNumber changed, no conflict → updates table number")
        void newTableNumber_noConflict_updates() {
            request.setTableNumber("T99");
            when(tableRepository.findByTableNumber("T99")).thenReturn(Optional.empty());

            tableService.updateTable(1L, request);

            assertThat(table.getTableNumber()).isEqualTo("T99");
        }

        @Test
        @DisplayName("tableNumber changed, conflicts with existing → throws DuplicateResourceException")
        void newTableNumber_conflicts_throws() {
            request.setTableNumber("T99");
            when(tableRepository.findByTableNumber("T99")).thenReturn(Optional.of(new RestaurantTable()));

            assertThatThrownBy(() -> tableService.updateTable(1L, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("T99");
        }

        @Test
        @DisplayName("capacity null → capacity NOT updated")
        void nullCapacity_notUpdated() {
            request.setCapacity(null);

            tableService.updateTable(1L, request);

            assertThat(table.getCapacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("capacity < 1 → throws InvalidOperationException")
        void capacityTooLow_throws() {
            request.setCapacity(0);

            assertThatThrownBy(() -> tableService.updateTable(1L, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("capacity must be between 1 and 20");
        }

        @Test
        @DisplayName("capacity > 20 → throws InvalidOperationException")
        void capacityTooHigh_throws() {
            request.setCapacity(21);

            assertThatThrownBy(() -> tableService.updateTable(1L, request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("capacity must be between 1 and 20");
        }

        @Test
        @DisplayName("valid capacity → capacity updated")
        void validCapacity_updated() {
            request.setCapacity(6);

            tableService.updateTable(1L, request);

            assertThat(table.getCapacity()).isEqualTo(6);
        }

        @Test
        @DisplayName("location null → location NOT updated")
        void nullLocation_notUpdated() {
            table.setLocation("Ground Floor");
            request.setLocation(null);

            tableService.updateTable(1L, request);

            assertThat(table.getLocation()).isEqualTo("Ground Floor");
        }

        @Test
        @DisplayName("location non-null → location updated")
        void nonNullLocation_updated() {
            request.setLocation("Rooftop");

            tableService.updateTable(1L, request);

            assertThat(table.getLocation()).isEqualTo("Rooftop");
        }

        @Test
        @DisplayName("status null → status NOT updated")
        void nullStatus_notUpdated() {
            request.setStatus(null);

            tableService.updateTable(1L, request);

            assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        }

        @Test
        @DisplayName("status non-null → status updated")
        void nonNullStatus_updated() {
            request.setStatus(TableStatus.OCCUPIED);

            tableService.updateTable(1L, request);

            assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        }
    }

    @Nested
    @DisplayName("updateTableStatus()")
    class UpdateTableStatus {

        @Test
        @DisplayName("status null → throws InvalidOperationException")
        void nullStatus_throws() {
            assertThatThrownBy(() -> tableService.updateTableStatus(1L, null))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Status cannot be null");

            verify(tableRepository, never()).findById(any());
        }

        @Test
        @DisplayName("table NOT found → throws ResourceNotFoundException")
        void tableNotFound_throws() {
            when(tableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.updateTableStatus(99L, TableStatus.OCCUPIED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with ID: 99");
        }

        @Test
        @DisplayName("valid status → updates and returns response")
        void validStatus_updatesTable() {
            when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
            when(tableRepository.save(table)).thenReturn(table);

            TableResponse result = tableService.updateTableStatus(1L, TableStatus.OCCUPIED);

            assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("regenerateQRCode()")
    class RegenerateQRCode {

        @Test
        @DisplayName("table NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(tableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.regenerateQRCode(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with ID: 99");
        }

        @Test
        @DisplayName("table found → QR code regenerated and saved")
        void found_regeneratesQrCode() {
            String oldQrCode = table.getQrCode();
            when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
            when(tableRepository.save(table)).thenReturn(table);

            TableResponse result = tableService.regenerateQRCode(1L);

            assertThat(table.getQrCode()).isNotEqualTo(oldQrCode);
            assertThat(table.getQrCode()).startsWith("TABLE_");
            assertThat(result).isNotNull();
            verify(tableRepository).save(table);
        }
    }

    @Nested
    @DisplayName("deleteTable()")
    class DeleteTable {

        @Test
        @DisplayName("table found → deleted")
        void found_deleted() {
            when(tableRepository.findById(1L)).thenReturn(Optional.of(table));

            tableService.deleteTable(1L);

            verify(tableRepository).delete(table);
        }

        @Test
        @DisplayName("table NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(tableRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.deleteTable(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with ID: 99");

            verify(tableRepository, never()).delete(any(RestaurantTable.class));
        }
    }
}