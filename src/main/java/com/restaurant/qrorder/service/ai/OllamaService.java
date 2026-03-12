package com.restaurant.qrorder.service.ai;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class OllamaService {

    private final WebClient webClient =
            WebClient.builder()
                    .baseUrl("http://localhost:11434")
                    .build();

    public String generate(String prompt) {

        Map<String,Object> body = Map.of(
                "model","mistral",
                "prompt",prompt,
                "stream",false
        );

        Map result =  webClient.post()
                .uri("/api/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) result.get("response");
    }
}
