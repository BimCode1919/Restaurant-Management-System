package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.MealRecommendRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.MealResponse;
import com.restaurant.qrorder.service.ai.AiRecommendationService;
import com.restaurant.qrorder.service.ai.ItemEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiRecommendationService aiRecommendationService;
    private final ItemEmbeddingService itemEmbeddingService;

    @PostMapping("/recommend-meal")
    public ResponseEntity<ApiResponse<MealResponse>> recommend(
            @RequestBody MealRecommendRequest request){

        return ResponseEntity.ok(
                ApiResponse.<MealResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("AI recommendation generated")
                        .data(aiRecommendationService.recommendMeal(request))
                        .build());
    }

    @PostMapping("/generate-embeddings")
    public ResponseEntity<ApiResponse<String>> generate(){

        itemEmbeddingService.generateEmbeddings();

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Embeddings generated")
                        .build());
    }
}
