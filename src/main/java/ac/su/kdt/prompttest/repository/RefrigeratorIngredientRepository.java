package ac.su.kdt.prompttest.repository;

import ac.su.kdt.prompttest.entity.RefrigeratorIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefrigeratorIngredientRepository extends JpaRepository<RefrigeratorIngredient, Integer> {
    List<RefrigeratorIngredient> findByRefrigeratorRefrigeratorId(Integer refrigeratorId);
    
    List<RefrigeratorIngredient> findByRefrigeratorRefrigeratorIdAndIngredientIngredientId(Integer refrigeratorId, Integer ingredientId);
    
    Optional<RefrigeratorIngredient> findByRefrigeratorRefrigeratorIdAndIngredientIngredientIdAndUnit(
        Integer refrigeratorId, Integer ingredientId, String unit);
    
    // 유통기한이 임박한 재료 조회 (7일 이내)
    @Query("SELECT ri FROM RefrigeratorIngredient ri WHERE ri.refrigerator.refrigeratorId = :refrigeratorId AND ri.expiryDate <= :expiryDate")
    List<RefrigeratorIngredient> findExpiringIngredients(@Param("refrigeratorId") Integer refrigeratorId, 
                                                        @Param("expiryDate") LocalDate expiryDate);
    
    // 만료된 재료 조회
    @Query("SELECT ri FROM RefrigeratorIngredient ri WHERE ri.refrigerator.refrigeratorId = :refrigeratorId AND ri.expiryDate < :today")
    List<RefrigeratorIngredient> findExpiredIngredients(@Param("refrigeratorId") Integer refrigeratorId, 
                                                       @Param("today") LocalDate today);
    
    // 보관 위치별 재료 조회
    List<RefrigeratorIngredient> findByRefrigeratorRefrigeratorIdAndStorageLocation(Integer refrigeratorId, 
                                                                       RefrigeratorIngredient.StorageLocation storageLocation);
    
    // 신선도 상태별 재료 조회
    List<RefrigeratorIngredient> findByRefrigeratorRefrigeratorIdAndFreshnessStatus(Integer refrigeratorId, 
                                                                       RefrigeratorIngredient.FreshnessStatus freshnessStatus);
    
    // 재료명으로 검색
    @Query("SELECT ri FROM RefrigeratorIngredient ri WHERE ri.refrigerator.refrigeratorId = :refrigeratorId AND ri.ingredient.name LIKE %:ingredientName%")
    List<RefrigeratorIngredient> findByRefrigeratorRefrigeratorIdAndIngredientNameContaining(
        @Param("refrigeratorId") Integer refrigeratorId, 
        @Param("ingredientName") String ingredientName);
} 