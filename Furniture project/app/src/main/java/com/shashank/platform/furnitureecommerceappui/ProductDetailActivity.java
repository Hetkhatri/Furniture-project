package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;
import com.shashank.platform.furnitureecommerceappui.models.Product;
import com.shashank.platform.furnitureecommerceappui.models.Review;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;
import com.shashank.platform.furnitureecommerceappui.utils.NetworkUtils;
import com.shashank.platform.furnitureecommerceappui.adapters.ProductAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView productImage, backButton, favoriteButton, shareButton;
    private TextView productName, productPrice, productCategory, productDescription;
    private TextView qtyText, stockText;
    private Button qtyMinus, qtyPlus, addToCartButton;
    private ProgressBar detailProgress;

    // Footer Cart
    private View cartFooter;
    private TextView footerCartCount, footerCartTotal;
    private Button footerViewCartButton;

    // Reviews
    private LinearLayout reviewsContainer;
    private TextView avgRatingText, noReviewsText;
    private Button writeReviewButton;

    private RecyclerView similarProductsRecyclerView;
    private ProductAdapter similarProductAdapter;

    private FirebaseHelper firebaseHelper;
    private Product currentProduct;
    private String productId;
    private int quantity = 1;
    private boolean isFavorite = false;

    // Fallback images
    private final int[] fallbackImages = {
        R.drawable.favorite_img_1, R.drawable.favorite_img_2,
        R.drawable.favorite_img_3, R.drawable.favorite_img_4,
        R.drawable.favorite_img_5, R.drawable.favorite_img_6
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        firebaseHelper = FirebaseHelper.getInstance();
        initViews();
        setupListeners();

        productId = getIntent().getStringExtra("product_id");
        if (productId != null) {
            loadProduct(productId);
            checkFavoriteStatus(productId);
            loadReviews(productId);
        }
    }

    private void initViews() {
        productImage = findViewById(R.id.detail_product_image);
        backButton = findViewById(R.id.detail_back_button);
        favoriteButton = findViewById(R.id.detail_favorite_button);
        productName = findViewById(R.id.detail_product_name);
        productPrice = findViewById(R.id.detail_product_price);
        productCategory = findViewById(R.id.detail_product_category);
        productDescription = findViewById(R.id.detail_product_description);
        qtyText = findViewById(R.id.detail_qty_text);
        stockText = findViewById(R.id.detail_stock_text);
        qtyMinus = findViewById(R.id.detail_qty_minus);
        qtyPlus = findViewById(R.id.detail_qty_plus);
        addToCartButton = findViewById(R.id.detail_add_to_cart);
        detailProgress = findViewById(R.id.detail_progress);

        // Footer Cart
        cartFooter = findViewById(R.id.cart_footer_container);
        footerCartCount = findViewById(R.id.footer_cart_count);
        footerCartTotal = findViewById(R.id.footer_cart_total);
        footerViewCartButton = findViewById(R.id.footer_view_cart_button);

        // Reviews
        reviewsContainer = findViewById(R.id.reviews_container);
        avgRatingText = findViewById(R.id.detail_avg_rating);
        noReviewsText = findViewById(R.id.no_reviews_text);
        writeReviewButton = findViewById(R.id.write_review_button);
        shareButton = findViewById(R.id.detail_share_button);
        similarProductsRecyclerView = findViewById(R.id.similar_products_recycler_view);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        qtyMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                qtyText.setText(String.valueOf(quantity));
            }
        });

        qtyPlus.setOnClickListener(v -> {
            if (currentProduct != null && quantity < currentProduct.getStock()) {
                quantity++;
                qtyText.setText(String.valueOf(quantity));
            }
        });

        addToCartButton.setOnClickListener(v -> {
            if (!NetworkUtils.checkAndNotify(this)) return;
            addToCart();
        });

        favoriteButton.setOnClickListener(v -> toggleFavorite());

        writeReviewButton.setOnClickListener(v -> {
            if (!NetworkUtils.checkAndNotify(this)) return;
            showWriteReviewDialog();
        });

        shareButton.setOnClickListener(v -> shareProduct());

        if (cartFooter != null) {
            cartFooter.setOnClickListener(v -> {
                startActivity(new Intent(this, CartActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        if (footerViewCartButton != null) {
            footerViewCartButton.setOnClickListener(v -> {
                startActivity(new Intent(this, CartActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        setupSimilarProductsRecyclerView();
        listenToCartFooter();
    }

    private void setupSimilarProductsRecyclerView() {
        similarProductAdapter = new ProductAdapter(this, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(ProductDetailActivity.this, ProductDetailActivity.class);
                intent.putExtra("product_id", product.getId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onFavoriteClick(Product product, boolean isFavorite) {
                // Handle favorite click if desired
            }
        }, fallbackImages);

        similarProductsRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        similarProductsRecyclerView.setAdapter(similarProductAdapter);
    }

    private void shareProduct() {
        if (currentProduct == null) return;
        String shareText = "Check out this product!\n\n"
            + "🪑 " + currentProduct.getName() + "\n"
            + "💰 ₹" + String.format(java.util.Locale.US, "%.2f", currentProduct.getPrice()) + "\n"
            + "📦 Category: " + currentProduct.getCategory() + "\n\n"
            + currentProduct.getDescription() + "\n\n"
            + "— Shared from Furniture Store App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentProduct.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Product via"));
    }

    private void loadProduct(String productId) {
        detailProgress.setVisibility(View.VISIBLE);

        firebaseHelper.getProductRef(productId).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    detailProgress.setVisibility(View.GONE);
                    currentProduct = snapshot.getValue(Product.class);
                    if (currentProduct != null) {
                        currentProduct.setId(snapshot.getKey());
                        displayProduct();
                    } else {
                        Toast.makeText(ProductDetailActivity.this,
                            "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    detailProgress.setVisibility(View.GONE);
                    Toast.makeText(ProductDetailActivity.this,
                        "Error loading product", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void displayProduct() {
        productName.setText(currentProduct.getName());
        productPrice.setText(String.format(Locale.US, "₹%.2f", currentProduct.getPrice()));
        productCategory.setText(currentProduct.getCategory());
        productDescription.setText(currentProduct.getDescription());

        loadSimilarProducts(currentProduct.getCategory());

        if (currentProduct.getStock() > 0) {
            stockText.setText(currentProduct.getStock() + " in stock");
            stockText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            stockText.setText("Out of stock");
            stockText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            addToCartButton.setEnabled(false);
            addToCartButton.setText("Out of Stock");
        }

        // Load image
        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
            Glide.with(this).load(currentProduct.getImageUrl()).centerCrop().into(productImage);
        } else {
            int idx = Math.abs(currentProduct.getName().hashCode()) % fallbackImages.length;
            productImage.setImageResource(fallbackImages[idx]);
        }
    }

    // ==================== REVIEWS ====================

    private void loadReviews(String productId) {
        firebaseHelper.getReviewsRef(productId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    reviewsContainer.removeAllViews();
                    float totalRating = 0;
                    int count = 0;

                    for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                        Review review = reviewSnap.getValue(Review.class);
                        if (review != null) {
                            addReviewView(review);
                            totalRating += review.getRating();
                            count++;
                        }
                    }

                    if (count > 0) {
                        float avg = totalRating / count;
                        avgRatingText.setText(String.format(Locale.US, "★ %.1f (%d)", avg, count));
                        noReviewsText.setVisibility(View.GONE);
                    } else {
                        avgRatingText.setText("");
                        noReviewsText.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void addReviewView(Review review) {
        LinearLayout reviewRow = new LinearLayout(this);
        reviewRow.setOrientation(LinearLayout.VERTICAL);
        reviewRow.setPadding(0, 12, 0, 12);

        // Header: user name + rating + date
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(this);
        nameView.setText(review.getUserName() != null ? review.getUserName() : "Anonymous");
        nameView.setTextColor(Color.parseColor("#333333"));
        nameView.setTextSize(14);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        nameView.setLayoutParams(nameParams);

        TextView ratingView = new TextView(this);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < (int) review.getRating(); i++) stars.append("★");
        for (int i = (int) review.getRating(); i < 5; i++) stars.append("☆");
        ratingView.setText(stars.toString());
        ratingView.setTextColor(Color.parseColor("#FF9800"));
        ratingView.setTextSize(14);

        header.addView(nameView);
        header.addView(ratingView);

        // Date
        TextView dateView = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        dateView.setText(sdf.format(new Date(review.getCreatedAt())));
        dateView.setTextColor(Color.parseColor("#999999"));
        dateView.setTextSize(12);

        // Comment
        TextView commentView = new TextView(this);
        commentView.setText(review.getComment());
        commentView.setTextColor(Color.parseColor("#555555"));
        commentView.setTextSize(14);
        commentView.setPadding(0, 4, 0, 0);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#eeeeee"));

        reviewRow.addView(header);
        reviewRow.addView(dateView);
        if (review.getComment() != null && !review.getComment().isEmpty()) {
            reviewRow.addView(commentView);
        }

        reviewsContainer.addView(reviewRow);
        reviewsContainer.addView(divider);
    }

    private void loadSimilarProducts(String category) {
        if (category == null) return;

        firebaseHelper.getProductsRef().orderByChild("category").equalTo(category).limitToFirst(10)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Product> products = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Product product = snapshot.getValue(Product.class);
                        if (product != null) {
                            product.setId(snapshot.getKey());
                            // Exclude current product
                            if (!product.getId().equals(productId)) {
                                products.add(product);
                            }
                        }
                    }
                    similarProductAdapter.setProducts(products);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
    }

    private void showWriteReviewDialog() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in to write a review", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 0);

        // RatingBar
        TextView ratingLabel = new TextView(this);
        ratingLabel.setText("Your Rating:");
        ratingLabel.setTextSize(16);
        ratingLabel.setTextColor(Color.BLACK);
        layout.addView(ratingLabel);

        RatingBar ratingBar = new RatingBar(this, null,
            android.R.attr.ratingBarStyleSmall);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);
        ratingBar.setRating(5);
        ratingBar.setIsIndicator(false);
        // Use a larger style
        RatingBar ratingBarFull = new RatingBar(this);
        ratingBarFull.setNumStars(5);
        ratingBarFull.setStepSize(1);
        ratingBarFull.setRating(5);
        layout.addView(ratingBarFull);

        // Comment
        EditText commentInput = new EditText(this);
        commentInput.setHint("Write your review (optional)");
        commentInput.setMinLines(3);
        commentInput.setGravity(Gravity.TOP);
        layout.addView(commentInput);

        new AlertDialog.Builder(this)
            .setTitle("Write a Review")
            .setView(layout)
            .setPositiveButton("Submit", (dialog, which) -> {
                float rating = ratingBarFull.getRating();
                String comment = commentInput.getText().toString().trim();

                if (rating < 1) {
                    Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitReview(uid, rating, comment);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void submitReview(String uid, float rating, String comment) {
        // Get user name
        firebaseHelper.getUserRef(uid).child("name")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String userName = snapshot.getValue(String.class);
                    if (userName == null) userName = "Anonymous";

                    Review review = new Review(uid, userName, rating, comment);
                    firebaseHelper.getReviewsRef(productId).push().setValue(review)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ProductDetailActivity.this,
                                "Review submitted!", Toast.LENGTH_SHORT).show();
                            loadReviews(productId); // Refresh
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ProductDetailActivity.this,
                                "Failed to submit review", Toast.LENGTH_SHORT).show();
                        });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    // ==================== CART ====================

    private void addToCart() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentProduct == null) return;

        addToCartButton.setEnabled(false);

        // Check if item already exists in cart
        firebaseHelper.getCartRef(uid).child(currentProduct.getId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Item already in cart — ask user
                        CartItem existingItem = snapshot.getValue(CartItem.class);
                        int existingQty = existingItem != null ? existingItem.getQuantity() : 0;
                        int newQty = existingQty + quantity;

                        // Check stock
                        if (newQty > currentProduct.getStock()) {
                            addToCartButton.setEnabled(true);
                            new AlertDialog.Builder(ProductDetailActivity.this)
                                .setTitle("Stock Limit")
                                .setMessage("You already have " + existingQty
                                    + " in your cart. Only " + currentProduct.getStock()
                                    + " available in stock.")
                                .setPositiveButton("OK", null)
                                .show();
                            return;
                        }

                        new AlertDialog.Builder(ProductDetailActivity.this)
                            .setTitle("Already in Cart")
                            .setMessage("This item is already in your cart (qty: " + existingQty
                                + "). Add " + quantity + " more?")
                            .setPositiveButton("Add More", (dialog, which) -> {
                                CartItem updatedItem = new CartItem(
                                    currentProduct.getId(),
                                    currentProduct.getName(),
                                    currentProduct.getImageUrl() != null ? currentProduct.getImageUrl() : "",
                                    currentProduct.getPrice(),
                                    newQty
                                );
                                firebaseHelper.addToCart(uid, updatedItem);
                                addToCartButton.setEnabled(true);
                                Toast.makeText(ProductDetailActivity.this,
                                    "Cart updated! (qty: " + newQty + ")",
                                    Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                addToCartButton.setEnabled(true);
                            })
                            .setCancelable(false)
                            .show();
                    } else {
                        // New item — check stock and add
                        if (quantity > currentProduct.getStock()) {
                            addToCartButton.setEnabled(true);
                            Toast.makeText(ProductDetailActivity.this,
                                "Only " + currentProduct.getStock() + " available in stock",
                                Toast.LENGTH_SHORT).show();
                            return;
                        }

                        CartItem cartItem = new CartItem(
                            currentProduct.getId(),
                            currentProduct.getName(),
                            currentProduct.getImageUrl() != null ? currentProduct.getImageUrl() : "",
                            currentProduct.getPrice(),
                            quantity
                        );
                        firebaseHelper.addToCart(uid, cartItem);
                        addToCartButton.setEnabled(true);
                        Toast.makeText(ProductDetailActivity.this,
                            "Added to cart!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    addToCartButton.setEnabled(true);
                    Toast.makeText(ProductDetailActivity.this,
                        "Error checking cart", Toast.LENGTH_SHORT).show();
                }
            });
    }

    // ==================== FAVORITES ====================

    private void toggleFavorite() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null || currentProduct == null) return;

        isFavorite = !isFavorite;
        favoriteButton.setImageResource(
            isFavorite ? R.drawable.ic_favorite_red_24dp : R.drawable.ic_favorite_white_24dp);

        if (isFavorite) {
            firebaseHelper.addToFavorites(uid, currentProduct.getId());
        } else {
            firebaseHelper.removeFromFavorites(uid, currentProduct.getId());
        }
    }

    private void checkFavoriteStatus(String productId) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getFavoriteItemRef(uid, productId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isFavorite = snapshot.exists();
                    favoriteButton.setImageResource(
                        isFavorite ? R.drawable.ic_favorite_red_24dp
                                   : R.drawable.ic_favorite_white_24dp);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }
    private void listenToCartFooter() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getCartRef(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                double total = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);
                    if (item != null) {
                        count += item.getQuantity();
                        total += item.getProductPrice() * item.getQuantity();
                    }
                }

                if (count > 0) {
                    updateFooterCart(count, total);
                } else {
                    if (cartFooter != null) cartFooter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateFooterCart(int count, double total) {
        if (cartFooter == null || footerCartCount == null || footerCartTotal == null) return;
        cartFooter.setVisibility(View.VISIBLE);
        footerCartCount.setText(count + (count == 1 ? " item" : " items") + " in cart");
        footerCartTotal.setText(String.format(java.util.Locale.US, "Total: ₹%.2f", total));
    }
}
