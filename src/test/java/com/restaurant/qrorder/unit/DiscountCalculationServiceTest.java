package com.restaurant.qrorder.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.service.DiscountCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DiscountCalculationService Unit Tests")
class DiscountCalculationServiceTest {

    private DiscountCalculationService service;

    @BeforeEach
    void setUp() {
        service = new DiscountCalculationService();
    }

    private Discount baseDiscount(DiscountType type) {
        Discount d = new Discount();
        d.setActive(true);
        d.setDiscountType(type);
        d.setValueType(DiscountValueType.PERCENTAGE);
        d.setValue(BigDecimal.TEN);
        d.setStartDate(null);
        d.setEndDate(null);
        d.setUsageLimit(null);
        d.setUsedCount(0);
        d.setMinOrderAmount(null);
        d.setMaxDiscountAmount(null);
        d.setApplyToSpecificItems(false);
        return d;
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Nested
    @DisplayName("validateDiscountApplicability()")
    class ValidateDiscountApplicability {

        @Test
        @DisplayName("inactive discount → throws InvalidOperationException")
        void inactive_throws() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setActive(false);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("startDate in future → throws")
        void startDateInFuture_throws() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setStartDate(now().plusDays(1));

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not started");
        }

        @Test
        @DisplayName("startDate in past → passes start check")
        void startDateInPast_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setStartDate(now().minusDays(1));

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("startDate null → start check skipped")
        void startDateNull_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setStartDate(null);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("endDate in past → throws")
        void endDateInPast_throws() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setEndDate(now().minusDays(1));

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("endDate in future → passes end check")
        void endDateInFuture_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setEndDate(now().plusDays(1));

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("endDate null → end check skipped")
        void endDateNull_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setEndDate(null);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("usageLimit reached → throws")
        void usageLimitReached_throws() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setUsageLimit(5);
            d.setUsedCount(5);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("usage limit");
        }

