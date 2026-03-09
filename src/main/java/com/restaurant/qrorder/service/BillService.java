package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateBillRequest;
import com.restaurant.qrorder.domain.dto.response.BillResponse;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.dto.response.OrderResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.DiscountMapper;
import com.restaurant.qrorder.repository.BillRepository;
import com.restaurant.qrorder.repository.ReservationRepository;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;
    private final DiscountService discountService;
    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final DiscountMapper discountMapper;

    /**
     * Create new bill
     */
    @Transactional
    public BillResponse createBill(CreateBillRequest request) {
        log.info("Creating new bill for {} tables, party size: {}", request.getTableIds().size(), request.getPartySize());
        
        // Validate and get tables
        List<RestaurantTable> tables = request.getTableIds().stream()
                .map(id -> tableRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id)))
                .collect(Collectors.toList());
        
        // Check if tables are available
        tables.forEach(table -> {
            if (table.getStatus() != TableStatus.AVAILABLE) {
                throw new InvalidOperationException("Table " + table.getTableNumber() + " is not available");
            }
        });
        
        Bill bill = Bill.builder()
                .totalPrice(BigDecimal.ZERO)
                .partySize(request.getPartySize())
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();
        
        // Link to reservation if provided
        if (request.getReservationId() != null) {
            Reservation reservation = reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
            bill.setReservation(reservation);
        }
        
        Bill savedBill = billRepository.save(bill);
        
        // Create bill-table associations and update table status
        for (RestaurantTable table : tables) {
            BillTable.BillTableId id = new BillTable.BillTableId(savedBill.getId(), table.getId());
            BillTable billTable = BillTable.builder()
                    .id(id)
                    .bill(savedBill)
                    .table(table)
                    .build();
            savedBill.getBillTables().add(billTable);
            
            // Update table status to OCCUPIED
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }
        
        billRepository.save(savedBill);
        
        log.info("Bill created successfully with ID: {}", savedBill.getId());
        return mapToResponse(savedBill);
    }

    /**
     * Get bill by ID
     */
    @Transactional(readOnly = true)
    public BillResponse getBillResponseById(Long billId) {
        Bill bill = getBillById(billId);
        return mapToResponse(bill);
    }

    /**
     * Get all bills
     */
    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills() {
        return billRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get bills by status
     */
    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByStatus(BillStatus status) {
        return billRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Close bill
     */
    @Transactional
    public BillResponse closeBill(Long billId) {
        log.info("Closing bill {}", billId);
        
        Bill bill = getBillById(billId);
        
        if (bill.getStatus() == BillStatus.CLOSED) {
            throw new InvalidOperationException("Bill is already closed");
        }
        
        if (bill.getStatus() != BillStatus.PAID) {
            throw new InvalidOperationException("Bill must be paid before closing");
        }
        
        bill.setStatus(BillStatus.CLOSED);
        bill.setClosedAt(LocalDateTime.now());
        
        // Update all tables back to AVAILABLE
        bill.getBillTables().forEach(billTable -> {
            RestaurantTable table = billTable.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            tableRepository.save(table);
        });
        
        Bill savedBill = billRepository.save(bill);
        
        log.info("Bill {} closed successfully", billId);
        return mapToResponse(savedBill);
    }

    /**
     * Get bill by ID (internal use)
     */
    public Bill getBillById(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));
    }

    /**
     * Calculate and apply best discount to bill
     */
    @Transactional
    public BillResponse applyBestDiscount(Long billId) {
        Bill bill = getBillById(billId);

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new InvalidOperationException("Can only apply discount to open bills");
        }

        // ← use the new method that finds AND applies in one call
        DiscountService.DiscountCalculationResult result =
                discountService.applyBestDiscountToBill(bill);

        Bill savedBill = billRepository.save(bill);

        if (result.getDiscountId() != null) {
            log.info("Applied best discount [{}] to bill [{}]: discountAmount={}, finalPrice={}",
                    result.getDiscountName(), billId,
                    result.getDiscountAmount(), result.getFinalAmount());
        } else {
            log.info("No applicable discount found for bill [{}]", billId);
        }

        return mapToResponse(savedBill);
    }

    /**
     * Apply specific discount to bill
     */
    @Transactional
    public Bill applyDiscount(Long billId, Long discountId) {
        Bill bill = getBillById(billId);

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new RuntimeException("Can only apply discount to open bills");
        }

        discountService.applyDiscountToBill(bill, discountId);
        Bill savedBill = billRepository.save(bill);

        log.info("Applied discount {} to bill {}", discountId, billId);

        return savedBill;
    }

    /**
     * Remove discount from bill
     */
    @Transactional
    public Bill removeDiscount(Long billId) {
        Bill bill = getBillById(billId);

        if (bill.getDiscount() == null) {
            throw new RuntimeException("Bill has no discount to remove");
        }

        // Decrement usage count
        Discount discount = bill.getDiscount();
        if (discount.getUsedCount() > 0) {
            discount.setUsedCount(discount.getUsedCount() - 1);
        }

        bill.setDiscount(null);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setFinalPrice(bill.getTotalPrice());

        Bill savedBill = billRepository.save(bill);

        log.info("Removed discount from bill {}", billId);

        return savedBill;
    }

    /**
     * Recalculate bill totals (after adding/removing items)
     */
    @Transactional
    public Bill recalculateBill(Long billId) {
        Bill bill = getBillById(billId);

        // Calculate total price from all orders
        BigDecimal totalPrice = bill.getOrders().stream()
                .flatMap(order -> order.getOrderDetails().stream())
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        bill.setTotalPrice(totalPrice);

        // Recalculate discount if exists
        if (bill.getDiscount() != null) {
            DiscountService.DiscountCalculationResult result = 
                    discountService.calculateDiscountAmount(bill.getDiscount(), bill);
            bill.setDiscountAmount(result.getDiscountAmount());
        }

        // Calculate final price
        bill.setFinalPrice(bill.getTotalPrice().subtract(bill.getDiscountAmount()));

        Bill savedBill = billRepository.save(bill);

        log.info("Recalculated bill {}: Total={}, Discount={}, Final={}",
                billId, bill.getTotalPrice(), bill.getDiscountAmount(), bill.getFinalPrice());

        return savedBill;
    }

    /**
     * Find best discount for bill without applying
     */
    public DiscountResponse findBestDiscount(Long billId) {
        Bill bill = getBillById(billId);
        return discountService.findBestDiscount(bill);
    }

    /**
     * Map Bill entity to BillResponse DTO
     */
    private BillResponse mapToResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .totalPrice(bill.getTotalPrice())
                .partySize(bill.getPartySize())
                .discount(bill.getDiscount() != null ? discountMapper.toResponse(bill.getDiscount()) : null)
                .discountAmount(bill.getDiscountAmount())
                .finalPrice(bill.getFinalPrice())
                .status(bill.getStatus())
                .reservationId(bill.getReservation() != null ? bill.getReservation().getId() : null)
                .paymentId(bill.getPayment() != null ? bill.getPayment().getId() : null)
                .tableNumbers(bill.getTableNumbers())
                .orders(bill.getOrders().stream()
                        .map(this::mapOrderToResponse)
                        .collect(Collectors.toList()))
                .createdAt(bill.getCreatedAt())
                .closedAt(bill.getClosedAt())
                .build();
    }

    /**
     * Merge multiple open bills into a brand new bill.
     * All source bills are marked as MERGED.
     */
    @Transactional
    public BillResponse mergeBills(List<Long> billIds) {
        log.info("Merging bills {} into a new bill", billIds);

        // ── Guards ────────────────────────────────────────────────────────────
        if (billIds == null || billIds.size() < 2) {
            throw new InvalidOperationException("At least two bill IDs are required to merge");
        }
        if (billIds.size() != new HashSet<>(billIds).size()) {
            throw new InvalidOperationException("Duplicate bill IDs found in merge request");
        }


        List<Bill> sourceBills = billIds.stream()
                .map(id -> {
                    Bill bill = getBillById(id);
                    if (bill.getStatus() != BillStatus.OPEN) {
                        throw new InvalidOperationException(
                                "Bill " + id + " must be OPEN to be merged — current status: " + bill.getStatus());
                    }
                    return bill;
                })
                .collect(Collectors.toList());

        // ── Build new bill ────────────────────────────────────────────────────
        int totalPartySize = sourceBills.stream()
                .mapToInt(b -> b.getPartySize() != null ? b.getPartySize() : 0)
                .sum();

        Bill mergedBill = Bill.builder()
                .totalPrice(BigDecimal.ZERO)
                .partySize(totalPartySize)
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        Bill savedMergedBill = billRepository.save(mergedBill);

        // ── Absorb orders and tables from each source bill ────────────────────
        Set<Long> assignedTableIds = new HashSet<>();

        for (Bill source : sourceBills) {

            // Re-parent orders to the new bill
            source.getOrders().forEach(order -> {
                order.setBill(savedMergedBill);
                savedMergedBill.getOrders().add(order);
            });

            // Re-parent tables, skipping any already linked
            source.getBillTables().forEach(bt -> {
                RestaurantTable table = bt.getTable();
                if (assignedTableIds.add(table.getId())) {
                    BillTable.BillTableId newId =
                            new BillTable.BillTableId(savedMergedBill.getId(), table.getId());
                    BillTable newBt = BillTable.builder()
                            .id(newId)
                            .bill(savedMergedBill)
                            .table(table)
                            .build();
                    savedMergedBill.getBillTables().add(newBt);
                }
            });

            // Mark source bill as merged
            source.setStatus(BillStatus.MERGED);
            source.setClosedAt(LocalDateTime.now());
            source.getOrders().clear();
            source.getBillTables().clear();
            billRepository.save(source);

            log.info("Bill {} marked as MERGED", source.getId());
        }

        // ── Calculate totals ──────────────────────────────────────────────────
        BigDecimal mergedTotal = savedMergedBill.getOrders().stream()
                .flatMap(order -> order.getOrderDetails().stream())
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        savedMergedBill.setTotalPrice(mergedTotal);

        // ── Apply best available discount ─────────────────────────────────────
        DiscountService.DiscountCalculationResult best =
                discountService.calculateBillDiscount(savedMergedBill);
        if (best.getDiscountId() != null) {
            discountService.applyDiscountToBill(savedMergedBill, best.getDiscountId());
        }

        savedMergedBill.setFinalPrice(
                savedMergedBill.getTotalPrice().subtract(savedMergedBill.getDiscountAmount()));

        Bill result = billRepository.save(savedMergedBill);

        log.info("Merge complete — new bill {} created from bills {}. Total: {}",
                result.getId(), billIds, result.getTotalPrice());

        return mapToResponse(result);
    }

       private OrderResponse mapOrderToResponse(Order order) {
        BigDecimal totalAmount = order.getOrderDetails().stream()
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return OrderResponse.builder()
                .id(order.getId())
                .billId(order.getBill().getId())
                .orderType(order.getOrderType())
                .createdBy(order.getCreatedBy().getFullName())
                .totalAmount(totalAmount)
                .items(order.getOrderDetails().stream()
                        .map(this::mapOrderDetailToResponse)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailResponse mapOrderDetailToResponse(OrderDetail detail) {
        BigDecimal subtotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        
        return OrderDetailResponse.builder()
                .id(detail.getId())
                .itemId(detail.getItem().getId())
                .itemName(detail.getItem().getName())
                .quantity(detail.getQuantity())
                .price(detail.getPrice())
                .subtotal(subtotal)
                .itemStatus(detail.getItemStatus())
                .notes(detail.getNote())
                .build();
    }
}
