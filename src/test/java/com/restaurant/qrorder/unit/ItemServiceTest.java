package com.restaurant.qrorder.unit;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.dto.request.CreateItemRequest;
import com.restaurant.qrorder.domain.dto.response.ItemResponse;
import com.restaurant.qrorder.domain.entity.Category;
import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.ItemMapper;
import com.restaurant.qrorder.repository.CategoryRepository;
import com.restaurant.qrorder.repository.DiscountRepository;
import com.restaurant.qrorder.repository.ItemRepository;
import com.restaurant.qrorder.service.ItemService;
import com.restaurant.qrorder.util.ItemSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Unit Tests")
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private ItemMapper itemMapper;

    @InjectMocks private ItemService itemService;

    private Category category;
    private Item item;
    private ItemResponse baseResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Food");

        item = new Item();
        item.setId(1L);
        item.setName("Burger");
        item.setCategory(category);
        item.setDiscounts(Collections.emptyList());

        baseResponse = new ItemResponse();
        baseResponse.setId(1L);
        baseResponse.setName("Burger");

        pageable = PageRequest.of(0, 10);
    }

    private void stubMapper(Item i, ItemResponse r) {
        when(itemMapper.toResponse(i)).thenReturn(r);
    }

    private Discount buildDiscount(boolean active, LocalDateTime start,
                                   LocalDateTime end, String applicableDays) {
        return buildDiscount(active, DiscountType.ITEM_SPECIFIC, start, end, applicableDays);
    }

    private Discount buildDiscount(boolean active, DiscountType type,
                                   LocalDateTime start, LocalDateTime end,
                                   String applicableDays) {
        Discount d = new Discount();
        d.setId(10L);
        d.setName("Promo");
        d.setValue(BigDecimal.valueOf(15.0));
        d.setActive(active);
        d.setDiscountType(type);
        d.setStartDate(start);
        d.setEndDate(end);
        d.setApplicableDays(applicableDays);
        return d;
    }

    private ItemResponse resolveWithDiscount(Discount d) {
        item.setDiscounts(List.of(d));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        ItemResponse resp = new ItemResponse();
        resp.setId(1L);
        when(itemMapper.toResponse(item)).thenReturn(resp);
        return itemService.getItemById(1L);
    }

    private String todayName() {
        return LocalDateTime.now().getDayOfWeek().name();
    }

    private String tomorrowName() {
        return LocalDateTime.now().getDayOfWeek().plus(1).name();
    }

    @Nested
    @DisplayName("getAllItems()")
    class GetAllItems {

        @Test
        @DisplayName("categoryId null → findAll, no category validation")
        void nullCategoryId_fetchesAllItems() {
            Page<Item> page = new PageImpl<>(List.of(item));
            when(itemRepository.findAll(pageable)).thenReturn(page);
            stubMapper(item, baseResponse);

            Page<ItemResponse> result = itemService.getAllItems(null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(categoryRepository, never()).findById(any());
            verify(itemRepository).findAll(pageable);
        }

        @Test
        @DisplayName("categoryId non-null, category exists → findByCategoryId")
        void validCategoryId_fetchesCategoryItems() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            Page<Item> page = new PageImpl<>(List.of(item));
            when(itemRepository.findByCategoryId(1L, pageable)).thenReturn(page);
            stubMapper(item, baseResponse);

            Page<ItemResponse> result = itemService.getAllItems(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(itemRepository).findByCategoryId(1L, pageable);
        }

        @Test
        @DisplayName("categoryId non-null, category NOT found → ResourceNotFoundException")
        void invalidCategoryId_throwsException() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getAllItems(99L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 99");

            verify(itemRepository, never()).findByCategoryId(any(), any());
        }
    }

    @Nested
    @DisplayName("getAvailableItems()")
    class GetAvailableItems {

        @Test
        @DisplayName("categoryId null → findAllAvailableOrderByName")
        void nullCategoryId_fetchesAllAvailable() {
            when(itemRepository.findAllAvailableOrderByName()).thenReturn(List.of(item));
            stubMapper(item, baseResponse);

            List<ItemResponse> result = itemService.getAvailableItems(null);

            assertThat(result).hasSize(1);
            verify(categoryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("categoryId non-null, category exists → findAvailableItemsByCategory")
        void validCategoryId_fetchesAvailableByCat() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(itemRepository.findAvailableItemsByCategory(1L)).thenReturn(List.of(item));
            stubMapper(item, baseResponse);

            List<ItemResponse> result = itemService.getAvailableItems(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("categoryId non-null, category NOT found → ResourceNotFoundException")
        void invalidCategoryId_throwsException() {
            when(categoryRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getAvailableItems(42L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 42");
        }
    }

    @Nested
    @DisplayName("getItemById()")
    class GetItemById {

        @Test
        @DisplayName("item found → mapped response returned")
        void itemFound_returnsResponse() {
            when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
            stubMapper(item, baseResponse);

            ItemResponse result = itemService.getItemById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("item NOT found → ResourceNotFoundException")
        void itemNotFound_throwsException() {
            when(itemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getItemById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item not found with id: 99");
        }
    }

    @Nested
    @DisplayName("createItem()")
    class CreateItem {

        private CreateItemRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateItemRequest();
            request.setName("  Burger  ");
            request.setCategoryId(1L);
        }

        @Test
        @DisplayName("getDuplicated returns true → DuplicateResourceException, no save")
        void duplicateName_throwsException() {
            try (MockedStatic<ItemSpecification> spec = mockStatic(ItemSpecification.class)) {
                Specification<Item> mockSpec = mock(Specification.class);
                spec.when(() -> ItemSpecification.getItemByIdAndName("Burger")).thenReturn(mockSpec);
                when(itemRepository.findAll(mockSpec)).thenReturn(List.of(item));

                assertThatThrownBy(() -> itemService.createItem(request))
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessageContaining("Duplicated resource error");

                verify(categoryRepository, never()).findById(any());
                verify(itemRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("getDuplicated false, category found → item saved and returned")
        void uniqueNameCategoryFound_savesItem() {
            try (MockedStatic<ItemSpecification> spec = mockStatic(ItemSpecification.class)) {
                Specification<Item> mockSpec = mock(Specification.class);
                spec.when(() -> ItemSpecification.getItemByIdAndName("Burger")).thenReturn(mockSpec);
                when(itemRepository.findAll(mockSpec)).thenReturn(Collections.emptyList());
                when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
                when(itemMapper.toEntity(request)).thenReturn(item);
                when(itemRepository.save(item)).thenReturn(item);
                stubMapper(item, baseResponse);

                ItemResponse result = itemService.createItem(request);

                assertThat(result).isNotNull();
                verify(itemRepository).save(item);
            }
        }

        @Test
        @DisplayName("getDuplicated false, category NOT found → ResourceNotFoundException")
        void uniqueNameCategoryNotFound_throwsException() {
            try (MockedStatic<ItemSpecification> spec = mockStatic(ItemSpecification.class)) {
                Specification<Item> mockSpec = mock(Specification.class);
                spec.when(() -> ItemSpecification.getItemByIdAndName("Burger")).thenReturn(mockSpec);
                when(itemRepository.findAll(mockSpec)).thenReturn(Collections.emptyList());
                when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> itemService.createItem(request))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Category not found with id: 1");

                verify(itemRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("deleteItem()")
    class DeleteItem {

        @Test
        @DisplayName("item found → deleted via repository")
        void itemFound_deletesItem() {
            when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

            itemService.deleteItem(1L);

            verify(itemRepository).delete(item);
        }

        @Test
        @DisplayName("item NOT found → ResourceNotFoundException, no delete")
        void itemNotFound_throwsException() {
            when(itemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.deleteItem(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item not found with id: 99");

            verify(itemRepository, never()).delete(any(Item.class));
        }
    }

    @Nested
    @DisplayName("searchItems()")
    class SearchItems {

        @Test
        @DisplayName("delegates to repository and maps results")
        void keywordSearch_returnsMappedPage() {
            Page<Item> page = new PageImpl<>(List.of(item));
            when(itemRepository.searchItems("burg", pageable)).thenReturn(page);
            stubMapper(item, baseResponse);

            Page<ItemResponse> result = itemService.searchItems("burg", pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getDuplicated()")
    class GetDuplicated {

        @Test
        @DisplayName("repository returns non-empty list → true")
        void duplicateFound_returnsTrue() {
            try (MockedStatic<ItemSpecification> spec = mockStatic(ItemSpecification.class)) {
                Specification<Item> mockSpec = mock(Specification.class);
                spec.when(() -> ItemSpecification.getItemByIdAndName("Pizza")).thenReturn(mockSpec);
                when(itemRepository.findAll(mockSpec)).thenReturn(List.of(item));

                assertThat(itemService.getDuplicated("Pizza")).isTrue();
            }
        }

        @Test
        @DisplayName("repository returns empty list → false")
        void noDuplicate_returnsFalse() {
            try (MockedStatic<ItemSpecification> spec = mockStatic(ItemSpecification.class)) {
                Specification<Item> mockSpec = mock(Specification.class);
                spec.when(() -> ItemSpecification.getItemByIdAndName("Tacos")).thenReturn(mockSpec);
                when(itemRepository.findAll(mockSpec)).thenReturn(Collections.emptyList());

                assertThat(itemService.getDuplicated("Tacos")).isFalse();
            }
        }

        @Test
        @DisplayName("name with whitespace is trimmed before spec lookup")
        void whitespaceNameTrimmed_beforeLookup() {
            try (MockedStatic<ItemSpecification> spec = mockStatic(ItemSpecification.class)) {
                Specification<Item> mockSpec = mock(Specification.class);
                spec.when(() -> ItemSpecification.getItemByIdAndName("Sushi")).thenReturn(mockSpec);
                when(itemRepository.findAll(mockSpec)).thenReturn(Collections.emptyList());

                itemService.getDuplicated("  Sushi  ");

                spec.verify(() -> ItemSpecification.getItemByIdAndName("Sushi"));
            }
        }
    }

    @Nested
    @DisplayName("isDiscountActive() branches — indirect coverage via getItemById()")
    class IsDiscountActive {

        @Test
        @DisplayName("B1: active=false → excluded")
        void b1_inactiveDiscount_excluded() {
            Discount d = buildDiscount(false, null, null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("B2: discountType != ITEM_SPECIFIC → excluded by stream pre-filter")
        void b2_nonItemSpecificType_excludedByStreamFilter() {
            Discount d = buildDiscount(true, DiscountType.HOLIDAY, null, null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("B3: startDate in future → excluded")
        void b3_startDateInFuture_excluded() {
            Discount d = buildDiscount(true, LocalDateTime.now().plusDays(1), null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("B4: startDate=null → start check skipped → included (no other constraint)")
        void b4_nullStartDate_passesCheck() {
            Discount d = buildDiscount(true, null, null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B5: startDate in past → not before → passes start check → included")
        void b5_startDateInPast_passesCheck() {
            Discount d = buildDiscount(true, LocalDateTime.now().minusDays(1), null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B6: endDate in past → excluded")
        void b6_endDateInPast_excluded() {
            Discount d = buildDiscount(true, null, LocalDateTime.now().minusDays(1), null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("B7: endDate=null → end check skipped → included (no other constraint)")
        void b7_nullEndDate_passesCheck() {
            Discount d = buildDiscount(true, null, null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B8: endDate in future → not after → passes end check → included")
        void b8_endDateInFuture_passesCheck() {
            Discount d = buildDiscount(true, null, LocalDateTime.now().plusDays(1), null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B9: applicableDays=null → day check skipped → included")
        void b9_nullApplicableDays_skipped() {
            Discount d = buildDiscount(true, null, null, null);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B10: applicableDays is empty string → day check skipped → included")
        void b10_emptyApplicableDays_skipped() {
            Discount d = buildDiscount(true, null, null, "");
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B11: applicableDays contains today → isDayApplicable=true → included")
        void b11_applicableDaysContainsToday_included() {
            Discount d = buildDiscount(true, null, null, todayName());
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("B12: applicableDays contains only tomorrow → isDayApplicable=false → excluded")
        void b12_applicableDaysDoesNotContainToday_excluded() {
            Discount d = buildDiscount(true, null, null, tomorrowName());
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("B13: invalid day string only → IllegalArgumentException caught, warn logged → excluded")
        void b13_invalidDayString_warnLoggedAndExcluded() {
            Discount d = buildDiscount(true, null, null, "FUNDAY");
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).isEmpty();
        }

        @Test
        @DisplayName("B14: invalid day then valid today → catch skips invalid, matches today → included")
        void b14_invalidDayThenValidToday_included() {
            String days = "FUNDAY," + todayName();
            Discount d = buildDiscount(true, null, null, days);
            assertThat(resolveWithDiscount(d).getActiveDiscounts()).hasSize(1);
        }

        @Test
        @DisplayName("Included discount → ActiveDiscountInfo fields populated from entity")
        void includedDiscount_infoFieldsMapped() {
            Discount d = buildDiscount(true, null, null, null);
            d.setId(55L);
            d.setName("Happy Hour");
            d.setValue(BigDecimal.valueOf(20.0));

            ItemResponse resp = resolveWithDiscount(d);

            assertThat(resp.getActiveDiscounts()).hasSize(1);
            ItemResponse.ActiveDiscountInfo info = resp.getActiveDiscounts().get(0);
            assertThat(info.getDiscountId()).isEqualTo(55L);
            assertThat(info.getDiscountName()).isEqualTo("Happy Hour");
            assertThat(info.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
            assertThat(info.getDiscountType()).isEqualTo(DiscountType.ITEM_SPECIFIC.name());
        }

        @Test
        @DisplayName("Multiple discounts: only qualifying ITEM_SPECIFIC active ones appear")
        void multipleDiscounts_onlyQualifyingIncluded() {
            Discount active   = buildDiscount(true,  DiscountType.ITEM_SPECIFIC, null, null, null);
            Discount inactive = buildDiscount(false, DiscountType.ITEM_SPECIFIC, null, null, null);
            Discount holiday  = buildDiscount(true,  DiscountType.HOLIDAY,       null, null, null);

            item.setDiscounts(List.of(active, inactive, holiday));
            when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
            ItemResponse resp = new ItemResponse();
            resp.setId(1L);
            when(itemMapper.toResponse(item)).thenReturn(resp);

            ItemResponse result = itemService.getItemById(1L);
            assertThat(result.getActiveDiscounts()).hasSize(1);
        }
    }
}