package ac.su.kdt.prompttest.service;

import ac.su.kdt.prompttest.entity.Ingredient;
import ac.su.kdt.prompttest.entity.RecipeIngredient;
import ac.su.kdt.prompttest.repository.IngredientRepository;
import ac.su.kdt.prompttest.repository.RecipeIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeIngredientService {
    
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public void saveRecipeIngredients(Integer recipeId, String ingredientsText) {
        if (ingredientsText == null || ingredientsText.trim().isEmpty()) {
            return;
        }

        // 재료 텍스트 파싱 (예: "돼지고기 300g, 양파 1개, 당근 1개")
        List<IngredientInfo> ingredients = parseIngredients(ingredientsText);
        
        for (IngredientInfo info : ingredients) {
            // 재료 찾기 또는 생성
            Ingredient ingredient = ingredientRepository.findByName(info.name)
                    .orElseGet(() -> createIngredient(info.name, info.amount));
            
            // RecipeIngredient 관계 생성
            RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                    .recipeId(recipeId)
                    .ingredientId(ingredient.getIngredientId())
                    .build();
            
            recipeIngredientRepository.save(recipeIngredient);
        }
    }

    @Transactional(readOnly = true)
    public List<Ingredient> getRecipeIngredients(Integer recipeId) {
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipeId);
        List<Ingredient> ingredients = new ArrayList<>();
        
        for (RecipeIngredient ri : recipeIngredients) {
            ingredientRepository.findById(ri.getIngredientId())
                    .ifPresent(ingredients::add);
        }
        
        return ingredients;
    }

    @Transactional
    public void updateRecipeIngredients(Integer recipeId, String newIngredientsText) {
        // 기존 재료 관계 삭제
        recipeIngredientRepository.deleteByRecipeId(recipeId);
        
        // 새로운 재료 관계 생성
        saveRecipeIngredients(recipeId, newIngredientsText);
    }

    @Transactional
    public void deleteRecipeIngredients(Integer recipeId) {
        recipeIngredientRepository.deleteByRecipeId(recipeId);
    }

    private Ingredient createIngredient(String name, Float amount) {
        Ingredient ingredient = Ingredient.builder()
                .name(name)
                .requiredAmount(amount)
                .build();
        return ingredientRepository.save(ingredient);
    }

    private List<IngredientInfo> parseIngredients(String ingredientsText) {
        List<IngredientInfo> ingredients = new ArrayList<>();
        
        // 정규식 패턴: "재료명 양 단위" 형식 매칭
        Pattern pattern = Pattern.compile("([가-힣a-zA-Z]+)\\s*(\\d+)?\\s*([가-힣a-zA-Z]+)?");
        Matcher matcher = pattern.matcher(ingredientsText);
        
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String amount = matcher.group(2);
            String unit = matcher.group(3);
            
            // 단위를 g 또는 ml로 변환
            Float convertedAmount = convertToStandardUnit(amount, unit);
            
            ingredients.add(new IngredientInfo(
                name,
                convertedAmount,
                unit
            ));
        }
        
        return ingredients;
    }

    private Float convertToStandardUnit(String amount, String unit) {
        if (amount == null) return null;
        
        float value = Float.parseFloat(amount);
        if (unit == null) return value;
        
        // 단위 변환 로직
        switch (unit.trim()) {
            case "g":
            case "ml":
                return value;
            case "kg":
                return value * 1000;
            case "L":
                return value * 1000;
            case "개":
            case "장":
            case "쪽":
                return value; // 기본 단위로 변환하지 않음
            default:
                return value;
        }
    }

    // 재료 정보를 담는 내부 클래스
    private static class IngredientInfo {
        String name;
        Float amount;
        String unit;

        IngredientInfo(String name, Float amount, String unit) {
            this.name = name;
            this.amount = amount;
            this.unit = unit;
        }
    }
} 