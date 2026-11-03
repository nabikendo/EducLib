package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklySuggestionDao {
    @Query("SELECT * FROM weekly_suggestions ORDER BY createdAt DESC")
    fun getAllSuggestions(): Flow<List<WeeklySuggestionEntity>>

    @Query("SELECT * FROM weekly_suggestions ORDER BY createdAt DESC")
    suspend fun getAllSuggestionsSnapshot(): List<WeeklySuggestionEntity>

    @Query("DELETE FROM weekly_suggestions")
    suspend fun deleteAllSuggestions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: WeeklySuggestionEntity): Long

    @Delete
    suspend fun deleteSuggestion(suggestion: WeeklySuggestionEntity)
}
