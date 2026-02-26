package com.shashank.platform.furnitureecommerceappui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.adapters.ProductAdapter;
import com.shashank.platform.furnitureecommerceappui.models.Product;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private RecyclerView favoritesRecyclerView;
    private ProgressBar favoritesProgress;
    private View favoritesEmpty;
    private LinearLayout homeLinearLayout, profileLinearLayout;
    private ProductAdapter adapter;
    private FirebaseHelper firebaseHelper;

    private final int[] fallbackImages = {
        R.drawable.favorite_img_1, R.drawable.favorite_img_2,
        R.drawable.favorite_img_3, R.drawable.favorite_img_4,
        R.drawable.favorite_img_5, R.drawable.favorite_img_6
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        firebaseHelper = FirebaseHelper.getInstance();

        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);
        favoritesProgress = findViewById(R.id.favorites_progress);
        favoritesEmpty = findViewById(R.id.favorites_empty);
        homeLinearLayout = findViewById(R.id.home_linear_layout);
        
        // Find profileLinearLayout correctly from the bottom bar
        View bottomBar = findViewById(R.id.bottom_nav_bar);
        if (bottomBar instanceof LinearLayout) {
            profileLinearLayout = (LinearLayout) ((LinearLayout)bottomBar).getChildAt(1);
        } else {
            // Fallback if ID not found directly
            View parent = (View) homeLinearLayout.getParent();
            if (parent instanceof LinearLayout) {
                profileLinearLayout = (LinearLayout) ((LinearLayout)parent).getChildAt(1);
            }
        }

        adapter = new ProductAdapter(this, this, fallbackImages);
        favoritesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        favoritesRecyclerView.setAdapter(adapter);

        homeLinearLayout.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        if (profileLinearLayout != null) {
            profileLinearLayout.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            });
        }

        Button browseButton = findViewById(R.id.empty_fav_shop_button);
        if (browseButton != null) {
            browseButton.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            });
        }

        loadFavorites();
    }

    private void loadFavorites() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) {
            if (favoritesEmpty != null) {
                favoritesEmpty.setVisibility(View.VISIBLE);
                TextView emptyText = favoritesEmpty.findViewById(R.id.favorites_empty_text);
                if (emptyText != null) {
                    emptyText.setText("Please log in to see your favourites");
                }
            }
            return;
        }

        favoritesProgress.setVisibility(View.VISIBLE);

        firebaseHelper.getFavoritesRef(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> favoriteIds = new HashSet<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    favoriteIds.add(snapshot.getKey());
                }

                if (favoriteIds.isEmpty()) {
                    favoritesProgress.setVisibility(View.GONE);
                    if (favoritesEmpty != null) {
                        favoritesEmpty.setVisibility(View.VISIBLE);
                    }
                    favoritesRecyclerView.setVisibility(View.GONE);
                    return;
                }

                // Now fetch each favorite product
                loadFavoriteProducts(favoriteIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                favoritesProgress.setVisibility(View.GONE);
                Toast.makeText(FavoritesActivity.this,
                    "Failed to load favourites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFavoriteProducts(Set<String> favoriteIds) {
        firebaseHelper.getProductsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                favoritesProgress.setVisibility(View.GONE);
                List<Product> favoriteProducts = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (favoriteIds.contains(snapshot.getKey())) {
                        Product product = snapshot.getValue(Product.class);
                        if (product != null) {
                            product.setId(snapshot.getKey());
                            favoriteProducts.add(product);
                        }
                    }
                }

                if (favoriteProducts.isEmpty()) {
                    if (favoritesEmpty != null) {
                        favoritesEmpty.setVisibility(View.VISIBLE);
                    }
                    favoritesRecyclerView.setVisibility(View.GONE);
                } else {
                    if (favoritesEmpty != null) {
                        favoritesEmpty.setVisibility(View.GONE);
                    }
                    favoritesRecyclerView.setVisibility(View.VISIBLE);
                    adapter.setProducts(favoriteProducts);
                    adapter.setFavoriteIds(favoriteIds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                favoritesProgress.setVisibility(View.GONE);
                Toast.makeText(FavoritesActivity.this,
                    "Failed to load products", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Product product, boolean isFavorite) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        if (isFavorite) {
            firebaseHelper.addToFavorites(uid, product.getId());
        } else {
            firebaseHelper.removeFromFavorites(uid, product.getId());
        }
    }
}
