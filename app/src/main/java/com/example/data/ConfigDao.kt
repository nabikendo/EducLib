package com.example.data

import androidx.room.*

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)

    @Query("DELETE FROM configs WHERE `key` = :key")
    suspend fun deleteConfig(key: String)
}
