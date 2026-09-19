package com.lostfound.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    private int itemId;
    private int userId;          
    private String itemName;
    private String category;
    private ItemType type;       
    private String description;
    private String location;
    private LocalDate itemDate;  
    private String imageUrl;     
    private ItemStatus status;   
    private LocalDateTime createdAt;

    
    public Item(int itemId, int userId, String itemName, String category, ItemType type,
                String description, String location, LocalDate itemDate, String imageUrl,
                ItemStatus status, LocalDateTime createdAt) {
        this.itemId = itemId;
        this.userId = userId;
        this.itemName = itemName;
        this.category = category;
        this.type = type;
        this.description = description;
        this.location = location;
        this.itemDate = itemDate;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    





    public Item(int userId, String itemName, String category, ItemType type,
                String description, String location, LocalDate itemDate) {
        this.userId = userId;
        this.itemName = itemName;
        this.category = category;
        this.type = type;
        this.description = description;
        this.location = location;
        this.itemDate = itemDate;
        this.status = (type == ItemType.LOST) ? ItemStatus.LOST : ItemStatus.FOUND;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getItemDate() {
        return itemDate;
    }

    public void setItemDate(LocalDate itemDate) {
        this.itemDate = itemDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return itemId == item.itemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", category='" + category + '\'' +
                ", type=" + type +
                ", location='" + location + '\'' +
                ", itemDate=" + itemDate +
                ", status=" + status +
                '}';
    }
}
