package com.aitip.service.provider;

public interface RestaurantProvider {
    /**
     * Fetches metadata for a given restaurant name, optionally scoped by a location/address.
     * @param restaurantName the name of the restaurant
     * @param location the location context (optional)
     * @return a rich context string describing the restaurant, or null if not found
     */
    String fetchRestaurantContext(String restaurantName, String location);
}
