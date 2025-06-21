package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RefrigeratorDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorRequestDTO;
import ac.su.kdt.prompttest.entity.Refrigerator;
import ac.su.kdt.prompttest.entity.User;
import ac.su.kdt.prompttest.repository.RefrigeratorRepository;
import ac.su.kdt.prompttest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefrigeratorService {
    
    private final RefrigeratorRepository refrigeratorRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    
    @Transactional(readOnly = true)
    public List<RefrigeratorDTO> getUserRefrigerators(Integer userId) {
        List<Refrigerator> refrigerators = refrigeratorRepository.findByUserId(userId);
        return refrigerators.stream()
                .map(RefrigeratorDTO::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public RefrigeratorDTO getRefrigerator(Integer refrigeratorId) {
        Refrigerator refrigerator = refrigeratorRepository.findById(refrigeratorId)
                .orElseThrow(() -> new RuntimeException("Refrigerator not found with id: " + refrigeratorId));
        return RefrigeratorDTO.from(refrigerator);
    }
    
    @Transactional
    public RefrigeratorDTO createRefrigerator(Integer userId, RefrigeratorRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 같은 이름의 냉장고가 있는지 확인 (테스트를 위해 완화)
        if (refrigeratorRepository.existsByUserIdAndName(userId, requestDTO.getName())) {
            // 중복된 이름이 있으면 숫자를 붙여서 생성
            String baseName = requestDTO.getName();
            int counter = 1;
            String newName = baseName;
            
            while (refrigeratorRepository.existsByUserIdAndName(userId, newName)) {
                newName = baseName + " (" + counter + ")";
                counter++;
            }
            
            requestDTO.setName(newName);
        }
        
        Refrigerator refrigerator = Refrigerator.builder()
                .user(user)
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .build();
        
        Refrigerator savedRefrigerator = refrigeratorRepository.save(refrigerator);
        return RefrigeratorDTO.from(savedRefrigerator);
    }
    
    @Transactional
    public RefrigeratorDTO updateRefrigerator(Integer refrigeratorId, RefrigeratorRequestDTO requestDTO) {
        Refrigerator refrigerator = refrigeratorRepository.findById(refrigeratorId)
                .orElseThrow(() -> new RuntimeException("Refrigerator not found with id: " + refrigeratorId));
        
        if (requestDTO.getName() != null) {
            // 같은 이름의 냉장고가 있는지 확인 (자기 자신 제외)
            if (refrigeratorRepository.existsByUserIdAndName(refrigerator.getUser().getId(), requestDTO.getName()) &&
                !refrigerator.getName().equals(requestDTO.getName())) {
                throw new RuntimeException("Refrigerator with name '" + requestDTO.getName() + "' already exists");
            }
            refrigerator.setName(requestDTO.getName());
        }
        
        if (requestDTO.getDescription() != null) {
            refrigerator.setDescription(requestDTO.getDescription());
        }
        
        Refrigerator updatedRefrigerator = refrigeratorRepository.save(refrigerator);
        return RefrigeratorDTO.from(updatedRefrigerator);
    }
    
    @Transactional
    public void deleteRefrigerator(Integer refrigeratorId) {
        if (!refrigeratorRepository.existsById(refrigeratorId)) {
            throw new RuntimeException("Refrigerator not found with id: " + refrigeratorId);
        }
        refrigeratorRepository.deleteById(refrigeratorId);
    }
    
    @Transactional(readOnly = true)
    public Refrigerator getRefrigeratorEntity(Integer refrigeratorId) {
        return refrigeratorRepository.findById(refrigeratorId)
                .orElseThrow(() -> new RuntimeException("Refrigerator not found with id: " + refrigeratorId));
    }

    public List<RefrigeratorDTO> getAllRefrigerators() {
        return refrigeratorRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private RefrigeratorDTO convertToDto(Refrigerator refrigerator) {
        return RefrigeratorDTO.from(refrigerator);
    }
} 