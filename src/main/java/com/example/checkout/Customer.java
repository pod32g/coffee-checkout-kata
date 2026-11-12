package com.example.checkout;

public class Customer {

    private String id;
    private String name;
    private String tier; // REGULAR, GOLD or PLATINUM
    private int loyaltyPoints;
    private int orders;

    public Customer(String id, String name, String tier, int loyaltyPoints, int orders) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.loyaltyPoints = loyaltyPoints;
        this.orders = orders;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public int getOrders() {
        return orders;
    }

    public void setOrders(int orders) {
        this.orders = orders;
    }
}
