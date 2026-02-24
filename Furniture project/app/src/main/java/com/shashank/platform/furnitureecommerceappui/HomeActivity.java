package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.adapters.ProductAdapter;
import com.shashank.platform.furnitureecommerceappui.models.CartItem;
import com.shashank.platform.furnitureecommerceappui.models.Product;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;
import com.shashank.platform.furnitureecommerceappui.utils.LoadingDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private ProgressBar productsProgress;
    private TextView emptyText;
    private EditText searchEditText;
    private FloatingActionButton favorite, chatbotFab;
    private LinearLayout personLinearLayout;
    private FrameLayout cartIcon;
    private TextView cartBadge, greetingText;
    private LinearLayout recentSearchesContainer, recentSearchesLayout;
    private ImageView sortButton, menuIcon;
    private View cartFooter;
    private TextView footerCartCount, footerCartTotal;
    private Button footerViewCartButton;
    private int currentSortMode = 0; // 0=default, 1=low→high, 2=high→low

    // Category UI
    private CardView categoryAll, categoryChairs, categoryLamps, categoryPlants;
    private TextView countAll, countChairs, countLamps, countPlants;
    private String currentCategory = "All";

    private FirebaseHelper firebaseHelper;
    private ValueEventListener cartBadgeListener;

    // Fallback images for products without URLs
    private final int[] fallbackImages = {
        R.drawable.favorite_img_1, R.drawable.favorite_img_2,
        R.drawable.favorite_img_3, R.drawable.favorite_img_4,
        R.drawable.favorite_img_5, R.drawable.favorite_img_6
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupRecyclerView();
        setupCategoryFilters();
        setupSearch();
        loadSearchHistory();
        setupSortButton();
        setupNavigation();
        loadGreeting();
        loadProducts();
        loadFavorites();
        listenToCartBadge();
    }

    private void initViews() {
        productsRecyclerView = findViewById(R.id.products_recycler_view);
        productsProgress = findViewById(R.id.products_progress);
        emptyText = findViewById(R.id.empty_text);
        searchEditText = findViewById(R.id.search_edit_text);
        favorite = findViewById(R.id.favorite);
        personLinearLayout = findViewById(R.id.person_linear_layout);
        cartIcon = findViewById(R.id.cart_icon);
        cartBadge = findViewById(R.id.cart_badge);
        greetingText = findViewById(R.id.greeting_text);
        recentSearchesContainer = findViewById(R.id.recent_searches_container);
        recentSearchesLayout = findViewById(R.id.recent_searches_layout);
        menuIcon = findViewById(R.id.menu_icon);
        cartFooter = findViewById(R.id.cart_footer_container);
        footerCartCount = findViewById(R.id.footer_cart_count);
        footerCartTotal = findViewById(R.id.footer_cart_total);
        footerViewCartButton = findViewById(R.id.footer_view_cart_button);
        chatbotFab = findViewById(R.id.chatbot_fab);

        categoryAll = findViewById(R.id.category_all);
        categoryChairs = findViewById(R.id.category_chairs);
        categoryLamps = findViewById(R.id.category_lamps);
        categoryPlants = findViewById(R.id.category_plants);

        countAll = findViewById(R.id.category_all_count);
        countChairs = findViewById(R.id.category_chairs_count);
        countLamps = findViewById(R.id.category_lamps_count);
        countPlants = findViewById(R.id.category_plants_count);
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(this, this, fallbackImages);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        productsRecyclerView.setLayoutManager(layoutManager);
        productsRecyclerView.setAdapter(productAdapter);

        // Staggered fall-down animation for items
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
            this, R.anim.layout_fall_down);
        productsRecyclerView.setLayoutAnimation(animation);
    }

    private void setupCategoryFilters() {
        View.OnClickListener categoryClickListener = v -> {
            String category = "All";
            int id = v.getId();
            if (id == R.id.category_all) category = "All";
            else if (id == R.id.category_chairs) category = "Chairs";
            else if (id == R.id.category_lamps) category = "Lamps";
            else if (id == R.id.category_plants) category = "Plants";

            currentCategory = category;
            productAdapter.filterByCategory(category);
            updateEmptyState();
            updateCategoryUI();
        };

        categoryAll.setOnClickListener(categoryClickListener);
        categoryChairs.setOnClickListener(categoryClickListener);
        categoryLamps.setOnClickListener(categoryClickListener);
        categoryPlants.setOnClickListener(categoryClickListener);
        
        updateCategoryUI();
    }

    private void updateCategoryUI() {
        categoryAll.setCardBackgroundColor(currentCategory.equals("All") 
            ? getResources().getColor(R.color.colorPrimaryDark) : android.graphics.Color.WHITE);
        ((TextView)findViewById(R.id.category_all_text)).setTextColor(currentCategory.equals("All") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));
        countAll.setTextColor(currentCategory.equals("All") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));

        categoryChairs.setCardBackgroundColor(currentCategory.equals("Chairs") 
            ? getResources().getColor(R.color.colorPrimaryDark) : android.graphics.Color.WHITE);
        ((TextView)findViewById(R.id.category_chairs_text)).setTextColor(currentCategory.equals("Chairs") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));
        countChairs.setTextColor(currentCategory.equals("Chairs") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));

        categoryLamps.setCardBackgroundColor(currentCategory.equals("Lamps") 
            ? getResources().getColor(R.color.colorPrimaryDark) : android.graphics.Color.WHITE);
        ((TextView)findViewById(R.id.category_lamps_text)).setTextColor(currentCategory.equals("Lamps") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));
        countLamps.setTextColor(currentCategory.equals("Lamps") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));

        categoryPlants.setCardBackgroundColor(currentCategory.equals("Plants") 
            ? getResources().getColor(R.color.colorPrimaryDark) : android.graphics.Color.WHITE);
        ((TextView)findViewById(R.id.category_plants_text)).setTextColor(currentCategory.equals("Plants") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));
        countPlants.setTextColor(currentCategory.equals("Plants") 
            ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#3C3C3C"));
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                productAdapter.filterBySearch(query);
                updateEmptyState();
                
                if (query.isEmpty()) {
                    loadSearchHistory();
                } else {
                    recentSearchesContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSortButton() {
        sortButton = findViewById(R.id.sort_button);
        if (sortButton != null) {
            sortButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, sortButton);
                popup.getMenu().add(0, 0, 0, "Default");
                popup.getMenu().add(0, 1, 1, "Price: Low → High");
                popup.getMenu().add(0, 2, 2, "Price: High → Low");
                popup.setOnMenuItemClickListener(item -> {
                    currentSortMode = item.getItemId();
                    productAdapter.sortByPrice(currentSortMode);
                    updateEmptyState();
                    return true;
                });
                popup.show();
            });
        }
    }

    private void setupNavigation() {
        personLinearLayout.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        favorite.setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cartIcon.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

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

        chatbotFab.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatbotActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void listenToCartBadge() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        cartBadgeListener = new ValueEventListener() {
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
                    cartBadge.setVisibility(View.VISIBLE);
                    cartBadge.setText(String.valueOf(count));
                    updateFooterCart(count, total);
                } else {
                    cartBadge.setVisibility(View.GONE);
                    cartFooter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getCartRef(uid).addValueEventListener(cartBadgeListener);
    }

    private void updateFooterCart(int count, double total) {
        if (cartFooter == null || footerCartCount == null || footerCartTotal == null) return;
        cartFooter.setVisibility(View.VISIBLE);
        footerCartCount.setText(count + (count == 1 ? " item" : " items") + " in cart");
        footerCartTotal.setText(String.format(java.util.Locale.US, "Total: ₹%.2f", total));
    }

    private void loadProducts() {
        productsProgress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        firebaseHelper.getProductsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Product> products = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product != null) {
                        product.setId(snapshot.getKey());
                        products.add(product);
                    }
                }

                productsProgress.setVisibility(View.GONE);

                if (products.isEmpty()) {
                    // No products in database — seed sample data
                    seedAndReload();
                } else {
                    productAdapter.setProducts(products);
                    productAdapter.filterByCategory(currentCategory);
                    productsRecyclerView.scheduleLayoutAnimation();
                    updateEmptyState();
                    loadCategoryCounts(products);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                productsProgress.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this,
                    "Failed to load products: " + databaseError.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadGreeting() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUsersRef().child(uid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    greetingText.setText("Hello, " + name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();

        android.content.SharedPreferences prefs = getSharedPreferences("search_history", MODE_PRIVATE);
        String historyJson = prefs.getString("history", "[]");
        java.util.List<String> history = new java.util.ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(historyJson);
            for (int i = 0; i < array.length(); i++) {
                history.add(array.getString(i));
            }
        } catch (org.json.JSONException e) {}

        // Remove if already exists and add to top
        history.remove(query);
        history.add(0, query);

        // Keep last 5
        if (history.size() > 5) {
            history = history.subList(0, 5);
        }

        org.json.JSONArray newArray = new org.json.JSONArray(history);
        prefs.edit().putString("history", newArray.toString()).apply();
    }

    private void loadSearchHistory() {
        android.content.SharedPreferences prefs = getSharedPreferences("search_history", MODE_PRIVATE);
        String historyJson = prefs.getString("history", "[]");
        try {
            org.json.JSONArray array = new org.json.JSONArray(historyJson);
            if (array.length() > 0) {
                recentSearchesContainer.setVisibility(View.VISIBLE);
                recentSearchesLayout.removeAllViews();
                for (int i = 0; i < array.length(); i++) {
                    String query = array.getString(i);
                    addSearchChip(query);
                }
            } else {
                recentSearchesContainer.setVisibility(View.GONE);
            }
        } catch (org.json.JSONException e) {
            recentSearchesContainer.setVisibility(View.GONE);
        }
    }

    private void addSearchChip(String query) {
        TextView chip = new TextView(this);
        chip.setText(query);
        chip.setTextColor(android.graphics.Color.WHITE);
        chip.setBackgroundResource(R.drawable.white_rounded_shape);
        chip.getBackground().setAlpha(40); // Subtle background
        chip.setPadding(24, 12, 24, 12);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> {
            searchEditText.setText(query);
            searchEditText.setSelection(query.length());
        });
        recentSearchesLayout.addView(chip);
    }

    private void loadCategoryCounts(List<Product> products) {
        int chairs = 0, lamps = 0, plants = 0;
        for (Product p : products) {
            if ("Chairs".equalsIgnoreCase(p.getCategory())) chairs++;
            else if ("Lamps".equalsIgnoreCase(p.getCategory())) lamps++;
            else if ("Plants".equalsIgnoreCase(p.getCategory())) plants++;
        }
        countAll.setText(products.size() + " items");
        countChairs.setText(chairs + " items");
        countLamps.setText(lamps + " items");
        countPlants.setText(plants + " items");
    }

    private void loadFavorites() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getFavoritesRef(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> favoriteIds = new HashSet<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    favoriteIds.add(snapshot.getKey());
                }
                productAdapter.setFavoriteIds(favoriteIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Silent fail for favorites
            }
        });
    }

    private void seedAndReload() {
        firebaseHelper.seedSampleData();
        // The ValueEventListener on products will automatically pick up the seeded data
    }

    private void updateEmptyState() {
        if (productAdapter.getItemCount() == 0) {
            emptyText.setVisibility(View.VISIBLE);
            productsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            productsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ==================== ProductAdapter Callbacks ====================

    @Override
    public void onProductClick(Product product) {
        String query = searchEditText.getText().toString();
        if (!query.isEmpty()) {
            saveSearch(query);
        }
        
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onFavoriteClick(Product product, boolean isFavorite) {
        String query = searchEditText.getText().toString();
        if (!query.isEmpty()) {
            saveSearch(query);
        }

        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavorite) {
            firebaseHelper.addToFavorites(uid, product.getId());
        } else {
            firebaseHelper.removeFromFavorites(uid, product.getId());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up cart badge listener
        String uid = firebaseHelper.getCurrentUserId();
        if (uid != null && cartBadgeListener != null) {
            firebaseHelper.getCartRef(uid).removeEventListener(cartBadgeListener);
        }
    }
}
