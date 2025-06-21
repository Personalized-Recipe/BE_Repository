package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RefrigeratorIngredientDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorIngredientRequestDTO;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.entity.Refrigerator;
import ac.su.kdt.prompttest.entity.RefrigeratorIngredient;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import ac.su.kdt.prompttest.repository.RefrigeratorIngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefrigeratorIngredientServiceTest {

    @Mock
    private RefrigeratorIngredientRepository refrigeratorIngredientRepository;

    @Mock
    private RefrigeratorService refrigeratorService;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RefrigeratorIngredientService refrigeratorIngredientService;

    private User testUser;
    private Refrigerator testRefrigerator;
    private Ingredient testIngredient;
    private RefrigeratorIngredient testRefrigeratorIngredient;
    private RefrigeratorIngredientRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1)
                .email("test@example.com")
                .name("Test User")
                .build();

        testRefrigerator = Refrigerator.builder()
                .refrigeratorId(1)
                .user(testUser)
                .name("테스트 냉장고")
                .description("테스트용 냉장고입니다")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testIngredient = Ingredient.builder()
                .ingredientId(1)
                .name("양파")
                .requiredAmount(100.0f)
                .calories(40)
                .build();

        testRefrigeratorIngredient = RefrigeratorIngredient.builder()
                .id(1)
                .refrigerator(testRefrigerator)
                .ingredient(testIngredient)
                .quantity(2.0f)
                .unit("개")
                .expiryDate(LocalDate.now().plusDays(7))
                .purchaseDate(LocalDate.now())
                .storageLocation(RefrigeratorIngredient.StorageLocation.REFRIGERATOR)
                .freshnessStatus(RefrigeratorIngredient.FreshnessStatus.GOOD)
                .notes("신선한 양파입니다")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDTO = RefrigeratorIngredientRequestDTO.builder()
                .ingredientId(1)
                .quantity(2.0f)
                .unit("개")
                .expiryDate(LocalDate.now().plusDays(7))
                .purchaseDate(LocalDate.now())
                .storageLocation("냉장실")
                .notes("신선한 양파입니다")
                .build();
    }

    @Test
    void getRefrigeratorIngredients_ShouldReturnIngredients() {
        // given
        List<RefrigeratorIngredient> ingredients = Arrays.asList(testRefrigeratorIngredient);
        when(refrigeratorIngredientRepository.findByRefrigeratorId(1)).thenReturn(ingredients);

        // when
        List<RefrigeratorIngredientDTO> result = refrigeratorIngredientService.getRefrigeratorIngredients(1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIngredientName()).isEqualTo("양파");
        assertThat(result.get(0).getQuantity()).isEqualTo(2.0f);
        verify(refrigeratorIngredientRepository).findByRefrigeratorId(1);
    }

    @Test
    void getRefrigeratorIngredient_ShouldReturnIngredient() {
        // given
        when(refrigeratorIngredientRepository.findById(1)).thenReturn(Optional.of(testRefrigeratorIngredient));

        // when
        RefrigeratorIngredientDTO result = refrigeratorIngredientService.getRefrigeratorIngredient(1);

        // then
        assertThat(result.getIngredientName()).isEqualTo("양파");
        assertThat(result.getRefrigeratorId()).isEqualTo(1);
        verify(refrigeratorIngredientRepository).findById(1);
    }

    @Test
    void getRefrigeratorIngredient_ShouldThrowException_WhenNotFound() {
        // given
        when(refrigeratorIngredientRepository.findById(999)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refrigeratorIngredientService.getRefrigeratorIngredient(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refrigerator ingredient not found");
    }

    @Test
    void addIngredient_ShouldAddNewIngredient() {
        // given
        when(refrigeratorService.getRefrigeratorEntity(1)).thenReturn(testRefrigerator);
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(testIngredient));
        when(refrigeratorIngredientRepository.findByRefrigeratorIdAndIngredientIdAndUnit(1, 1, "개"))
                .thenReturn(Optional.empty());
        when(refrigeratorIngredientRepository.save(any(RefrigeratorIngredient.class)))
                .thenReturn(testRefrigeratorIngredient);

        // when
        RefrigeratorIngredientDTO result = refrigeratorIngredientService.addIngredient(1, requestDTO);

        // then
        assertThat(result.getIngredientName()).isEqualTo("양파");
        assertThat(result.getQuantity()).isEqualTo(2.0f);
        verify(refrigeratorIngredientRepository).save(any(RefrigeratorIngredient.class));
    }

    @Test
    void addIngredient_ShouldThrowException_WhenIngredientNotFound() {
        // given
        when(refrigeratorService.getRefrigeratorEntity(1)).thenReturn(testRefrigerator);
        when(ingredientRepository.findById(999)).thenReturn(Optional.empty());

        RefrigeratorIngredientRequestDTO invalidRequest = RefrigeratorIngredientRequestDTO.builder()
                .ingredientId(999)
                .quantity(2.0f)
                .unit("개")
                .build();

        // when & then
        assertThatThrownBy(() -> refrigeratorIngredientService.addIngredient(1, invalidRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ingredient not found");
    }

    @Test
    void addIngredient_ShouldThrowException_WhenSameIngredientAndUnitExists() {
        // given
        when(refrigeratorService.getRefrigeratorEntity(1)).thenReturn(testRefrigerator);
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(testIngredient));
        when(refrigeratorIngredientRepository.findByRefrigeratorIdAndIngredientIdAndUnit(1, 1, "개"))
                .thenReturn(Optional.of(testRefrigeratorIngredient));

        // when & then
        assertThatThrownBy(() -> refrigeratorIngredientService.addIngredient(1, requestDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updateIngredient_ShouldUpdateIngredient() {
        // given
        RefrigeratorIngredientRequestDTO updateDTO = RefrigeratorIngredientRequestDTO.builder()
                .quantity(3.0f)
                .notes("수정된 메모")
                .build();

        when(refrigeratorIngredientRepository.findById(1)).thenReturn(Optional.of(testRefrigeratorIngredient));
        when(refrigeratorIngredientRepository.save(any(RefrigeratorIngredient.class)))
                .thenReturn(testRefrigeratorIngredient);

        // when
        RefrigeratorIngredientDTO result = refrigeratorIngredientService.updateIngredient(1, updateDTO);

        // then
        verify(refrigeratorIngredientRepository).save(any(RefrigeratorIngredient.class));
    }

    @Test
    void deleteIngredient_ShouldDeleteIngredient() {
        // given
        when(refrigeratorIngredientRepository.existsById(1)).thenReturn(true);

        // when
        refrigeratorIngredientService.deleteIngredient(1);

        // then
        verify(refrigeratorIngredientRepository).deleteById(1);
    }

    @Test
    void getExpiringIngredients_ShouldReturnExpiringIngredients() {
        // given
        List<RefrigeratorIngredient> ingredients = Arrays.asList(testRefrigeratorIngredient);
        when(refrigeratorIngredientRepository.findExpiringIngredients(eq(1), any(LocalDate.class)))
                .thenReturn(ingredients);

        // when
        List<RefrigeratorIngredientDTO> result = refrigeratorIngredientService.getExpiringIngredients(1);

        // then
        assertThat(result).hasSize(1);
        verify(refrigeratorIngredientRepository).findExpiringIngredients(eq(1), any(LocalDate.class));
    }

    @Test
    void getExpiredIngredients_ShouldReturnExpiredIngredients() {
        // given
        List<RefrigeratorIngredient> ingredients = Arrays.asList(testRefrigeratorIngredient);
        when(refrigeratorIngredientRepository.findExpiredIngredients(eq(1), any(LocalDate.class)))
                .thenReturn(ingredients);

        // when
        List<RefrigeratorIngredientDTO> result = refrigeratorIngredientService.getExpiredIngredients(1);

        // then
        assertThat(result).hasSize(1);
        verify(refrigeratorIngredientRepository).findExpiredIngredients(eq(1), any(LocalDate.class));
    }

    @Test
    void searchIngredients_ShouldReturnMatchingIngredients() {
        // given
        List<RefrigeratorIngredient> ingredients = Arrays.asList(testRefrigeratorIngredient);
        when(refrigeratorIngredientRepository.findByRefrigeratorIdAndIngredientNameContaining(1, "양파"))
                .thenReturn(ingredients);

        // when
        List<RefrigeratorIngredientDTO> result = refrigeratorIngredientService.searchIngredients(1, "양파");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIngredientName()).isEqualTo("양파");
        verify(refrigeratorIngredientRepository).findByRefrigeratorIdAndIngredientNameContaining(1, "양파");
    }
} 