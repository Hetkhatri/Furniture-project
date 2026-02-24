package com.shashank.platform.furnitureecommerceappui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

public class SavedAddressesActivity extends AppCompatActivity implements AddressAdapter.OnAddressClickListener {

    private RecyclerView recyclerView;
    private AddressAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ImageView backButton, addAddressButton;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_addresses);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupRecyclerView();
        loadAddresses();

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        addAddressButton.setOnClickListener(v -> showAddAddressDialog());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.address_recycler_view);
        progressBar = findViewById(R.id.address_progress);
        emptyState = findViewById(R.id.address_empty_state);
        backButton = findViewById(R.id.address_back_button);
        addAddressButton = findViewById(R.id.add_address_button);
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAddresses() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        progressBar.setVisibility(View.VISIBLE);
        firebaseHelper.getUsersRef().child(uid).child("addresses")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Address> addresses = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Address address = ds.getValue(Address.class);
                        if (address != null) {
                            address.setId(ds.getKey());
                            addresses.add(address);
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                    adapter.setAddresses(addresses);
                    emptyState.setVisibility(addresses.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressBar.setVisibility(View.GONE);
                }
            });
    }

    private void showAddAddressDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null);
        EditText labelEdit = view.findViewById(R.id.edit_address_label);
        EditText nameEdit = view.findViewById(R.id.edit_address_name);
        EditText phoneEdit = view.findViewById(R.id.edit_address_phone);
        EditText line1Edit = view.findViewById(R.id.edit_address_line1);
        EditText cityEdit = view.findViewById(R.id.edit_address_city);
        EditText stateEdit = view.findViewById(R.id.edit_address_state);
        EditText pinEdit = view.findViewById(R.id.edit_address_pincode);

        new AlertDialog.Builder(this)
            .setTitle("Add New Address")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                String label = labelEdit.getText().toString().trim();
                String name = nameEdit.getText().toString().trim();
                String phone = phoneEdit.getText().toString().trim();
                String line1 = line1Edit.getText().toString().trim();
                String city = cityEdit.getText().toString().trim();
                String state = stateEdit.getText().toString().trim();
                String pin = pinEdit.getText().toString().trim();

                if (label.isEmpty() || name.isEmpty() || phone.isEmpty() || line1.isEmpty() || city.isEmpty() || state.isEmpty() || pin.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveAddressToFirebase(new Address(null, label, name, phone, line1, "", city, state, pin));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveAddressToFirebase(Address address) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUsersRef().child(uid).child("addresses").push().setValue(address);
    }

    @Override
    public void onAddressClick(Address address) {
        // Option to select this address if opened for picking
        if (getIntent().getBooleanExtra("pick_address", false)) {
            // Return address details to checkout
            Intent data = new Intent();
            data.putExtra("address_id", address.getId());
            data.putExtra("address_text", address.getFullDisplayAddress());
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onDeleteClick(Address address) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getUsersRef().child(uid).child("addresses").child(address.getId()).removeValue();
    }
}
