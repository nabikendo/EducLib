package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shelf_positions",
    foreignKeys = [
        ForeignKey(
            entity = ShelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["shelfId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shelfId"])]
)
data class ShelfPositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shelfId: Int,
    val accessibilityLevel: Int, // 1 = Baixa (muito alto/baixo), 2 = Média, 3 = Alta (olhos)
    val coordinate: String // ex: Identificação como "Fileira 2, Prateleira 3"
)
