package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BookEntity::class, LoanEntity::class, ConfigEntity::class, BorrowerEntity::class, ReservationEntity::class, ShelfEntity::class, ShelfPositionEntity::class, WeeklySuggestionEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun loanDao(): LoanDao
    abstract fun configDao(): ConfigDao
    abstract fun borrowerDao(): BorrowerDao
    abstract fun reservationDao(): ReservationDao
    abstract fun shelfDao(): ShelfDao
    abstract fun weeklySuggestionDao(): WeeklySuggestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE borrowers ADD COLUMN blockedUntil INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: shelves
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `shelves` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error creating shelves table: ${e.message}")
                }
                
                // Table: shelf_positions
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `shelf_positions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `shelfId` INTEGER NOT NULL, `accessibilityLevel` INTEGER NOT NULL, `coordinate` TEXT NOT NULL, FOREIGN KEY(`shelfId`) REFERENCES `shelves`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_shelf_positions_shelfId` ON `shelf_positions` (`shelfId`)")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error creating shelf_positions table/index: ${e.message}")
                }

                // Alter books table to add JTA and ROOF columns with safety
                val columnsToAlter = listOf(
                    "lastLoanDate" to "INTEGER DEFAULT NULL",
                    "totalLoans" to "INTEGER NOT NULL DEFAULT 0",
                    "useFrequency" to "INTEGER NOT NULL DEFAULT 0",
                    "category" to "TEXT NOT NULL DEFAULT ''",
                    "tipoObra" to "TEXT NOT NULL DEFAULT 'Literário'",
                    "publicoAlvo" to "TEXT NOT NULL DEFAULT 'Geral'",
                    "classificacaoJTA" to "TEXT DEFAULT NULL",
                    "textoJustificativa" to "TEXT DEFAULT NULL",
                    "posicaoEstanteId" to "INTEGER DEFAULT NULL"
                )

                for ((colName, colDef) in columnsToAlter) {
                    try {
                        db.execSQL("ALTER TABLE `books` ADD COLUMN `$colName` $colDef")
                    } catch (e: Exception) {
                        android.util.Log.i("AppDatabase", "Column $colName might already exist: ${e.message}")
                    }
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add photoUri to borrowers table
                try {
                    db.execSQL("ALTER TABLE `borrowers` ADD COLUMN `photoUri` TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "photoUri column might already exist: ${e.message}")
                }
                // Create weekly_suggestions table
                try {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `weekly_suggestions` (" +
                          "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                          "`bookId` INTEGER NOT NULL, " +
                          "`bookTitle` TEXT NOT NULL, " +
                          "`bookAuthor` TEXT NOT NULL, " +
                          "`professorName` TEXT NOT NULL, " +
                          "`classRoom` TEXT NOT NULL, " +
                          "`recommendation` TEXT NOT NULL, " +
                          "`createdAt` INTEGER NOT NULL" +
                        ")"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error creating weekly_suggestions table: ${e.message}")
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safety migration to prevent data loss on schema bump
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `books` ADD COLUMN `isbn` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "isbn column might already exist: ${e.message}")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            return synchronized(this) {
                val tempInstance2 = INSTANCE
                if (tempInstance2 != null) {
                    tempInstance2
                } else {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "school_library_db"
                    )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration(true)
                    .build()

                    try {
                        instance.openHelper.writableDatabase
                        INSTANCE = instance
                        instance
                    } catch (e: Throwable) {
                        android.util.Log.e("AppDatabase", "Schema mismatch or migration error. Rebuilding database clean...", e)
                        try {
                            instance.close()
                        } catch (ex: Exception) {
                            // ignore
                        }
                        try {
                            context.deleteDatabase("school_library_db")
                        } catch (ex: Exception) {
                            // ignore
                        }
                        try {
                            val dbFile = context.getDatabasePath("school_library_db")
                            if (dbFile.exists()) dbFile.delete()
                            val walFile = java.io.File(dbFile.path + "-wal")
                            if (walFile.exists()) walFile.delete()
                            val shmFile = java.io.File(dbFile.path + "-shm")
                            if (shmFile.exists()) shmFile.delete()
                        } catch (ex: Exception) {
                            // ignore
                        }

                        val cleanInstance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "school_library_db"
                        )
                        .fallbackToDestructiveMigration(true)
                        .build()

                        try {
                            cleanInstance.openHelper.writableDatabase
                        } catch (ex: Exception) {
                            // ignore
                        }

                        INSTANCE = cleanInstance
                        cleanInstance
                    }
                }
            }
        }
    }
}
