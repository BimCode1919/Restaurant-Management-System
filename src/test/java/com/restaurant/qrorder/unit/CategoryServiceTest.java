package com.restaurant.qrorder.unit;

import com.restaurant.qrorder.domain.dto.request.CreateCategoryRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateCategoryRequest;
import com.restaurant.qrorder.domain.dto.response.CategoryResponse;
import com.restaurant.qrorder.domain.entity.Category;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.CategoryMapper;
import com.restaurant.qrorder.repository.CategoryRepository;
import com.restaurant.qrorder.service.CategoryService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private CategoryService categoryService;

    private Category category;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Food")
                .description("Food items")
                .build();

        categoryResponse = new CategoryResponse();
        categoryResponse.setId(1L);
        categoryResponse.setName("Food");
    }

    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("returns mapped list of all categories ordered by name")
        void returnsAllCategories() {
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(categoryRepository).findAllByOrderByNameAsc();
        }

        @Test
        @DisplayName("returns empty list when no categories exist")
        void returnsEmptyList() {
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCategoryById()")
    class GetCategoryById {

        @Test
        @DisplayName("category found → returns mapped response")
        void categoryFound_returnsResponse() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.getCategoryById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("category NOT found → throws ResourceNotFoundException")
        void categoryNotFound_throwsException() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 99");
        }
    }

    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {

        private CreateCategoryRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateCategoryRequest();
            request.setName("Food");
            request.setDescription("Food items");
        }

        @Test
        @DisplayName("name already exists (case-insensitive) → throws DuplicateResourceException")
        void duplicateName_throwsException() {
            Category existing = Category.builder().id(2L).name("food").build();
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(existing));

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Category already exists with name: Food");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("unique name → saves and returns category")
        void uniqueName_savesCategory() {
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.createCategory(request);

            assertThat(result).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("existing category with different name → saves new category")
        void existingCategoryWithDifferentName_savesCategory() {
            Category other = Category.builder().id(2L).name("Drinks").build();
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(other));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.createCategory(request);

            assertThat(result).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategory {

        private UpdateCategoryRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateCategoryRequest();
        }

        @Test
        @DisplayName("category NOT found → throws ResourceNotFoundException")
        void categoryNotFound_throwsException() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 99");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("request.name is null → name not updated, saves with existing name")
        void nullName_skipsNameUpdate() {
            request.setName(null);
            request.setDescription("Updated desc");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            CategoryResponse result = categoryService.updateCategory(1L, request);

            assertThat(result).isNotNull();
            assertThat(category.getName()).isEqualTo("Food");
            verify(categoryRepository, never()).findAllByOrderByNameAsc();
        }

        @Test
        @DisplayName("request.name equals existing name → name update skipped, no duplicate check")
        void sameName_skipsNameUpdate() {
            request.setName("Food");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.updateCategory(1L, request);

            verify(categoryRepository, never()).findAllByOrderByNameAsc();
        }

        @Test
        @DisplayName("request.name differs, no conflict → name updated and saved")
        void newUniqueName_updatesName() {
            request.setName("Beverages");
            Category other = Category.builder().id(2L).name("Drinks").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(other));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.updateCategory(1L, request);

            assertThat(category.getName()).isEqualTo("Beverages");
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("request.name differs, conflict with another category → throws DuplicateResourceException")
        void newNameConflictsWithOtherCategory_throwsException() {
            request.setName("Beverages");
            Category conflicting = Category.builder().id(2L).name("beverages").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(conflicting));

            assertThatThrownBy(() -> categoryService.updateCategory(1L, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Category already exists with name: Beverages");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("request.name differs, conflict is same category (same id) → no duplicate thrown")
        void newNameConflictsWithSameCategory_noException() {
            request.setName("Beverages");
            Category sameId = Category.builder().id(1L).name("beverages").build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(sameId));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.updateCategory(1L, request);

            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("request.description is null → description not updated")
        void nullDescription_skipsDescriptionUpdate() {
            request.setDescription(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.updateCategory(1L, request);

            assertThat(category.getDescription()).isEqualTo("Food items");
        }

        @Test
        @DisplayName("request.description is non-null → description updated")
        void nonNullDescription_updatesDescription() {
            request.setDescription("New description");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            categoryService.updateCategory(1L, request);

            assertThat(category.getDescription()).isEqualTo("New description");
        }
    }

    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategory {

        @Test
        @DisplayName("category found → deleted via repository")
        void categoryFound_deletesCategory() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            categoryService.deleteCategory(1L);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("category NOT found → throws ResourceNotFoundException, no delete")
        void categoryNotFound_throwsException() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 99");

            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }
}