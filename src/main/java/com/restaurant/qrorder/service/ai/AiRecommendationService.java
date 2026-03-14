package com.restaurant.qrorder.service.ai;

import com.restaurant.qrorder.domain.dto.request.MealRecommendRequest;
import com.restaurant.qrorder.domain.dto.response.MealResponse;
import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final SemanticSearchService semanticSearchService;
    private final MenuContextBuilder menuContextBuilder;
    private final PromptBuilder promptBuilder;
    private final OllamaService ollamaService;

    public MealResponse recommendMeal(MealRecommendRequest request){

        List<Item> items =
                semanticSearchService.search(request.getPrompt());

        String menu =
                menuContextBuilder.build(items);

        String prompt =
                promptBuilder.build(menu, request.getPrompt());

        String aiResult =
                ollamaService.generate(prompt);

        return mapToMealResponse(aiResult);
    }

    private MealResponse mapToMealResponse(String aiResult){

        MealResponse response = new MealResponse();

        response.setAppetizer(
                extractValue(aiResult, "appetizer"));

        response.setMainCourse(
                extractValue(aiResult, "mainCourse"));

        response.setDessert(
                extractValue(aiResult, "dessert"));

        return response;
    }

    private String extractValue(String json, String key){

        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";

        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(json);

        if(matcher.find()){
            return matcher.group(1);
        }

        return null;
    }
}