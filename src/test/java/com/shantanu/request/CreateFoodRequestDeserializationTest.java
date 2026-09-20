package com.shantanu.request;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateFoodRequestDeserializationTest {

    @Test
    void nullableBooleanInputReachesServiceValidationInsteadOfFailingJsonParsing() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        CreateFoodRequest request = mapper.readValue(
                "{\"categoryId\":2,\"ingredientIds\":[3,4],\"vegetarian\":null,\"seasonal\":null}",
                CreateFoodRequest.class
        );

        assertNull(request.getVegetarian());
        assertNull(request.getSeasonal());
    }

    @Test
    void parsesTheIdOnlyMenuCreationContract() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        CreateFoodRequest request = mapper.readValue("""
                {
                  "name": "Rice bowl",
                  "description": "Fresh vegetables and rice",
                  "price": 250,
                  "categoryId": 2,
                  "restaurantId": 1,
                  "vegetarian": true,
                  "seasonal": false,
                  "ingredientIds": [3, 4],
                  "images": ["https://example.com/food.jpg"]
                }
                """, CreateFoodRequest.class);

        assertEquals(2L, request.getCategoryId());
        assertEquals(2, request.getIngredientIds().size());
        assertEquals(true, request.getVegetarian());
        assertEquals(false, request.getSeasonal());
    }
}