        @Test
        @DisplayName("usageLimit not reached → passes")
        void usageLimitNotReached_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setUsageLimit(5);
            d.setUsedCount(4);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("usageLimit null → usage check skipped")
        void usageLimitNull_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setUsageLimit(null);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("orderTotal below minOrderAmount → throws")
        void belowMinOrderAmount_throws() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setMinOrderAmount(BigDecimal.valueOf(200));

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("at least");
        }

        @Test
        @DisplayName("orderTotal meets minOrderAmount → passes")
        void meetsMinOrderAmount_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setMinOrderAmount(BigDecimal.valueOf(100));

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("minOrderAmount null → min amount check skipped")
        void minOrderAmountNull_passes() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setMinOrderAmount(null);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("HOLIDAY type → delegates to validateHolidayDiscount")
        void holidayType_delegatesToHolidayValidation() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays(null);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("applicable days");
        }

        @Test
        @DisplayName("PARTY_SIZE type → delegates to validatePartySizeDiscount")
        void partySizeType_delegatesToPartySizeValidation() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Party size is required");
        }

        @Test
        @DisplayName("ITEM_SPECIFIC and BILL_TIER types → no extra type validation")
        void otherTypes_noExtraValidation() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()));

            Discount billTier = baseDiscount(DiscountType.BILL_TIER);
            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(billTier, BigDecimal.valueOf(100), null, now()));
        }
    }

    @Nested
    @DisplayName("calculateDiscountAmount()")
    class CalculateDiscountAmount {

        @Test
        @DisplayName("ITEM_SPECIFIC PERCENTAGE → correct percentage discount")
        void itemSpecific_percentage() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        }

        @Test
        @DisplayName("ITEM_SPECIFIC FIXED_AMOUNT → returns fixed value")
        void itemSpecific_fixedAmount() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(DiscountValueType.FIXED_AMOUNT);
            d.setValue(BigDecimal.valueOf(30));

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(30));
        }

        @Test
        @DisplayName("ITEM_SPECIFIC FIXED_PRICE → returns orderTotal minus value")
        void itemSpecific_fixedPrice() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(DiscountValueType.FIXED_PRICE);
            d.setValue(BigDecimal.valueOf(150));

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("ITEM_SPECIFIC null valueType → throws NullPointerException (switch cannot handle null)")
        void itemSpecific_nullValueType_throws() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(null);

            assertThatThrownBy(() -> service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("HOLIDAY on applicable day → calculates discount")
        void holiday_applicableDay() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            String today = now().getDayOfWeek().name();
            d.setApplicableDays(today);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(100), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.TEN);
        }

        @Test
        @DisplayName("HOLIDAY on non-applicable day → throws")
        void holiday_nonApplicableDay_throws() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            String tomorrow = now().getDayOfWeek().plus(1).name();
            d.setApplicableDays(tomorrow);

            assertThatThrownBy(() -> service.calculateDiscountAmount(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not applicable on this day");
        }

        @Test
        @DisplayName("PARTY_SIZE with valid party → calculates discount")
        void partySize_valid() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMinPartySize(4);
            d.setMaxPartySize(10);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), 5, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        }

        @Test
        @DisplayName("BILL_TIER with matching tier → calculates percentage discount")
        void billTier_percentageTier() throws Exception {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig("{\"tier1\":{\"min\":100,\"discount\":10}}");

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        }

        @Test
        @DisplayName("discount capped by maxDiscountAmount → returns cap")
        void cappedByMaxDiscountAmount() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.valueOf(50));
            d.setMaxDiscountAmount(BigDecimal.valueOf(30));

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(30));
        }

        @Test
        @DisplayName("discount below maxDiscountAmount → returns calculated amount")
        void belowMaxDiscountAmount_returnsFull() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);
            d.setMaxDiscountAmount(BigDecimal.valueOf(50));

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        }

        @Test
        @DisplayName("maxDiscountAmount null → no cap applied")
        void maxDiscountAmountNull_noCapApplied() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.valueOf(50));
            d.setMaxDiscountAmount(null);

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        }
    }

    @Nested
    @DisplayName("validateHolidayDiscount() (via calculateDiscountAmount)")
    class ValidateHolidayDiscount {

        @Test
        @DisplayName("applicableDays null → throws")
        void nullApplicableDays_throws() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays(null);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("applicable days");
        }

        @Test
        @DisplayName("applicableDays empty string → throws")
        void emptyApplicableDays_throws() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays("");

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("applicable days");
        }

        @Test
        @DisplayName("applicableDays matches specific date (yyyy-MM-dd) → passes")
        void specificDateMatch_passes() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            String today = now().toLocalDate().toString();
            d.setApplicableDays(today);
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);

            assertThatNoException().isThrownBy(() ->
                    service.calculateDiscountAmount(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("applicableDays specific date does NOT match today → throws")
        void specificDateNoMatch_throws() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays("2000-01-01");

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not applicable on this day");
        }

        @Test
        @DisplayName("applicableDays contains day-of-week matching today → passes")
        void dayOfWeekMatch_passes() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays(now().getDayOfWeek().name());
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);

            assertThatNoException().isThrownBy(() ->
                    service.calculateDiscountAmount(d, BigDecimal.valueOf(100), null, now()));
        }

        @Test
        @DisplayName("applicableDays contains invalid string (not date, not day) → treated as false → throws")
        void invalidDayString_throws() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays("FUNDAY");

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("not applicable on this day");
        }

        @Test
        @DisplayName("applicableDays has invalid entry then valid day-of-week → passes")
        void invalidThenValidDay_passes() {
            Discount d = baseDiscount(DiscountType.HOLIDAY);
            d.setApplicableDays("FUNDAY," + now().getDayOfWeek().name());
            d.setValueType(DiscountValueType.PERCENTAGE);
            d.setValue(BigDecimal.TEN);

            assertThatNoException().isThrownBy(() ->
                    service.calculateDiscountAmount(d, BigDecimal.valueOf(100), null, now()));
        }
    }

    @Nested
    @DisplayName("validatePartySizeDiscount()")
    class ValidatePartySizeDiscount {

        @Test
        @DisplayName("partySize null → throws")
        void nullPartySize_throws() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Party size is required");
        }

        @Test
        @DisplayName("partySize below minPartySize → throws")
        void belowMinPartySize_throws() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMinPartySize(5);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), 3, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Minimum party size is 5");
        }

        @Test
        @DisplayName("partySize meets minPartySize → passes")
        void meetsMinPartySize_passes() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMinPartySize(5);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), 5, now()));
        }

        @Test
        @DisplayName("minPartySize null → min check skipped")
        void nullMinPartySize_passes() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMinPartySize(null);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), 2, now()));
        }

        @Test
        @DisplayName("partySize above maxPartySize → throws")
        void aboveMaxPartySize_throws() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMaxPartySize(10);

            assertThatThrownBy(() -> service.validateDiscountApplicability(d, BigDecimal.valueOf(100), 11, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Maximum party size is 10");
        }

        @Test
        @DisplayName("partySize meets maxPartySize → passes")
        void meetsMaxPartySize_passes() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMaxPartySize(10);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), 10, now()));
        }

        @Test
        @DisplayName("maxPartySize null → max check skipped")
        void nullMaxPartySize_passes() {
            Discount d = baseDiscount(DiscountType.PARTY_SIZE);
            d.setMaxPartySize(null);

            assertThatNoException().isThrownBy(() ->
                    service.validateDiscountApplicability(d, BigDecimal.valueOf(100), 50, now()));
        }
    }

    @Nested
    @DisplayName("calculateBillTierDiscount()")
    class CalculateBillTierDiscount {

        @Test
        @DisplayName("tierConfig null → throws")
        void nullTierConfig_throws() {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig(null);

            assertThatThrownBy(() -> service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("tier configuration");
        }

        @Test
        @DisplayName("tierConfig empty → throws")
        void emptyTierConfig_throws() {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig("");

            assertThatThrownBy(() -> service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("tier configuration");
        }

        @Test
        @DisplayName("tierConfig invalid JSON → throws InvalidOperationException")
        void invalidJson_throws() {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig("NOT_JSON");

            assertThatThrownBy(() -> service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now()))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Invalid tier configuration");
        }

        @Test
        @DisplayName("orderTotal below all tier minimums → returns zero")
        void belowAllTiers_returnsZero() {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig("{\"tier1\":{\"min\":500,\"discount\":10}}");

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(100), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("discount value > 100 → treated as fixed amount")
        void discountAbove100_treatedAsFixed() {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig("{\"tier1\":{\"min\":100,\"discount\":150}}");

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(200), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(150));
        }

        @Test
        @DisplayName("multiple tiers, highest applicable discount wins")
        void multipleTiers_highestDiscountWins() {
            Discount d = baseDiscount(DiscountType.BILL_TIER);
            d.setTierConfig("{\"tier1\":{\"min\":100,\"discount\":5},\"tier2\":{\"min\":200,\"discount\":15}}");

            BigDecimal result = service.calculateDiscountAmount(d, BigDecimal.valueOf(300), null, now());

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(45.00));
        }
    }

    @Nested
    @DisplayName("isDiscountApplicableToItem()")
    class IsDiscountApplicableToItem {

        @Test
        @DisplayName("applyToSpecificItems=false → always returns true")
        void notSpecificItems_returnsTrue() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setApplyToSpecificItems(false);

            assertThat(service.isDiscountApplicableToItem(d, 99L)).isTrue();
        }

        @Test
        @DisplayName("applyToSpecificItems=true, item in list → returns true")
        void specificItems_itemFound_returnsTrue() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setApplyToSpecificItems(true);
            Item item = new Item();
            item.setId(5L);
            d.setItems(List.of(item));

            assertThat(service.isDiscountApplicableToItem(d, 5L)).isTrue();
        }

        @Test
        @DisplayName("applyToSpecificItems=true, item NOT in list → returns false")
        void specificItems_itemNotFound_returnsFalse() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setApplyToSpecificItems(true);
            Item item = new Item();
            item.setId(5L);
            d.setItems(List.of(item));

            assertThat(service.isDiscountApplicableToItem(d, 99L)).isFalse();
        }

        @Test
        @DisplayName("applyToSpecificItems=true, empty item list → returns false")
        void specificItems_emptyList_returnsFalse() {
            Discount d = baseDiscount(DiscountType.ITEM_SPECIFIC);
            d.setApplyToSpecificItems(true);
            d.setItems(List.of());

            assertThat(service.isDiscountApplicableToItem(d, 1L)).isFalse();
        }
    }
}