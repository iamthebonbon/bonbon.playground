package com.bonbon.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;

public class OrderRepository {

    static final String TABLE_NAME = System.getenv().getOrDefault("ORDERS_TABLE", "orders");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DynamoDbClient dynamoDb;

    public OrderRepository() {
        this.dynamoDb = DynamoDbClient.create();
    }

    OrderRepository(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    public String getOrder(String orderId) {
        try {
            GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of("orderId", AttributeValue.builder().s(orderId).build()))
                    .build());

            ObjectNode result = MAPPER.createObjectNode();
            if (!response.hasItem() || response.item().isEmpty()) {
                result.put("found", false);
                result.put("orderId", orderId);
            } else {
                result.put("found", true);
                response.item().forEach((k, v) -> result.put(k, v.s() != null ? v.s() : v.toString()));
            }
            return result.toString();
        } catch (Exception e) {
            // Stub data when DynamoDB is not configured (PoC fallback)
            return stubOrder(orderId);
        }
    }

    private String stubOrder(String orderId) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("orderId", orderId);
        result.put("status", "SHIPPED");
        result.put("customer", "Jane Doe");
        result.put("items", "3x Widget, 2x Gadget");
        result.put("total", "$89.97");
        result.put("estimatedDelivery", "2026-06-19");
        return result.toString();
    }
}
