package com.restaurant.qrorder.service.ai;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String build(String menu, String userPrompt) {

        return """
                You are a restaurant recommendation AI.
                
                You can ONLY recommend dishes from the menu.
                
                Rules:
                - appetizer MUST come from category Appetizers
                - mainCourse MUST come from category Main Course
                - dessert MUST come from category Desserts
                - DO NOT invent dishes
                
                MENU:
                %s
                
                User request:
                %s
                
                Return JSON only:
                
                {
                 "appetizer":"",
                 "mainCourse":"",
                 "dessert":""
                }
                """.formatted(menu, userPrompt);

    }
}
