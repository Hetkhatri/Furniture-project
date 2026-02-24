package com.shashank.platform.furnitureecommerceappui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shashank.platform.furnitureecommerceappui.R;
import com.shashank.platform.furnitureecommerceappui.models.Address;

import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private List<Address> addresses = new ArrayList<>();
    private final OnAddressClickListener listener;

    public interface OnAddressClickListener {
        void onAddressClick(Address address);
        void onDeleteClick(Address address);
    }

    public AddressAdapter(OnAddressClickListener listener) {
        this.listener = listener;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addresses.get(position);
        holder.label.setText(address.getLabel());
        holder.fullAddress.setText(address.getFullDisplayAddress());

        holder.itemView.setOnClickListener(v -> listener.onAddressClick(address));
        holder.deleteIcon.setOnClickListener(v -> listener.onDeleteClick(address));
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView label, fullAddress;
        ImageView deleteIcon;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.item_address_label);
            fullAddress = itemView.findViewById(R.id.item_address_full);
            deleteIcon = itemView.findViewById(R.id.item_address_delete);
        }
    }
}
