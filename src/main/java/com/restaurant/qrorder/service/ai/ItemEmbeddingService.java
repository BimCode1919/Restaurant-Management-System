package com.restaurant.qrorder.service.ai;

import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemEmbeddingService {

    private final ItemRepository itemRepository;
    private final EmbeddingService embeddingService;

    public void generateEmbeddings(){

        List<Item> items = itemRepository.findAllWithCategory();

        for(Item item : items){

            String text = item.getName() + " " +
                    item.getCategory().getName() + " " +
                    item.getDescription();

            List<Double> vector = embeddingService.embed(text);

            item.setEmbedding(vector.toString());

            itemRepository.save(item);
        }
    }
}
