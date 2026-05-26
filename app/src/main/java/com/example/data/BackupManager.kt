package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class BackupPayload(
    val books: List<BookEntity>?,
    val loans: List<LoanEntity>?
)

object BackupManager {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val adapter = moshi.adapter(BackupPayload::class.java)

    /**
     * Converts books and loans to a JSON string.
     */
    fun toJson(books: List<BookEntity>, loans: List<LoanEntity>): String {
        val payload = BackupPayload(books, loans)
        return adapter.toJson(payload)
    }

    /**
     * Parses a JSON string to a BackupPayload of books and loans.
     */
    fun fromJson(json: String): BackupPayload? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Automatically saves a local backup file on device cache for redundancy.
     */
    fun saveAutoBackup(context: Context, books: List<BookEntity>, loans: List<LoanEntity>) {
        try {
            val json = toJson(books, loans)
            val file = File(context.filesDir, "library_backup_auto.json")
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exports the current state into a JSON file and returns a share intent.
     */
    fun getShareIntent(context: Context, books: List<BookEntity>, loans: List<LoanEntity>): Intent? {
        return try {
            val json = toJson(books, loans)
            val file = File(context.cacheDir, "biblioteca_escola_backup.json")
            file.writeText(json)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup - Biblioteca Escolar")
                putExtra(Intent.EXTRA_TEXT, "Segue em anexo o arquivo de backup (.json) da Biblioteca Escolar.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
