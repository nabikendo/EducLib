package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val genre: String,
    val year: Int,
    val location: String,
    val photoPath: String?, // Local file path of the saved photo
    val totalQuantity: Int,
    val availableQuantity: Int
)
