package com.shashank.platform.furnitureecommerceappui.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;
import com.shashank.platform.furnitureecommerceappui.models.Category;
import com.shashank.platform.furnitureecommerceappui.models.Order;
import com.shashank.platform.furnitureecommerceappui.models.Product;
import com.shashank.platform.furnitureecommerceappui.models.User;

/**
 * Centralized Firebase helper class providing easy access to all database references.
 */
public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;

    // Database node names
    public static final String NODE_USERS = "users";
    public static final String NODE_PRODUCTS = "products";
    public static final String NODE_CATEGORIES = "categories";
    public static final String NODE_CART = "cart";
    public static final String NODE_ORDERS = "orders";
    public static final String NODE_FAVORITES = "favorites";
    public static final String NODE_REVIEWS = "reviews";

    private FirebaseHelper() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ==================== AUTH ====================
    public FirebaseAuth getAuth() { return auth; }

    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ==================== DATABASE REFERENCES ====================
    public DatabaseReference getUsersRef() {
        return database.getReference(NODE_USERS);
    }

    public DatabaseReference getUserRef(String uid) {
        return database.getReference(NODE_USERS).child(uid);
    }

    public DatabaseReference getProductsRef() {
        return database.getReference(NODE_PRODUCTS);
    }

    public DatabaseReference getProductRef(String productId) {
        return database.getReference(NODE_PRODUCTS).child(productId);
    }

    public DatabaseReference getCategoriesRef() {
        return database.getReference(NODE_CATEGORIES);
    }

    public DatabaseReference getCartRef(String uid) {
        return database.getReference(NODE_CART).child(uid);
    }

    public DatabaseReference getCartItemRef(String uid, String productId) {
        return database.getReference(NODE_CART).child(uid).child(productId);
    }

    public DatabaseReference getOrdersRef() {
        return database.getReference(NODE_ORDERS);
    }

    public DatabaseReference getUserOrdersRef(String uid) {
        return database.getReference(NODE_ORDERS);
        // To query: getUserOrdersRef().orderByChild("uid").equalTo(uid)
    }

    public DatabaseReference getFavoritesRef(String uid) {
        return database.getReference(NODE_FAVORITES).child(uid);
    }

    public DatabaseReference getFavoriteItemRef(String uid, String productId) {
        return database.getReference(NODE_FAVORITES).child(uid).child(productId);
    }

    public DatabaseReference getReviewsRef(String productId) {
        return database.getReference(NODE_REVIEWS).child(productId);
    }

    // ==================== STORAGE ====================
    public StorageReference getProductImagesRef() {
        return storage.getReference("product_images");
    }

    public StorageReference getProfileImagesRef() {
        return storage.getReference("profile_images");
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Save a user profile to the database
     */
    public void saveUser(User user) {
        getUserRef(user.getUid()).setValue(user);
    }

    /**
     * Add a product (generates a new key)
     */
    public String addProduct(Product product) {
        DatabaseReference newRef = getProductsRef().push();
        product.setId(newRef.getKey());
        newRef.setValue(product);
        return newRef.getKey();
    }

    /**
     * Update an existing product
     */
    public void updateProduct(Product product) {
        getProductRef(product.getId()).setValue(product);
    }

    /**
     * Delete a product
     */
    public void deleteProduct(String productId) {
        getProductRef(productId).removeValue();
    }

    /**
     * Add or update a cart item
     */
    public void addToCart(String uid, CartItem item) {
        getCartItemRef(uid, item.getProductId()).setValue(item);
    }

    /**
     * Remove a cart item
     */
    public void removeFromCart(String uid, String productId) {
        getCartItemRef(uid, productId).removeValue();
    }

    /**
     * Clear entire cart for a user
     */
    public void clearCart(String uid) {
        getCartRef(uid).removeValue();
    }

    /**
     * Toggle a product in favorites
     */
    public void addToFavorites(String uid, String productId) {
        getFavoriteItemRef(uid, productId).setValue(true);
    }

    public void removeFromFavorites(String uid, String productId) {
        getFavoriteItemRef(uid, productId).removeValue();
    }

    /**
     * Place an order (generates a new key)
     */
    public String placeOrder(Order order) {
        DatabaseReference newRef = getOrdersRef().push();
        order.setId(newRef.getKey());
        newRef.setValue(order);
        return newRef.getKey();
    }

    /**
     * Update order status (admin action)
     */
    public void updateOrderStatus(String orderId, String status) {
        getOrdersRef().child(orderId).child("status").setValue(status);
    }

    // ==================== ORDER CALLBACK ====================
    public interface OrderCallback {
        void onSuccess(String orderId);
        void onError(String error);
    }

    /**
     * Save an order with callback
     */
    public void saveOrder(String uid, Order order, OrderCallback callback) {
        DatabaseReference newRef = getOrdersRef().push();
        String orderId = newRef.getKey();
        order.setId(orderId);
        newRef.setValue(order)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess(orderId);
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    /**
     * Seed initial product data — call once to populate the database
     */
    public void seedSampleData() {
        DatabaseReference productsRef = getProductsRef();

        // Only seed if products don't already exist (checked by caller)
        Product[] products = {
            new Product("Modern Wooden Chair", "Elegant wooden chair with comfortable cushion. Perfect for living room or dining area.", 129.99, "Chairs", "", 4.5f, 25),
            new Product("Minimalist Desk Lamp", "Sleek desk lamp with adjustable arm and warm LED light. Great for home office.", 59.99, "Lamps", "", 4.2f, 40),
            new Product("Indoor Cactus Plant", "Low-maintenance indoor cactus in a beautiful ceramic pot. Adds green to any room.", 34.99, "Plants", "", 4.7f, 60),
            new Product("Leather Armchair", "Premium leather armchair with ergonomic design. Available in brown and black.", 349.99, "Chairs", "", 4.8f, 10),
            new Product("Hanging Pendant Light", "Modern pendant light with brass finish. Perfect for kitchen islands and dining tables.", 89.99, "Lamps", "", 4.3f, 30),
            new Product("Monstera Plant", "Beautiful Monstera deliciosa in handcrafted wooden planter. Tropical vibes for your home.", 44.99, "Plants", "", 4.6f, 45),
            new Product("Scandinavian Bar Stool", "Minimalist bar stool with natural wood legs and padded seat. Set of 2.", 199.99, "Chairs", "", 4.4f, 20),
            new Product("Floor Standing Lamp", "Tall floor lamp with fabric shade. Creates warm ambient lighting.", 119.99, "Lamps", "", 4.1f, 15),
            new Product("Glass Coffee Table", "Tempered glass coffee table with oak wood legs. Modern and durable.", 259.99, "Tables", "", 4.5f, 12),
            new Product("Bookshelf Unit", "5-tier bookshelf with industrial metal frame and wood shelves.", 179.99, "Storage", "", 4.3f, 18),
        };

        for (Product product : products) {
            String key = productsRef.push().getKey();
            product.setId(key);
            productsRef.child(key).setValue(product);
        }

        // Seed categories
        DatabaseReference categoriesRef = getCategoriesRef();
        String[] categoryNames = {"All", "Chairs", "Lamps", "Plants", "Tables", "Storage"};
        for (String catName : categoryNames) {
            String key = categoriesRef.push().getKey();
            Category category = new Category(catName, "");
            category.setId(key);
            categoriesRef.child(key).setValue(category);
        }
    }
}
