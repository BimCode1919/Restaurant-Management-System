package com.restaurant.qrorder.unit;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateBillRequest;
import com.restaurant.qrorder.domain.dto.response.BillResponse;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.repository.BillRepository;
import com.restaurant.qrorder.repository.ReservationRepository;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import com.restaurant.qrorder.service.BillService;
import com.restaurant.qrorder.service.DiscountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillService Unit Tests")
class BillServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private DiscountService discountService;
    @Mock private RestaurantTableRepository tableRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks private BillService billService;

    private RestaurantTable availableTable;
    private Bill openBill;
    private Bill paidBill;
    private Bill closedBill;

    @BeforeEach
    void setUp() {
        availableTable = new RestaurantTable();
        availableTable.setId(1L);
        availableTable.setTableNumber("T01");
        availableTable.setStatus(TableStatus.AVAILABLE);

        openBill = Bill.builder()
                .id(1L)
                .status(BillStatus.OPEN)
                .totalPrice(BigDecimal.valueOf(200))
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.valueOf(200))
                .partySize(2)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        paidBill = Bill.builder()
                .id(2L)
                .status(BillStatus.PAID)
                .totalPrice(BigDecimal.valueOf(200))
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.valueOf(200))
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        closedBill = Bill.builder()
                .id(3L)
                .status(BillStatus.CLOSED)
                .totalPrice(BigDecimal.valueOf(200))
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.valueOf(200))
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("createBill()")
    class CreateBill {

        private CreateBillRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateBillRequest();
            request.setTableIds(List.of(1L));
            request.setPartySize(2);
            request.setReservationId(null);
        }

        @Test
        @DisplayName("table not found → throws ResourceNotFoundException")
        void tableNotFound_throws() {
            when(tableRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billService.createBill(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Table not found with ID: 1");

            verify(billRepository, never()).save(any());
        }

        @Test
        @DisplayName("table not AVAILABLE → throws InvalidOperationException")
        void tableNotAvailable_throws() {
            availableTable.setStatus(TableStatus.OCCUPIED);
            when(tableRepository.findById(1L)).thenReturn(Optional.of(availableTable));

            assertThatThrownBy(() -> billService.createBill(request))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not available");

            verify(billRepository, never()).save(any());
        }

        @Test
        @DisplayName("reservationId null → bill created without reservation")
        void noReservation_billCreated() {
            when(tableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
            when(billRepository.save(any(Bill.class))).thenReturn(openBill);

            BillResponse result = billService.createBill(request);

            assertThat(result).isNotNull();
            verify(reservationRepository, never()).findById(any());
            verify(tableRepository, atLeastOnce()).save(availableTable);
        }

        @Test
        @DisplayName("reservationId provided, reservation found → bill linked to reservation")
        void withReservation_found_billLinked() {
            request.setReservationId(10L);
            Reservation reservation = new Reservation();
            reservation.setId(10L);

            when(tableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
            when(billRepository.save(any(Bill.class))).thenReturn(openBill);
            when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

            BillResponse result = billService.createBill(request);

            assertThat(result).isNotNull();
            verify(reservationRepository).findById(10L);
        }

        @Test
        @DisplayName("reservationId provided, reservation NOT found → throws ResourceNotFoundException")
        void withReservation_notFound_throws() {
            request.setReservationId(99L);
            when(tableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
            when(billRepository.save(any(Bill.class))).thenReturn(openBill);
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billService.createBill(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation not found");
        }

        @Test
        @DisplayName("table set to OCCUPIED after bill creation")
        void tableSetToOccupied_afterCreation() {
            when(tableRepository.findById(1L)).thenReturn(Optional.of(availableTable));
            when(billRepository.save(any(Bill.class))).thenReturn(openBill);

            billService.createBill(request);

            assertThat(availableTable.getStatus()).isEqualTo(TableStatus.OCCUPIED);
            verify(tableRepository, atLeastOnce()).save(availableTable);
        }
    }

    @Nested
    @DisplayName("getBillResponseById()")
    class GetBillResponseById {

        @Test
        @DisplayName("bill found → returns response")
        void billFound_returnsResponse() {
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("bill NOT found → throws ResourceNotFoundException")
        void billNotFound_throws() {
            when(billRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billService.getBillResponseById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Bill not found with ID: 99");
        }
    }

    @Nested
    @DisplayName("getAllBills()")
    class GetAllBills {

        @Test
        @DisplayName("returns mapped list of all bills")
        void returnsAllBills() {
            when(billRepository.findAll()).thenReturn(List.of(openBill));

            List<BillResponse> result = billService.getAllBills();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no bills exist")
        void returnsEmptyList() {
            when(billRepository.findAll()).thenReturn(Collections.emptyList());

            List<BillResponse> result = billService.getAllBills();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBillsByStatus()")
    class GetBillsByStatus {

        @Test
        @DisplayName("returns bills filtered by status")
        void returnsBillsByStatus() {
            when(billRepository.findByStatus(BillStatus.OPEN)).thenReturn(List.of(openBill));

            List<BillResponse> result = billService.getBillsByStatus(BillStatus.OPEN);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("closeBill()")
    class CloseBill {

        @Test
        @DisplayName("bill already CLOSED → throws InvalidOperationException")
        void alreadyClosed_throws() {
            when(billRepository.findById(3L)).thenReturn(Optional.of(closedBill));

            assertThatThrownBy(() -> billService.closeBill(3L))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("already closed");
        }

        @Test
        @DisplayName("bill is OPEN (not PAID) → throws InvalidOperationException")
        void notPaid_throws() {
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            assertThatThrownBy(() -> billService.closeBill(1L))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("must be paid before closing");
        }

        @Test
        @DisplayName("bill is PAID → closes bill and sets tables AVAILABLE")
        void paid_closesBillAndFreeTables() {
            RestaurantTable table = new RestaurantTable();
            table.setId(1L);
            table.setStatus(TableStatus.OCCUPIED);

            BillTable billTable = new BillTable();
            billTable.setTable(table);
            paidBill.getBillTables().add(billTable);

            when(billRepository.findById(2L)).thenReturn(Optional.of(paidBill));
            when(billRepository.save(paidBill)).thenReturn(paidBill);

            BillResponse result = billService.closeBill(2L);

            assertThat(result).isNotNull();
            assertThat(paidBill.getStatus()).isEqualTo(BillStatus.CLOSED);
            assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
            verify(tableRepository).save(table);
        }

        @Test
        @DisplayName("bill NOT found → throws ResourceNotFoundException")
        void billNotFound_throws() {
            when(billRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billService.closeBill(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("applyBestDiscount()")
    class ApplyBestDiscount {

        @Test
        @DisplayName("bill not OPEN → throws RuntimeException")
        void billNotOpen_throws() {
            when(billRepository.findById(2L)).thenReturn(Optional.of(paidBill));

            assertThatThrownBy(() -> billService.applyBestDiscount(2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("open bills");
        }

        @Test
        @DisplayName("no applicable discount found → bill unchanged, no apply called")
        void noDiscount_billUnchanged() {
            DiscountService.DiscountCalculationResult noDiscount = DiscountService.DiscountCalculationResult.noDiscount();
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(discountService.calculateBillDiscount(openBill)).thenReturn(noDiscount);

            Bill result = billService.applyBestDiscount(1L);

            assertThat(result).isEqualTo(openBill);
            verify(discountService, never()).applyDiscountToBill(any(), any());
        }

        @Test
        @DisplayName("discount found → applies discount and saves bill")
        void discountFound_appliesAndSaves() {
            DiscountService.DiscountCalculationResult result = DiscountService.DiscountCalculationResult.builder()
                    .discountId(5L)
                    .discountName("Summer Sale")
                    .discountAmount(BigDecimal.TEN)
                    .build();

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(discountService.calculateBillDiscount(openBill)).thenReturn(result);
            when(billRepository.save(openBill)).thenReturn(openBill);

            billService.applyBestDiscount(1L);

            verify(discountService).applyDiscountToBill(openBill, 5L);
            verify(billRepository).save(openBill);
        }
    }

    @Nested
    @DisplayName("applyDiscount()")
    class ApplyDiscount {

        @Test
        @DisplayName("bill not OPEN → throws RuntimeException")
        void billNotOpen_throws() {
            when(billRepository.findById(2L)).thenReturn(Optional.of(paidBill));

            assertThatThrownBy(() -> billService.applyDiscount(2L, 5L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("open bills");
        }

        @Test
        @DisplayName("bill OPEN → applies discount and saves")
        void billOpen_appliesDiscount() {
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(billRepository.save(openBill)).thenReturn(openBill);

            Bill result = billService.applyDiscount(1L, 5L);

            verify(discountService).applyDiscountToBill(openBill, 5L);
            verify(billRepository).save(openBill);
            assertThat(result).isEqualTo(openBill);
        }
    }

    @Nested
    @DisplayName("removeDiscount()")
    class RemoveDiscount {

        @Test
        @DisplayName("bill has no discount → throws RuntimeException")
        void noDiscount_throws() {
            openBill.setDiscount(null);
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            assertThatThrownBy(() -> billService.removeDiscount(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no discount to remove");
        }

        @Test
        @DisplayName("bill has discount, usedCount > 0 → decrements usedCount and clears discount")
        void hasDiscount_usedCountPositive_decrementsAndClears() {
            Discount discount = new Discount();
            discount.setId(5L);
            discount.setUsedCount(3);
            openBill.setDiscount(discount);
            openBill.setDiscountAmount(BigDecimal.TEN);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(billRepository.save(openBill)).thenReturn(openBill);

            Bill result = billService.removeDiscount(1L);

            assertThat(discount.getUsedCount()).isEqualTo(2);
            assertThat(result.getDiscount()).isNull();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("bill has discount, usedCount == 0 → does not decrement below zero")
        void hasDiscount_usedCountZero_doesNotDecrement() {
            Discount discount = new Discount();
            discount.setId(5L);
            discount.setUsedCount(0);
            openBill.setDiscount(discount);
            openBill.setDiscountAmount(BigDecimal.TEN);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(billRepository.save(openBill)).thenReturn(openBill);

            billService.removeDiscount(1L);

            assertThat(discount.getUsedCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("recalculateBill()")
    class RecalculateBill {

        @Test
        @DisplayName("bill with no orders → totalPrice is zero")
        void noOrders_totalIsZero() {
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(billRepository.save(openBill)).thenReturn(openBill);

            Bill result = billService.recalculateBill(1L);

            assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("bill with orders → totalPrice summed from order details")
        void withOrders_totalSummed() {
            Item item = new Item();
            item.setId(1L);

            OrderDetail detail = new OrderDetail();
            detail.setItem(item);
            detail.setPrice(BigDecimal.valueOf(100));
            detail.setQuantity(2);

            Order order = new Order();
            order.setOrderDetails(List.of(detail));
            openBill.setOrders(List.of(order));
            openBill.setDiscountAmount(BigDecimal.ZERO);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(billRepository.save(openBill)).thenReturn(openBill);

            Bill result = billService.recalculateBill(1L);

            assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(result.getFinalPrice()).isEqualByComparingTo(BigDecimal.valueOf(200));
        }

        @Test
        @DisplayName("bill has discount → discount recalculated and applied")
        void withDiscount_discountRecalculated() {
            Discount discount = new Discount();
            discount.setId(5L);
            openBill.setDiscount(discount);
            openBill.setDiscountAmount(BigDecimal.ZERO);

            DiscountService.DiscountCalculationResult calcResult = DiscountService.DiscountCalculationResult.builder()
                    .discountAmount(BigDecimal.TEN)
                    .build();

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(discountService.calculateDiscountAmount(discount, openBill)).thenReturn(calcResult);
            when(billRepository.save(openBill)).thenReturn(openBill);

            billService.recalculateBill(1L);

            assertThat(openBill.getDiscountAmount()).isEqualByComparingTo(BigDecimal.TEN);
            verify(discountService).calculateDiscountAmount(discount, openBill);
        }

        @Test
        @DisplayName("bill has no discount → discount not recalculated")
        void withoutDiscount_discountNotRecalculated() {
            openBill.setDiscount(null);
            openBill.setDiscountAmount(BigDecimal.ZERO);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(billRepository.save(openBill)).thenReturn(openBill);

            billService.recalculateBill(1L);

            verify(discountService, never()).calculateDiscountAmount(any(), any());
        }
    }

    @Nested
    @DisplayName("findBestDiscount()")
    class FindBestDiscount {

        @Test
        @DisplayName("delegates to discountService and returns response")
        void delegates_returnsResponse() {
            DiscountResponse discountResponse = new DiscountResponse();
            discountResponse.setId(5L);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(discountService.findBestDiscount(openBill)).thenReturn(discountResponse);

            DiscountResponse result = billService.findBestDiscount(1L);

            assertThat(result.getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("no discount found → returns null")
        void noDiscount_returnsNull() {
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));
            when(discountService.findBestDiscount(openBill)).thenReturn(null);

            DiscountResponse result = billService.findBestDiscount(1L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("mapToResponse() — field mapping")
    class MapToResponse {

        @Test
        @DisplayName("bill with discount → discount field mapped in response")
        void withDiscount_mappedInResponse() {
            Discount discount = new Discount();
            discount.setId(5L);
            discount.setName("Promo");
            openBill.setDiscount(discount);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result.getDiscount()).isNotNull();
            assertThat(result.getDiscount().getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("bill without discount → discount null in response")
        void withoutDiscount_nullInResponse() {
            openBill.setDiscount(null);
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result.getDiscount()).isNull();
        }

        @Test
        @DisplayName("bill with reservation → reservationId mapped")
        void withReservation_idMapped() {
            Reservation reservation = new Reservation();
            reservation.setId(7L);
            openBill.setReservation(reservation);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result.getReservationId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("bill without reservation → reservationId null")
        void withoutReservation_nullId() {
            openBill.setReservation(null);
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result.getReservationId()).isNull();
        }

        @Test
        @DisplayName("bill with payment → paymentId mapped")
        void withPayment_idMapped() {
            Payment payment = new Payment();
            payment.setId(9L);
            openBill.setPayment(payment);

            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result.getPaymentId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("bill without payment → paymentId null")
        void withoutPayment_nullId() {
            openBill.setPayment(null);
            when(billRepository.findById(1L)).thenReturn(Optional.of(openBill));

            BillResponse result = billService.getBillResponseById(1L);

            assertThat(result.getPaymentId()).isNull();
        }
    }
}