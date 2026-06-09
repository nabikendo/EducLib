package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_suggestions")
data class WeeklySuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val bookTitle: String,
    val bookAuthor: String,
    val professorName: String,
    val classRoom: String, // e.g. "9º Ano A"
    val recommendation: String,
    val createdAt: Long = System.currentTimeMillis()
)
