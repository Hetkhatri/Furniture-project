package com.shashank.platform.furnitureecommerceappui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.shashank.platform.furnitureecommerceappui.adapters.AddressAdapter;
import com.shashank.platform.furnitureecommerceappui.models.Address;
import com.shashank.platform.furnitureecommerceappui.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class SavedAddressesActivity extends AppCompatActivity {

    private RecyclerView addressRecyclerView;
    private AddressAdapter addressAdapter;
    private ProgressBar addressProgress;
    private View addressEmptyState;
    private ImageView backButton, addAddressButton;

    private FirebaseHelper firebaseHelper;
    private List<Address> addressList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_addresses);

        firebaseHelper = FirebaseHelper.getInstance();

        addressRecyclerView = findViewById(R.id.address_recycler_view);
        addressProgress = findViewById(R.id.address_progress);
        addressEmptyState = findViewById(R.id.address_empty_state);
        backButton = findViewById(R.id.address_back_button);
        addAddressButton = findViewById(R.id.add_address_button);

        addressAdapter = new AddressAdapter(this, addressList, new AddressAdapter.OnAddressClickListener() {
            @Override
            public void onAddressClick(Address address) {
                // For checkout selection if needed
            }

            @Override
            public void onDeleteClick(Address address) {
                deleteAddress(address);
            }
        });

        addressRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        addressRecyclerView.setAdapter(addressAdapter);

        backButton.setOnClickListener(v -> finish());
        addAddressButton.setOnClickListener(v -> showAddAddressDialog());

        loadAddresses();
    }

    private void loadAddresses() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        addressProgress.setVisibility(View.VISIBLE);
        firebaseHelper.getUserRef(uid).child("addresses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                addressProgress.setVisibility(View.GONE);
                addressList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Address address = ds.getValue(Address.class);
                    if (address != null) {
                        address.setId(ds.getKey());
                        addressList.add(address);
                    }
                }

                if (addressList.isEmpty()) {
                    addressEmptyState.setVisibility(View.VISIBLE);
                    addressRecyclerView.setVisibility(View.GONE);
                } else {
                    addressEmptyState.setVisibility(View.GONE);
                    addressRecyclerView.setVisibility(View.VISIBLE);
                    addressAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                addressProgress.setVisibility(View.GONE);
                Toast.makeText(SavedAddressesActivity.this, "Failed to load addresses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddAddressDialog() {
        if (addressList.size() >= 3) {
            Toast.makeText(this, "Maximum 3 addresses allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null);
        builder.setView(view);

        EditText etTitle = view.findViewById(R.id.et_address_title);
        EditText etStreet = view.findViewById(R.id.et_address_street);
        EditText etCity = view.findViewById(R.id.et_address_city);
        EditText etState = view.findViewById(R.id.et_address_state);
        EditText etZip = view.findViewById(R.id.et_address_zip);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String street = etStreet.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String state = etState.getText().toString().trim();
            String zip = etZip.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(street) || TextUtils.isEmpty(city)) {
                Toast.makeText(SavedAddressesActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Address newAddress = new Address(title, street, city, state, zip);
            saveAddressToFirebase(newAddress);
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void saveAddressToFirebase(Address address) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUserRef(uid).child("addresses").push().setValue(address)
                .addOnSuccessListener(aVoid -> Toast.makeText(SavedAddressesActivity.this, "Address added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SavedAddressesActivity.this, "Failed to add", Toast.LENGTH_SHORT).show());
    }

    private void deleteAddress(Address address) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUserRef(uid).child("addresses").child(address.getId()).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(SavedAddressesActivity.this, "Address deleted", Toast.LENGTH_SHORT).show());
    }
}
