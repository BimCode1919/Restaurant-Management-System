package com.restaurant.qrorder.service.ai;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient =
            WebClient.builder()
                    .baseUrl("http://localhost:11434")
                    .build();

    public List<Double> embed(String text){

        Map<String,Object> body = Map.of(
                "model","mistral",
                "prompt",text
        );

        Map result = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (List<Double>) result.get("embedding");
    }
}
