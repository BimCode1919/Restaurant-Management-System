package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.common.OrderType;
import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateBillRequest;
import com.restaurant.qrorder.domain.dto.request.SplitBillRequest;
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
import java.util.*;
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

    @Transactional
    public BillResponse createBill(CreateBillRequest request) {
        log.info("Creating new bill for {} tables, party size: {}", request.getTableIds().size(), request.getPartySize());

        List<RestaurantTable> tables = request.getTableIds().stream()
                .map(id -> tableRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Table not found with ID: " + id)))
                .collect(Collectors.toList());

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

        if (request.getReservationId() != null) {
            Reservation reservation = reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
            bill.setReservation(reservation);
        }

        Bill savedBill = billRepository.save(bill);

        for (RestaurantTable table : tables) {
            BillTable billTable = BillTable.builder()
                    .id(new BillTable.BillTableId(savedBill.getId(), table.getId()))
                    .bill(savedBill)
                    .table(table)
                    .build();
            savedBill.getBillTables().add(billTable);
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        billRepository.save(savedBill);
        log.info("Bill created successfully with ID: {}", savedBill.getId());
        return mapToResponse(savedBill);
    }

    @Transactional(readOnly = true)
    public BillResponse getBillResponseById(Long billId) {
        return mapToResponse(getBillById(billId));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills() {
        return billRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByStatus(BillStatus status) {
        return billRepository.findByStatus(status).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

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

        bill.getBillTables().forEach(billTable -> {
            RestaurantTable table = billTable.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            tableRepository.save(table);
        });

        log.info("Bill {} closed successfully", billId);
        return mapToResponse(billRepository.save(bill));
    }

    public Bill getBillById(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));
    }

    @Transactional
    public BillResponse applyBestDiscount(Long billId) {
        Bill bill = getBillById(billId);

        if (bill.getStatus() != BillStatus.OPEN) {
            throw new InvalidOperationException("Can only apply discount to open bills");
        }

        DiscountResponse discountResponse = discountService.findBestDiscount(bill);
        discountService.applyDiscountToBill(bill, discountResponse.getId());
        Bill savedBill = billRepository.save(bill);

        if (discountResponse.getId() != null) {
            log.info("Applied best discount [{}] to bill [{}]: discountAmount={}, finalPrice={}",
                    discountResponse.getName(), billId,
                    discountResponse.getMaxDiscountAmount(), discountResponse.getCalculatedAmount());
        } else {
            log.info("No applicable discount found for bill [{}]", billId);
        }

        return mapToResponse(savedBill);
    }

    @Transactional
    public BillResponse applyDiscount(Long billId, Long discountId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.OPEN) {
            throw new RuntimeException("Can only apply discount to open bills");
        }
        discountService.applyDiscountToBill(bill, discountId);
        Bill savedBill = billRepository.save(bill);
        log.info("Applied discount {} to bill {}", discountId, billId);
        return mapToResponse(savedBill);
    }

    @Transactional
    public BillResponse removeDiscount(Long billId) {
        Bill bill = getBillById(billId);
        if (bill.getDiscount() == null) {
            throw new RuntimeException("Bill has no discount to remove");
        }
        Discount discount = bill.getDiscount();
        if (discount.getUsedCount() > 0) {
            discount.setUsedCount(discount.getUsedCount() - 1);
        }
        bill.setDiscount(null);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setFinalPrice(bill.getTotalPrice());
        log.info("Removed discount from bill {}", billId);
        return mapToResponse(billRepository.save(bill));
    }

    @Transactional
    public Bill recalculateBill(Long billId) {
        Bill bill = getBillById(billId);

        BigDecimal totalPrice = bill.getOrders().stream()
                .flatMap(order -> order.getOrderDetails().stream())
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        bill.setTotalPrice(totalPrice);

        if (bill.getDiscount() != null) {
            DiscountService.DiscountCalculationResult result =
                    discountService.calculateDiscountAmount(bill.getDiscount(), bill);
            bill.setDiscountAmount(result.getDiscountAmount());
        }

        if (bill.getReservation() != null && bill.getReservation().getDepositAmount() != null) {
            BigDecimal afterDeposit = bill.getTotalPrice().subtract(bill.getReservation().getDepositAmount());
            bill.setTotalPrice(afterDeposit.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : afterDeposit);
        }

        bill.setFinalPrice(bill.getTotalPrice().subtract(bill.getDiscountAmount()));

        Bill savedBill = billRepository.save(bill);
        log.info("Recalculated bill {}: Total={}, Discount={}, Final={}",
                billId, bill.getTotalPrice(), bill.getDiscountAmount(), bill.getFinalPrice());
        return savedBill;
    }

    public DiscountResponse findBestDiscount(Long billId) {
        return discountService.findBestDiscount(getBillById(billId));
    }

    @Transactional
    public List<BillResponse> unmergeBill(Long billId) {
        log.info("Unmerging bill {}", billId);

        if (billId == null) {
            throw new InvalidOperationException("Bill ID must not be null");
        }

        Bill mergedBill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));

        if (mergedBill.getStatus() == BillStatus.CLOSED || mergedBill.getStatus() == BillStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot unmerge a closed or cancelled bill");
        }

        if (mergedBill.getPreviousBillIds().isEmpty()) {
            throw new InvalidOperationException("Bill " + billId + " has no merge history to unmerge");
        }

        List<Bill> extractedBills = new ArrayList<>();

        for (Long previousId : mergedBill.getPreviousBillIds()) {
            Bill previousBill = billRepository.findById(previousId)
                    .orElseThrow(() -> new ResourceNotFoundException("Previous bill not found with ID: " + previousId));

            List<Order> ordersToMove = mergedBill.getOrders().stream()
                    .filter(order -> order.getOriginalBillId() != null
                            && order.getOriginalBillId().equals(previousId))
                    .collect(Collectors.toList());

            ordersToMove.forEach(order -> {
                order.setBill(previousBill);
                previousBill.getOrders().add(order);
                mergedBill.getOrders().remove(order);
            });

            mergedBill.getBillTables().stream()
                    .filter(bt -> previousId.equals(bt.getOriginalBillId()))
                    .forEach(bt -> {
                        BillTable restored = BillTable.builder()
                                .id(new BillTable.BillTableId(previousBill.getId(), bt.getTable().getId()))
                                .bill(previousBill)
                                .table(bt.getTable())
                                .build();
                        previousBill.getBillTables().add(restored);
                    });

            BigDecimal total = previousBill.getOrders().stream()
                    .flatMap(o -> o.getOrderDetails().stream())
                    .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            previousBill.setTotalPrice(total);
            previousBill.setFinalPrice(total);
            previousBill.setDiscountAmount(BigDecimal.ZERO);
            previousBill.setStatus(BillStatus.OPEN);

            extractedBills.add(billRepository.save(previousBill));
            log.info("Restored previous bill {} with {} tables", previousId, previousBill.getBillTables().size());
        }

        log.info("Unmerge complete — bill {} split back into bills {}", billId, mergedBill.getPreviousBillIds());
        billRepository.delete(mergedBill);

        return extractedBills.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public BillResponse mergeBills(List<Long> billIds) {
        log.info("Merging bills {} into a new bill", billIds);

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

        for (Bill source : sourceBills) {
            if (source.getPreviousBillIds() != null && !source.getPreviousBillIds().isEmpty()) {
                savedMergedBill.getPreviousBillIds().addAll(source.getPreviousBillIds());
            } else {
                savedMergedBill.getPreviousBillIds().add(source.getId());
            }
        }

        Map<RestaurantTable, Long> tableToOriginalBillId = new LinkedHashMap<>();

        for (Bill source : sourceBills) {
            source.getOrders().forEach(order -> {
                if (order.getOriginalBillId() == null) {
                    order.setOriginalBillId(source.getId());
                }
                order.setBill(savedMergedBill);
                savedMergedBill.getOrders().add(order);
            });

            source.getBillTables().forEach(bt -> {
                Long originalId = bt.getOriginalBillId() != null ? bt.getOriginalBillId() : source.getId();
                tableToOriginalBillId.put(bt.getTable(), originalId);
            });
        }

        for (Bill source : sourceBills) {
            source.setStatus(BillStatus.MERGED);
            source.setClosedAt(LocalDateTime.now());
            source.getOrders().clear();
            source.getBillTables().clear();
            billRepository.save(source);
            log.info("Bill {} marked as MERGED", source.getId());
        }

        billRepository.flush();

        for (Map.Entry<RestaurantTable, Long> entry : tableToOriginalBillId.entrySet()) {
            BillTable newBt = BillTable.builder()
                    .id(new BillTable.BillTableId(savedMergedBill.getId(), entry.getKey().getId()))
                    .bill(savedMergedBill)
                    .table(entry.getKey())
                    .originalBillId(entry.getValue())
                    .build();
            savedMergedBill.getBillTables().add(newBt);
        }

        BigDecimal mergedTotal = savedMergedBill.getOrders().stream()
                .flatMap(order -> order.getOrderDetails().stream())
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        savedMergedBill.setTotalPrice(mergedTotal);

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

    @Transactional
    public List<BillResponse> splitBill(SplitBillRequest request) {
        log.info("Splitting {} items off bill {}", request.getOrderDetailIds().size(), request.getBillId());

        Bill originalBill = getBillById(request.getBillId());

        if (originalBill.getStatus() != BillStatus.OPEN) {
            throw new InvalidOperationException("Can only split OPEN bills");
        }

        if (originalBill.getOrders().isEmpty()) {
            throw new InvalidOperationException("Bill has no orders to split");
        }

        Map<Long, OrderDetail> allDetails = originalBill.getOrders().stream()
                .flatMap(o -> o.getOrderDetails().stream())
                .collect(Collectors.toMap(OrderDetail::getId, d -> d));

        for (SplitBillRequest.SplitItem splitItem : request.getOrderDetailIds()) {
            OrderDetail detail = allDetails.get(splitItem.getOrderDetailId());
            if (detail == null) {
                throw new InvalidOperationException(
                        "OrderDetail ID " + splitItem.getOrderDetailId() + " does not belong to bill " + request.getBillId());
            }
            if (detail.getItemStatus() == ItemStatus.CANCELLED || detail.getItemStatus() == ItemStatus.SERVED) {
                throw new InvalidOperationException(
                        "Cannot split item with status " + detail.getItemStatus() + ": ID " + splitItem.getOrderDetailId());
            }
            if (splitItem.getQuantity() > detail.getQuantity()) {
                throw new InvalidOperationException(
                        "Cannot split " + splitItem.getQuantity() + " of item " + splitItem.getOrderDetailId()
                                + " — only " + detail.getQuantity() + " available");
            }
        }

        List<RestaurantTable> availableTables = tableRepository.findByStatus(TableStatus.AVAILABLE);
        if (availableTables.isEmpty()) {
            throw new InvalidOperationException("No available tables to assign to the new split bill");
        }
        RestaurantTable assignedTable = availableTables.get(0);

        Bill newBill = Bill.builder()
                .totalPrice(BigDecimal.ZERO)
                .partySize(1)
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .build();

        Bill savedNewBill = billRepository.save(newBill);

        BillTable billTable = BillTable.builder()
                .id(new BillTable.BillTableId(savedNewBill.getId(), assignedTable.getId()))
                .bill(savedNewBill)
                .table(assignedTable)
                .build();
        savedNewBill.getBillTables().add(billTable);
        assignedTable.setStatus(TableStatus.OCCUPIED);
        tableRepository.save(assignedTable);

        Order newOrder = Order.builder()
                .bill(savedNewBill)
                .orderType(originalBill.getOrders().stream().findFirst()
                        .map(Order::getOrderType).orElse(OrderType.AT_TABLE))
                .createdBy(originalBill.getOrders().stream().findFirst()
                        .map(Order::getCreatedBy).orElse(null))
                .orderDetails(new ArrayList<>())
                .build();

        BigDecimal splitTotal = BigDecimal.ZERO;

        for (SplitBillRequest.SplitItem splitItem : request.getOrderDetailIds()) {
            OrderDetail original = allDetails.get(splitItem.getOrderDetailId());
            int splitQty = splitItem.getQuantity();

            if (splitQty == original.getQuantity()) {
                original.setOrder(newOrder);
                newOrder.getOrderDetails().add(original);
            } else {
                original.setQuantity(original.getQuantity() - splitQty);

                OrderDetail splitDetail = OrderDetail.builder()
                        .order(newOrder)
                        .item(original.getItem())
                        .quantity(splitQty)
                        .price(original.getPrice())
                        .note(original.getNote())
                        .itemStatus(original.getItemStatus())
                        .build();
                newOrder.getOrderDetails().add(splitDetail);
            }

            splitTotal = splitTotal.add(original.getPrice().multiply(BigDecimal.valueOf(splitQty)));
        }

        savedNewBill.getOrders().add(newOrder);
        savedNewBill.setTotalPrice(splitTotal);
        savedNewBill.setFinalPrice(splitTotal);
        billRepository.save(savedNewBill);

        log.info("New bill {} created with {} items on table {}",
                savedNewBill.getId(), request.getOrderDetailIds().size(), assignedTable.getTableNumber());

        recalculateBill(originalBill.getId());

        log.info("Split complete — original bill {} updated, new bill {} created",
                request.getBillId(), savedNewBill.getId());

        return List.of(
                getBillResponseById(originalBill.getId()),
                mapToResponse(savedNewBill)
        );
    }

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