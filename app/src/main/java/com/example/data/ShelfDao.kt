package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {
    @Query("SELECT * FROM shelves ORDER BY name ASC")
    fun getAllShelves(): Flow<List<ShelfEntity>>

    @Query("SELECT * FROM shelves WHERE id = :id")
    suspend fun getShelfById(id: Int): ShelfEntity?

    @Query("SELECT * FROM shelf_positions ORDER BY shelfId ASC, coordinate ASC")
    fun getAllPositionsFlow(): Flow<List<ShelfPositionEntity>>

    @Query("SELECT * FROM shelf_positions")
    suspend fun getAllPositions(): List<ShelfPositionEntity>

    @Query("SELECT * FROM shelf_positions WHERE shelfId = :shelfId ORDER BY coordinate ASC")
    fun getPositionsForShelfFlow(shelfId: Int): Flow<List<ShelfPositionEntity>>

    @Query("SELECT * FROM shelf_positions WHERE shelfId = :shelfId ORDER BY coordinate ASC")
    suspend fun getPositionsForShelf(shelfId: Int): List<ShelfPositionEntity>

    @Query("SELECT * FROM shelf_positions WHERE id = :id")
    suspend fun getPositionById(id: Int): ShelfPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity): Long

    @Update
    suspend fun updateShelf(shelf: ShelfEntity)

    @Delete
    suspend fun deleteShelf(shelf: ShelfEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: ShelfPositionEntity): Long

    @Update
    suspend fun updatePosition(position: ShelfPositionEntity)

    @Delete
    suspend fun deletePosition(position: ShelfPositionEntity)
}
