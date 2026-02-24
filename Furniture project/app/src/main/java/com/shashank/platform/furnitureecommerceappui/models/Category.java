package com.shashank.platform.furnitureecommerceappui.models;

public class Category {
    private String id;
    private String name;
    private String iconUrl;

    // Required empty constructor for Firebase
    public Category() {}

    public Category(String name, String iconUrl) {
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
