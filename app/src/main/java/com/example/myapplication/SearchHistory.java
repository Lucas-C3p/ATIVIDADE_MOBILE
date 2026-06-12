package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistory {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String city;
    public String userEmail;
    public long timestamp;

    public SearchHistory(String city, String userEmail, long timestamp) {
        this.city = city;
        this.userEmail = userEmail;
        this.timestamp = timestamp;
    }
}