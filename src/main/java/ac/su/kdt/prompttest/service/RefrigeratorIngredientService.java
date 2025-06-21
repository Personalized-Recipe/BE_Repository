package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.dto.RefrigeratorIngredientDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorIngredientRequestDTO;
import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.entity.Refrigerator;
import ac.su.kdt.prompttest.entity.RefrigeratorIngredient;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import ac.su.kdt.prompttest.repository.RefrigeratorIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefrigeratorIngredientService {
    
    private final RefrigeratorIngredientRepository refrigeratorIngredientRepository;
    private final RefrigeratorService refrigeratorService;
    private final IngredientRepository ingredientRepository;
    
    @Transactional(readOnly = true)
    public List<RefrigeratorIngredientDTO> getRefrigeratorIngredients(Integer refrigeratorId) {
        List<RefrigeratorIngredient> ingredients = refrigeratorIngredientRepository.findByRefrigeratorRefrigeratorId(refrigeratorId);
        return ingredients.stream()
                .map(RefrigeratorIngredientDTO::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public RefrigeratorIngredientDTO getRefrigeratorIngredient(Integer ingredientId) {
        RefrigeratorIngredient ingredient = refrigeratorIngredientRepository.findById(ingredientId)
                .orElseThrow(() -> new RuntimeException("Refrigerator ingredient not found with id: " + ingredientId));
        return RefrigeratorIngredientDTO.from(ingredient);
    }
    
    @Transactional
    public RefrigeratorIngredientDTO addIngredient(Integer refrigeratorId, RefrigeratorIngredientRequestDTO requestDTO) {
        Refrigerator refrigerator = refrigeratorService.getRefrigeratorEntity(refrigeratorId);
        Ingredient ingredient = ingredientRepository.findById(requestDTO.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Ingredient not found with id: " + requestDTO.getIngredientId()));
        
        // 같은 재료와 단위가 있는지 확인
        refrigeratorIngredientRepository.findByRefrigeratorRefrigeratorIdAndIngredientIngredientIdAndUnit(
                refrigeratorId, requestDTO.getIngredientId(), requestDTO.getUnit())
                .ifPresent(existing -> {
                    throw new RuntimeException("Ingredient with same unit already exists in refrigerator");
                });
        
        RefrigeratorIngredient refrigeratorIngredient = RefrigeratorIngredient.builder()
                .refrigerator(refrigerator)
                .ingredient(ingredient)
                .quantity(requestDTO.getQuantity())
                .unit(requestDTO.getUnit())
                .expiryDate(requestDTO.getExpiryDate())
                .purchaseDate(requestDTO.getPurchaseDate())
                .storageLocation(parseStorageLocation(requestDTO.getStorageLocation()))
                .freshnessStatus(parseFreshnessStatus(requestDTO.getFreshness()))
                .notes(requestDTO.getNotes())
                .build();
        
        RefrigeratorIngredient savedIngredient = refrigeratorIngredientRepository.save(refrigeratorIngredient);
        return RefrigeratorIngredientDTO.from(savedIngredient);
    }
    
    @Transactional
    public RefrigeratorIngredientDTO updateIngredient(Integer ingredientId, RefrigeratorIngredientRequestDTO requestDTO) {
        RefrigeratorIngredient ingredient = refrigeratorIngredientRepository.findById(ingredientId)
                .orElseThrow(() -> new RuntimeException("Refrigerator ingredient not found with id: " + ingredientId));
        
        if (requestDTO.getQuantity() != null) {
            ingredient.setQuantity(requestDTO.getQuantity());
        }
        if (requestDTO.getUnit() != null) {
            ingredient.setUnit(requestDTO.getUnit());
        }
        if (requestDTO.getExpiryDate() != null) {
            ingredient.setExpiryDate(requestDTO.getExpiryDate());
        }
        if (requestDTO.getPurchaseDate() != null) {
            ingredient.setPurchaseDate(requestDTO.getPurchaseDate());
        }
        if (requestDTO.getStorageLocation() != null) {
            ingredient.setStorageLocation(parseStorageLocation(requestDTO.getStorageLocation()));
        }
        if (requestDTO.getFreshness() != null) {
            ingredient.setFreshnessStatus(parseFreshnessStatus(requestDTO.getFreshness()));
        }
        if (requestDTO.getNotes() != null) {
            ingredient.setNotes(requestDTO.getNotes());
        }
        
        RefrigeratorIngredient updatedIngredient = refrigeratorIngredientRepository.save(ingredient);
        return RefrigeratorIngredientDTO.from(updatedIngredient);
    }
    
    @Transactional
    public void deleteIngredient(Integer ingredientId) {
        if (!refrigeratorIngredientRepository.existsById(ingredientId)) {
            throw new RuntimeException("Refrigerator ingredient not found with id: " + ingredientId);
        }
        refrigeratorIngredientRepository.deleteById(ingredientId);
    }
    
    @Transactional(readOnly = true)
    public List<RefrigeratorIngredientDTO> getExpiringIngredients(Integer refrigeratorId) {
        LocalDate expiryDate = LocalDate.now().plusDays(7); // 7일 이내
        List<RefrigeratorIngredient> ingredients = refrigeratorIngredientRepository.findExpiringIngredients(refrigeratorId, expiryDate);
        return ingredients.stream()
                .map(RefrigeratorIngredientDTO::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<RefrigeratorIngredientDTO> getExpiredIngredients(Integer refrigeratorId) {
        List<RefrigeratorIngredient> ingredients = refrigeratorIngredientRepository.findExpiredIngredients(refrigeratorId, LocalDate.now());
        return ingredients.stream()
                .map(RefrigeratorIngredientDTO::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<RefrigeratorIngredientDTO> getIngredientsByStorageLocation(Integer refrigeratorId, String storageLocation) {
        RefrigeratorIngredient.StorageLocation location = parseStorageLocation(storageLocation);
        List<RefrigeratorIngredient> ingredients = refrigeratorIngredientRepository.findByRefrigeratorRefrigeratorIdAndStorageLocation(refrigeratorId, location);
        return ingredients.stream()
                .map(RefrigeratorIngredientDTO::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<RefrigeratorIngredientDTO> searchIngredients(Integer refrigeratorId, String ingredientName) {
        List<RefrigeratorIngredient> ingredients = refrigeratorIngredientRepository.findByRefrigeratorRefrigeratorIdAndIngredientNameContaining(refrigeratorId, ingredientName);
        return ingredients.stream()
                .map(RefrigeratorIngredientDTO::from)
                .collect(Collectors.toList());
    }
    
    private RefrigeratorIngredient.StorageLocation parseStorageLocation(String storageLocation) {
        if (storageLocation == null) return RefrigeratorIngredient.StorageLocation.REFRIGERATOR;
        
        switch (storageLocation.toLowerCase()) {
            case "냉동실":
            case "freezer":
                return RefrigeratorIngredient.StorageLocation.FREEZER;
            case "냉장실":
            case "refrigerator":
                return RefrigeratorIngredient.StorageLocation.REFRIGERATOR;
            case "문쪽":
            case "door":
                return RefrigeratorIngredient.StorageLocation.DOOR;
            case "채소칸":
            case "crisper":
                return RefrigeratorIngredient.StorageLocation.CRISPER;
            case "육류칸":
            case "meat_drawer":
                return RefrigeratorIngredient.StorageLocation.MEAT_DRAWER;
            default:
                return RefrigeratorIngredient.StorageLocation.REFRIGERATOR;
        }
    }
    
    private RefrigeratorIngredient.FreshnessStatus parseFreshnessStatus(String freshness) {
        if (freshness == null) return RefrigeratorIngredient.FreshnessStatus.GOOD;
        
        switch (freshness.toLowerCase()) {
            case "매우 신선":
            case "excellent":
                return RefrigeratorIngredient.FreshnessStatus.EXCELLENT;
            case "신선":
            case "good":
                return RefrigeratorIngredient.FreshnessStatus.GOOD;
            case "보통":
            case "fair":
                return RefrigeratorIngredient.FreshnessStatus.FAIR;
            case "나쁨":
            case "poor":
                return RefrigeratorIngredient.FreshnessStatus.POOR;
            case "만료됨":
            case "expired":
                return RefrigeratorIngredient.FreshnessStatus.EXPIRED;
            default:
                return RefrigeratorIngredient.FreshnessStatus.GOOD;
        }
    }
} 