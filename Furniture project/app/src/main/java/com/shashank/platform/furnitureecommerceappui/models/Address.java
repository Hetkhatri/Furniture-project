package com.shashank.platform.furnitureecommerceappui.models;

public class Address {
    private String id;
    private String label; // e.g., Home, Office
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;

    public Address() {}

    public Address(String id, String label, String fullName, String phone, String addressLine1, String addressLine2, String city, String state, String pincode) {
        this.id = id;
        this.label = label;
        this.fullName = fullName;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }

    public String getFullDisplayAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isEmpty()) sb.append(", ").append(addressLine2);
        sb.append(", ").append(city).append(", ").append(state).append(" - ").append(pincode);
        return sb.toString();
    }
}
