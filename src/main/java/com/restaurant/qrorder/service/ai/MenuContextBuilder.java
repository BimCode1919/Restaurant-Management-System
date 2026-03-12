package com.restaurant.qrorder.service.ai;

import com.restaurant.qrorder.domain.entity.Item;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuContextBuilder {

    public String build(List<Item> items){

        StringBuilder sb = new StringBuilder();

        for(Item item : items){

            sb.append(item.getName())
                    .append(" | ")
                    .append(item.getCategory().getName())
                    .append(" | ")
                    .append(item.getPrice())
                    .append(" | ")
                    .append(item.getDescription())
                    .append("\n");

        }

        return sb.toString();
    }
}
