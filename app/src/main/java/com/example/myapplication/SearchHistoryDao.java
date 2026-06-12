package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SearchHistoryDao {

    @Insert
    void insert(SearchHistory searchHistory);

    @Query("SELECT * FROM search_history WHERE userEmail = :email ORDER BY timestamp DESC")
    List<SearchHistory> getHistoryByUser(String email);

    @Query("DELETE FROM search_history WHERE userEmail = :email")
    void clearHistory(String email);
}