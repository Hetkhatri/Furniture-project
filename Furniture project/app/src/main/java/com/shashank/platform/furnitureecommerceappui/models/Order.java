package com.shashank.platform.furnitureecommerceappui.models;

import java.util.List;

public class Order {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String shippingAddress;
    private String shippingName;
    private String shippingPhone;
    private String phone;
    private List<CartItem> items;
    private double totalPrice;
    private String status; // "pending", "confirmed", "shipped", "delivered", "cancelled"
    private String paymentMethod; // "cod" or "card"
    private long createdAt;

    // Required empty constructor for Firebase
    public Order() {}

    public Order(String userId, String userName, String userEmail, String shippingAddress,
                 String phone, List<CartItem> items, double totalPrice) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.shippingAddress = shippingAddress;
        this.phone = phone;
        this.items = items;
        this.totalPrice = totalPrice;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // Keep old getter/setter for backward compatibility
    public String getUid() { return userId; }
    public void setUid(String uid) { this.userId = uid; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
