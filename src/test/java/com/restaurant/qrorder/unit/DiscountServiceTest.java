package com.restaurant.qrorder.unit;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
import com.restaurant.qrorder.domain.dto.request.CreateDiscountRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateDiscountRequest;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.DiscountMapper;
import com.restaurant.qrorder.repository.DiscountRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountService Unit Tests")
class DiscountServiceTest {

    @Mock private DiscountRepository discountRepository;
    @Mock private DiscountMapper discountMapper;

    @InjectMocks private DiscountService discountService;

    private Discount discount;
    private DiscountResponse discountResponse;
    private Bill bill;

    @BeforeEach
    void setUp() {
        discount = new Discount();
        discount.setId(1L);
        discount.setName("Test Discount");
        discount.setActive(true);
        discount.setDiscountType(DiscountType.HOLIDAY);
        discount.setValueType(DiscountValueType.PERCENTAGE);
        discount.setValue(BigDecimal.TEN);
        discount.setUsedCount(0);
        discount.setItems(new ArrayList<>());
        discount.setApplyToSpecificItems(false);

        discountResponse = new DiscountResponse();
        discountResponse.setId(1L);
        discountResponse.setName("Test Discount");

        bill = Bill.builder()
                .id(1L)
                .totalPrice(BigDecimal.valueOf(500))
                .partySize(4)
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.valueOf(500))
                .orders(new ArrayList<>())
                .billTables(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("getAllDiscounts()")
    class GetAllDiscounts {

        @Test
        @DisplayName("returns mapped list of all discounts")
        void returnsAllDiscounts() {
            when(discountRepository.findAll()).thenReturn(List.of(discount));
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            List<DiscountResponse> result = discountService.getAllDiscounts();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no discounts")
        void returnsEmptyList() {
            when(discountRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(discountService.getAllDiscounts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getActiveDiscounts()")
    class GetActiveDiscounts {

        @Test
        @DisplayName("returns only active discounts")
        void returnsActiveDiscounts() {
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            List<DiscountResponse> result = discountService.getActiveDiscounts();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getDiscountById()")
    class GetDiscountById {

        @Test
        @DisplayName("discount found → returns response")
        void found_returnsResponse() {
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThat(discountService.getDiscountById(1L).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("discount NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(discountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.getDiscountById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Discount not found with id: 99");
        }
    }

    @Nested
    @DisplayName("getDiscountByCode()")
    class GetDiscountByCode {

        @Test
        @DisplayName("code found → returns response")
        void found_returnsResponse() {
            when(discountRepository.findByCode("PROMO10")).thenReturn(Optional.of(discount));
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThat(discountService.getDiscountByCode("PROMO10")).isNotNull();
        }

        @Test
        @DisplayName("code NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(discountRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.getDiscountByCode("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Discount not found with code: INVALID");
        }
    }

    @Nested
    @DisplayName("validateDiscountCode()")
    class ValidateDiscountCode {

        @BeforeEach
        void setUp() {
            when(discountRepository.findByCode("CODE")).thenReturn(Optional.of(discount));
        }

        @Test
        @DisplayName("code NOT found → throws ResourceNotFoundException")
        void codeNotFound_throws() {
            when(discountRepository.findByCode("BAD")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.validateDiscountCode("BAD", null, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("discount inactive → throws IllegalStateException")
        void inactive_throws() {
            discount.setActive(false);

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("startDate in future → throws")
        void startDateInFuture_throws() {
            discount.setStartDate(LocalDateTime.now().plusDays(1));

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not yet valid");
        }

        @Test
        @DisplayName("startDate null → passes start check")
        void startDateNull_passes() {
            discount.setStartDate(null);
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("endDate in past → throws")
        void endDateInPast_throws() {
            discount.setEndDate(LocalDateTime.now().minusDays(1));

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("endDate null → passes end check")
        void endDateNull_passes() {
            discount.setEndDate(null);
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("usageLimit reached → throws")
        void usageLimitReached_throws() {
            discount.setUsageLimit(5);
            discount.setUsedCount(5);

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("usage limit");
        }

        @Test
        @DisplayName("usageLimit null → passes usage check")
        void usageLimitNull_passes() {
            discount.setUsageLimit(null);
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("orderAmount provided, below minOrderAmount → throws")
        void belowMinOrderAmount_throws() {
            discount.setMinOrderAmount(BigDecimal.valueOf(500));

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", 100.0, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("minimum requirement");
        }

        @Test
        @DisplayName("orderAmount null → min amount check skipped")
        void orderAmountNull_skipsCheck() {
            discount.setMinOrderAmount(BigDecimal.valueOf(500));
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("minOrderAmount null → min amount check skipped")
        void minOrderAmountNull_skipsCheck() {
            discount.setMinOrderAmount(null);
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", 100.0, null));
        }

        @Test
        @DisplayName("partySize below minPartySize → throws")
        void belowMinPartySize_throws() {
            discount.setMinPartySize(5);

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("minimum requirement");
        }

        @Test
        @DisplayName("partySize above maxPartySize → throws")
        void aboveMaxPartySize_throws() {
            discount.setMaxPartySize(10);

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, 15))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("maximum limit");
        }

        @Test
        @DisplayName("partySize null → party size checks skipped")
        void partySizeNull_skipsChecks() {
            discount.setMinPartySize(5);
            discount.setMaxPartySize(10);
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("not applicable on current day → throws")
        void notApplicableDay_throws() {
            String tomorrow = LocalDateTime.now().getDayOfWeek().plus(1).name();
            discount.setApplicableDays(tomorrow);

            assertThatThrownBy(() -> discountService.validateDiscountCode("CODE", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not applicable on");
        }

        @Test
        @DisplayName("applicable days null → day check skipped")
        void applicableDaysNull_skipsCheck() {
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("applicable days empty → day check skipped")
        void applicableDaysEmpty_skipsCheck() {
            discount.setApplicableDays("");
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            assertThatNoException().isThrownBy(() -> discountService.validateDiscountCode("CODE", null, null));
        }

        @Test
        @DisplayName("all checks pass → returns discount response")
        void allPass_returnsResponse() {
            discount.setApplicableDays(null);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            DiscountResponse result = discountService.validateDiscountCode("CODE", null, null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("createDiscount()")
    class CreateDiscount {

        @Test
        @DisplayName("maps request to entity, saves, and returns response")
        void createsAndReturns() {
            CreateDiscountRequest request = new CreateDiscountRequest();
            when(discountMapper.toEntity(request)).thenReturn(discount);
            when(discountRepository.save(discount)).thenReturn(discount);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            DiscountResponse result = discountService.createDiscount(request);

            assertThat(result).isNotNull();
            verify(discountRepository).save(discount);
        }
    }

    @Nested
    @DisplayName("updateDiscount()")
    class UpdateDiscount {

        private UpdateDiscountRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateDiscountRequest();
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
            when(discountRepository.save(discount)).thenReturn(discount);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);
        }

        @Test
        @DisplayName("discount NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(discountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.updateDiscount(99L, new UpdateDiscountRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("name non-null → name updated")
        void nonNullName_updated() {
            request.setName("New Name");
            discountService.updateDiscount(1L, request);
            assertThat(discount.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("name null → name NOT updated")
        void nullName_notUpdated() {
            request.setName(null);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getName()).isEqualTo("Test Discount");
        }

        @Test
        @DisplayName("description non-null → description updated")
        void nonNullDescription_updated() {
            request.setDescription("New desc");
            discountService.updateDiscount(1L, request);
            assertThat(discount.getDescription()).isEqualTo("New desc");
        }

        @Test
        @DisplayName("description null → description NOT updated")
        void nullDescription_notUpdated() {
            discount.setDescription("Original");
            request.setDescription(null);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getDescription()).isEqualTo("Original");
        }

        @Test
        @DisplayName("discountType non-null → type updated")
        void nonNullDiscountType_updated() {
            request.setDiscountType(DiscountType.BILL_TIER);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getDiscountType()).isEqualTo(DiscountType.BILL_TIER);
        }

        @Test
        @DisplayName("value non-null → value updated")
        void nonNullValue_updated() {
            request.setValue(BigDecimal.valueOf(20));
            discountService.updateDiscount(1L, request);
            assertThat(discount.getValue()).isEqualByComparingTo(BigDecimal.valueOf(20));
        }

        @Test
        @DisplayName("minOrderAmount non-null → updated")
        void nonNullMinOrderAmount_updated() {
            request.setMinOrderAmount(BigDecimal.valueOf(100));
            discountService.updateDiscount(1L, request);
            assertThat(discount.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("maxDiscountAmount non-null → updated")
        void nonNullMaxDiscountAmount_updated() {
            request.setMaxDiscountAmount(BigDecimal.valueOf(50));
            discountService.updateDiscount(1L, request);
            assertThat(discount.getMaxDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("startDate non-null → updated")
        void nonNullStartDate_updated() {
            LocalDateTime date = LocalDateTime.now().plusDays(1);
            request.setStartDate(date);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getStartDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("endDate non-null → updated")
        void nonNullEndDate_updated() {
            LocalDateTime date = LocalDateTime.now().plusDays(30);
            request.setEndDate(date);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getEndDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("usageLimit non-null → updated")
        void nonNullUsageLimit_updated() {
            request.setUsageLimit(100);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getUsageLimit()).isEqualTo(100);
        }

        @Test
        @DisplayName("minPartySize non-null → updated")
        void nonNullMinPartySize_updated() {
            request.setMinPartySize(3);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getMinPartySize()).isEqualTo(3);
        }

        @Test
        @DisplayName("maxPartySize non-null → updated")
        void nonNullMaxPartySize_updated() {
            request.setMaxPartySize(10);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getMaxPartySize()).isEqualTo(10);
        }

        @Test
        @DisplayName("tierConfig non-null → updated")
        void nonNullTierConfig_updated() {
            request.setTierConfig("500000:5,1000000:10");
            discountService.updateDiscount(1L, request);
            assertThat(discount.getTierConfig()).isEqualTo("500000:5,1000000:10");
        }

        @Test
        @DisplayName("applicableDays non-null → updated")
        void nonNullApplicableDays_updated() {
            request.setApplicableDays("MONDAY,FRIDAY");
            discountService.updateDiscount(1L, request);
            assertThat(discount.getApplicableDays()).isEqualTo("MONDAY,FRIDAY");
        }

        @Test
        @DisplayName("applyToSpecificItems non-null → updated")
        void nonNullApplyToSpecificItems_updated() {
            request.setApplyToSpecificItems(true);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getApplyToSpecificItems()).isTrue();
        }

        @Test
        @DisplayName("active non-null → updated")
        void nonNullActive_updated() {
            request.setActive(false);
            discountService.updateDiscount(1L, request);
            assertThat(discount.getActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteDiscount()")
    class DeleteDiscount {

        @Test
        @DisplayName("discount found → deleted")
        void found_deleted() {
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));

            discountService.deleteDiscount(1L);

            verify(discountRepository).delete(discount);
        }

        @Test
        @DisplayName("discount NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(discountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.deleteDiscount(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleDiscountStatus()")
    class ToggleDiscountStatus {

        @Test
        @DisplayName("active discount → toggled to inactive")
        void active_toggledToInactive() {
            discount.setActive(true);
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
            when(discountRepository.save(discount)).thenReturn(discount);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            discountService.toggleDiscountStatus(1L);

            assertThat(discount.getActive()).isFalse();
        }

        @Test
        @DisplayName("inactive discount → toggled to active")
        void inactive_toggledToActive() {
            discount.setActive(false);
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
            when(discountRepository.save(discount)).thenReturn(discount);
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            discountService.toggleDiscountStatus(1L);

            assertThat(discount.getActive()).isTrue();
        }

        @Test
        @DisplayName("discount NOT found → throws ResourceNotFoundException")
        void notFound_throws() {
            when(discountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.toggleDiscountStatus(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("calculateBillDiscount()")
    class CalculateBillDiscount {

        @Test
        @DisplayName("no active discounts → returns noDiscount result")
        void noActiveDiscounts_returnsNoDiscount() {
            when(discountRepository.findActiveDiscounts(any())).thenReturn(Collections.emptyList());

            DiscountService.DiscountCalculationResult result = discountService.calculateBillDiscount(bill);

            assertThat(result.getDiscountId()).isNull();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("applicable discount found → returns best discount result")
        void applicableDiscount_returnsBestResult() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setMinOrderAmount(BigDecimal.valueOf(100));
            discount.setTierConfig(null);

            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            DiscountService.DiscountCalculationResult result = discountService.calculateBillDiscount(bill);

            assertThat(result.getDiscountId()).isEqualTo(1L);
            assertThat(result.getDiscountAmount()).isNotNull();
        }
    }

    @Nested
    @DisplayName("calculateDiscountAmount() — switch branches")
    class CalculateDiscountAmountSwitch {

        @Test
        @DisplayName("HOLIDAY → calculates percentage discount on total")
        void holidayType_percentageOnTotal() {
            discount.setDiscountType(DiscountType.HOLIDAY);
            discount.setValue(BigDecimal.TEN);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("PARTY_SIZE → calculates percentage discount on total")
        void partySizeType_percentageOnTotal() {
            discount.setDiscountType(DiscountType.PARTY_SIZE);
            discount.setValue(BigDecimal.TEN);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("BILL_TIER with null tierConfig → falls back to simple percentage")
        void billTierNullConfig_fallsBackToPercentage() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setTierConfig(null);
            discount.setValue(BigDecimal.TEN);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("BILL_TIER with empty tierConfig → falls back to simple percentage")
        void billTierEmptyConfig_fallsBackToPercentage() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setTierConfig("");
            discount.setValue(BigDecimal.TEN);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("BILL_TIER with valid tierConfig, total meets tier → applies tier discount")
        void billTierValidConfig_meetsTier() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setTierConfig("100:5,400:15");
            discount.setValue(BigDecimal.ZERO);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        }

        @Test
        @DisplayName("BILL_TIER with valid tierConfig, total below all tiers → zero discount")
        void billTierValidConfig_belowAllTiers() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setTierConfig("1000:10,2000:20");
            discount.setValue(BigDecimal.ZERO);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("maxDiscountAmount cap applied → result capped")
        void maxDiscountAmountCap_applied() {
            discount.setDiscountType(DiscountType.HOLIDAY);
            discount.setValue(BigDecimal.valueOf(50));
            discount.setMaxDiscountAmount(BigDecimal.valueOf(30));

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));
        }

        @Test
        @DisplayName("maxDiscountAmount null → no cap applied")
        void maxDiscountAmountNull_noCap() {
            discount.setDiscountType(DiscountType.HOLIDAY);
            discount.setValue(BigDecimal.TEN);
            discount.setMaxDiscountAmount(null);

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("ITEM_SPECIFIC PERCENTAGE → discounts matching items")
        void itemSpecific_percentage() {
            Item item = new Item();
            item.setId(10L);
            discount.setDiscountType(DiscountType.ITEM_SPECIFIC);
            discount.setValueType(DiscountValueType.PERCENTAGE);
            discount.setValue(BigDecimal.TEN);
            discount.setItems(List.of(item));

            OrderDetail detail = new OrderDetail();
            detail.setItem(item);
            detail.setPrice(BigDecimal.valueOf(100));
            detail.setQuantity(2);

            Order order = new Order();
            order.setOrderDetails(List.of(detail));
            bill.setOrders(List.of(order));

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        }

        @Test
        @DisplayName("ITEM_SPECIFIC FIXED_AMOUNT → discounts matching items by fixed amount per unit")
        void itemSpecific_fixedAmount() {
            Item item = new Item();
            item.setId(10L);
            discount.setDiscountType(DiscountType.ITEM_SPECIFIC);
            discount.setValueType(DiscountValueType.FIXED_AMOUNT);
            discount.setValue(BigDecimal.valueOf(15));
            discount.setItems(List.of(item));

            OrderDetail detail = new OrderDetail();
            detail.setItem(item);
            detail.setPrice(BigDecimal.valueOf(100));
            detail.setQuantity(3);

            Order order = new Order();
            order.setOrderDetails(List.of(detail));
            bill.setOrders(List.of(order));

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(45));
        }

        @Test
        @DisplayName("ITEM_SPECIFIC item not in discount list → zero discount")
        void itemSpecific_itemNotInList_zeroDiscount() {
            Item discountItem = new Item();
            discountItem.setId(10L);
            discount.setDiscountType(DiscountType.ITEM_SPECIFIC);
            discount.setItems(List.of(discountItem));

            Item otherItem = new Item();
            otherItem.setId(99L);
            OrderDetail detail = new OrderDetail();
            detail.setItem(otherItem);
            detail.setPrice(BigDecimal.valueOf(100));
            detail.setQuantity(1);

            Order order = new Order();
            order.setOrderDetails(List.of(detail));
            bill.setOrders(List.of(order));

            DiscountService.DiscountCalculationResult result = discountService.calculateDiscountAmount(discount, bill);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("applyDiscountToBill()")
    class ApplyDiscountToBill {

        @Test
        @DisplayName("discount NOT found → throws ResourceNotFoundException")
        void discountNotFound_throws() {
            when(discountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> discountService.applyDiscountToBill(bill, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("discount inactive → throws IllegalStateException")
        void discountInactive_throws() {
            discount.setActive(false);
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));

            assertThatThrownBy(() -> discountService.applyDiscountToBill(bill, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("discount active → applied to bill, usedCount incremented")
        void discountActive_appliedAndIncremented() {
            discount.setDiscountType(DiscountType.HOLIDAY);
            discount.setValue(BigDecimal.TEN);
            discount.setUsedCount(2);
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
            when(discountRepository.save(discount)).thenReturn(discount);

            discountService.applyDiscountToBill(bill, 1L);

            assertThat(bill.getDiscount()).isEqualTo(discount);
            assertThat(discount.getUsedCount()).isEqualTo(3);
            assertThat(bill.getDiscountAmount()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findBestDiscount()")
    class FindBestDiscount {

        @Test
        @DisplayName("no applicable discount → returns null")
        void noApplicableDiscount_returnsNull() {
            when(discountRepository.findActiveDiscounts(any())).thenReturn(Collections.emptyList());

            DiscountResponse result = discountService.findBestDiscount(bill);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("discount found but not in repo → returns null")
        void discountIdNotInRepo_returnsNull() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setMinOrderAmount(BigDecimal.valueOf(100));
            discount.setTierConfig(null);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));
            when(discountRepository.findById(1L)).thenReturn(Optional.empty());

            DiscountResponse result = discountService.findBestDiscount(bill);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("discount found and in repo → returns response with calculatedAmount")
        void discountFoundInRepo_returnsResponse() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setMinOrderAmount(BigDecimal.valueOf(100));
            discount.setTierConfig(null);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));
            when(discountRepository.findById(1L)).thenReturn(Optional.of(discount));
            when(discountMapper.toResponse(discount)).thenReturn(discountResponse);

            DiscountResponse result = discountService.findBestDiscount(bill);

            assertThat(result).isNotNull();
            verify(discountMapper).toResponse(discount);
        }
    }

    @Nested
    @DisplayName("isApplicableDay()")
    class IsApplicableDay {

        @Test
        @DisplayName("applicableDays contains today → applicable")
        void containsToday_applicable() {
            String today = LocalDateTime.now().getDayOfWeek().name();
            discount.setApplicableDays(today);
            discount.setDiscountType(DiscountType.HOLIDAY);
            discount.setMinOrderAmount(null);

            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            DiscountService.DiscountCalculationResult result = discountService.calculateBillDiscount(bill);

            assertThat(result.getDiscountId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("applicableDays does not contain today → not applicable")
        void notContainsToday_notApplicable() {
            String tomorrow = LocalDateTime.now().getDayOfWeek().plus(1).name();
            discount.setApplicableDays(tomorrow);
            discount.setDiscountType(DiscountType.HOLIDAY);

            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            DiscountService.DiscountCalculationResult result = discountService.calculateBillDiscount(bill);

            assertThat(result.getDiscountId()).isNull();
        }
    }

    @Nested
    @DisplayName("isDiscountApplicable() — branch coverage")
    class IsDiscountApplicable {

        @Test
        @DisplayName("startDate in future → not applicable")
        void startDateInFuture_notApplicable() {
            discount.setStartDate(LocalDateTime.now().plusDays(1));
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("endDate in past → not applicable")
        void endDateInPast_notApplicable() {
            discount.setEndDate(LocalDateTime.now().minusDays(1));
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("usageLimit reached → not applicable")
        void usageLimitReached_notApplicable() {
            discount.setUsageLimit(5);
            discount.setUsedCount(5);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("bill below minOrderAmount → not applicable")
        void belowMinOrderAmount_notApplicable() {
            discount.setMinOrderAmount(BigDecimal.valueOf(1000));
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("partySize below minPartySize → not applicable")
        void belowMinPartySize_notApplicable() {
            discount.setMinPartySize(10);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("partySize above maxPartySize → not applicable")
        void aboveMaxPartySize_notApplicable() {
            discount.setMaxPartySize(2);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("bill partySize null with minPartySize set → not applicable")
        void billPartySizeNull_minSet_notApplicable() {
            bill.setPartySize(null);
            discount.setMinPartySize(2);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("bill partySize null with maxPartySize set → not applicable")
        void billPartySizeNull_maxSet_notApplicable() {
            bill.setPartySize(null);
            discount.setMaxPartySize(10);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("ITEM_SPECIFIC applyToSpecificItems=false → applicable to all")
        void itemSpecific_notSpecific_applicable() {
            discount.setDiscountType(DiscountType.ITEM_SPECIFIC);
            discount.setApplyToSpecificItems(false);
            discount.setApplicableDays(null);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("ITEM_SPECIFIC applyToSpecificItems=true, no matching items → not applicable")
        void itemSpecific_noMatchingItems_notApplicable() {
            Item discountItem = new Item();
            discountItem.setId(99L);
            discount.setDiscountType(DiscountType.ITEM_SPECIFIC);
            discount.setApplyToSpecificItems(true);
            discount.setItems(List.of(discountItem));
            discount.setApplicableDays(null);

            bill.setOrders(Collections.emptyList());
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("PARTY_SIZE with null partySize → not applicable")
        void partySizeType_nullPartySize_notApplicable() {
            bill.setPartySize(null);
            discount.setDiscountType(DiscountType.PARTY_SIZE);
            discount.setApplicableDays(null);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isNull();
        }

        @Test
        @DisplayName("BILL_TIER with null minOrderAmount → applicable (treated as zero)")
        void billTier_nullMinOrderAmount_applicable() {
            discount.setDiscountType(DiscountType.BILL_TIER);
            discount.setMinOrderAmount(null);
            discount.setApplicableDays(null);
            when(discountRepository.findActiveDiscounts(any())).thenReturn(List.of(discount));

            assertThat(discountService.calculateBillDiscount(bill).getDiscountId()).isEqualTo(1L);
        }
    }
}