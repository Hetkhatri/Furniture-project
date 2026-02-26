package com.shashank.platform.furnitureecommerceappui.models;

import com.google.firebase.database.PropertyName;

public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String imageUrl;
    private float rating;
    private int stock;
    private long createdAt;

    // Required empty constructor for Firebase
    public Product() {}

    public Product(String name, String description, double price, String category,
                   String imageUrl, float rating, int stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.stock = stock;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }

    // Improved setter to handle both Number and String types from Firebase
    @PropertyName("price")
    public void setPrice(Object price) {
        if (price instanceof Number) {
            this.price = ((Number) price).doubleValue();
        } else if (price instanceof String) {
            try {
                this.price = Double.parseDouble((String) price);
            } catch (NumberFormatException e) {
                this.price = 0;
            }
        }
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
