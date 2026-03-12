package com.restaurant.qrorder.service.ai;

import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.repository.ItemRepository;
import com.restaurant.qrorder.util.SimilarityUtils;
import com.restaurant.qrorder.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final ItemRepository itemRepository;
    private final EmbeddingService embeddingService;

    public List<Item> search(String prompt){

        List<Double> promptVector =
                embeddingService.embed(prompt);

        List<Item> items = itemRepository.findAllWithCategory();

        Map<Item,Double> scores = new HashMap<>();

        for(Item item : items){

            if(item.getEmbedding()==null) continue;

            List<Double> itemVector =
                    VectorUtils.parse(item.getEmbedding());

            double score =
                    SimilarityUtils.cosineSimilarity(
                            promptVector,
                            itemVector
                    );

            scores.put(item,score);
        }

        return scores.entrySet()
                .stream()
                .sorted(Map.Entry.<Item,Double>comparingByValue()
                        .reversed())
                .limit(15)
                .map(Map.Entry::getKey)
                .toList();
    }
}
