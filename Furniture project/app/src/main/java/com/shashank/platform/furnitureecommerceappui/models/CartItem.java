package com.shashank.platform.furnitureecommerceappui.models;

public class CartItem {
    private String productId;
    private String productName;
    private String productImage;
    private double productPrice;
    private int quantity;
    private long addedAt;

    // Required empty constructor for Firebase
    public CartItem() {}

    public CartItem(String productId, String productName, String productImage,
                    double productPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.addedAt = System.currentTimeMillis();
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getAddedAt() { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }

    public double getTotalPrice() {
        return productPrice * quantity;
    }
}
