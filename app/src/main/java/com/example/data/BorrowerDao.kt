package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowerDao {
    @Query("SELECT * FROM borrowers ORDER BY name ASC")
    fun getAllBorrowers(): Flow<List<BorrowerEntity>>

    @Query("SELECT * FROM borrowers WHERE name LIKE :query OR identifier LIKE :query OR classRoom LIKE :query OR type LIKE :query ORDER BY name ASC")
    fun searchBorrowers(query: String): Flow<List<BorrowerEntity>>

    @Query("SELECT * FROM borrowers WHERE name = :name LIMIT 1")
    suspend fun getBorrowerByName(name: String): BorrowerEntity?

    @Query("SELECT * FROM borrowers ORDER BY name ASC")
    suspend fun getAllBorrowersSnapshot(): List<BorrowerEntity>

    @Query("DELETE FROM borrowers")
    suspend fun deleteAllBorrowers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrower(borrower: BorrowerEntity): Long

    @Update
    suspend fun updateBorrower(borrower: BorrowerEntity)

    @Delete
    suspend fun deleteBorrower(borrower: BorrowerEntity)
}
