package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RefrigeratorDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorRequestDTO;
import ac.su.kdt.prompttest.entity.Refrigerator;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.RefrigeratorRepository;
import ac.su.kdt.prompttest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefrigeratorServiceTest {

    @Mock
    private RefrigeratorRepository refrigeratorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RefrigeratorService refrigeratorService;

    private User testUser;
    private Refrigerator testRefrigerator;
    private RefrigeratorRequestDTO requestDTO;

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

        requestDTO = RefrigeratorRequestDTO.builder()
                .name("테스트 냉장고")
                .description("테스트용 냉장고입니다")
                .build();
    }

    @Test
    void getUserRefrigerators_ShouldReturnUserRefrigerators() {
        // given
        List<Refrigerator> refrigerators = Arrays.asList(testRefrigerator);
        when(refrigeratorRepository.findByUserId(1)).thenReturn(refrigerators);

        // when
        List<RefrigeratorDTO> result = refrigeratorService.getUserRefrigerators(1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 냉장고");
        verify(refrigeratorRepository).findByUserId(1);
    }

    @Test
    void getRefrigerator_ShouldReturnRefrigerator() {
        // given
        when(refrigeratorRepository.findById(1)).thenReturn(Optional.of(testRefrigerator));

        // when
        RefrigeratorDTO result = refrigeratorService.getRefrigerator(1);

        // then
        assertThat(result.getName()).isEqualTo("테스트 냉장고");
        assertThat(result.getUserId()).isEqualTo(1);
        verify(refrigeratorRepository).findById(1);
    }

    @Test
    void getRefrigerator_ShouldThrowException_WhenNotFound() {
        // given
        when(refrigeratorRepository.findById(999)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refrigeratorService.getRefrigerator(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refrigerator not found");
    }

    @Test
    void createRefrigerator_ShouldCreateNewRefrigerator() {
        // given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(refrigeratorRepository.existsByUserIdAndName(1, "테스트 냉장고")).thenReturn(false);
        when(refrigeratorRepository.save(any(Refrigerator.class))).thenReturn(testRefrigerator);

        // when
        RefrigeratorDTO result = refrigeratorService.createRefrigerator(1, requestDTO);

        // then
        assertThat(result.getName()).isEqualTo("테스트 냉장고");
        assertThat(result.getUserId()).isEqualTo(1);
        verify(refrigeratorRepository).save(any(Refrigerator.class));
    }

    @Test
    void createRefrigerator_ShouldCreateNewRefrigerator_WhenNameAlreadyExists() {
        // given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        // 첫 번째 호출: true(중복), 두 번째 호출: false(중복 아님)
        when(refrigeratorRepository.existsByUserIdAndName(eq(1), anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(1);
            return name.equals("테스트 냉장고");
        });
        when(refrigeratorRepository.save(any(Refrigerator.class))).thenAnswer(invocation -> {
            Refrigerator saved = invocation.getArgument(0);
            saved.setRefrigeratorId(2);
            return saved;
        });

        // when
        RefrigeratorDTO result = refrigeratorService.createRefrigerator(1, requestDTO);

        // then
        assertThat(result.getName()).isEqualTo("테스트 냉장고 (1)");
        assertThat(result.getUserId()).isEqualTo(1);
        verify(refrigeratorRepository, atLeastOnce()).save(any(Refrigerator.class));
        verify(refrigeratorRepository, atLeast(2)).existsByUserIdAndName(eq(1), anyString());
    }

    @Test
    void updateRefrigerator_ShouldUpdateRefrigerator() {
        // given
        RefrigeratorRequestDTO updateDTO = RefrigeratorRequestDTO.builder()
                .name("수정된 냉장고")
                .description("수정된 설명")
                .build();

        when(refrigeratorRepository.findById(1)).thenReturn(Optional.of(testRefrigerator));
        when(refrigeratorRepository.existsByUserIdAndName(1, "수정된 냉장고")).thenReturn(false);
        when(refrigeratorRepository.save(any(Refrigerator.class))).thenReturn(testRefrigerator);

        // when
        RefrigeratorDTO result = refrigeratorService.updateRefrigerator(1, updateDTO);

        // then
        verify(refrigeratorRepository).save(any(Refrigerator.class));
    }

    @Test
    void deleteRefrigerator_ShouldDeleteRefrigerator() {
        // given
        when(refrigeratorRepository.existsById(1)).thenReturn(true);

        // when
        refrigeratorService.deleteRefrigerator(1);

        // then
        verify(refrigeratorRepository).deleteById(1);
    }

    @Test
    void deleteRefrigerator_ShouldThrowException_WhenNotFound() {
        // given
        when(refrigeratorRepository.existsById(999)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> refrigeratorService.deleteRefrigerator(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refrigerator not found");
    }
} 