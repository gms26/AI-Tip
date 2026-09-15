package com.aitip.service.provider;

public class MockRestaurantProvider implements RestaurantProvider {
    @Override
    public String fetchRestaurantContext(String restaurantName, String location) {
        // Return null or a mock context indicating fallback behavior works
        return null;
    }
}
