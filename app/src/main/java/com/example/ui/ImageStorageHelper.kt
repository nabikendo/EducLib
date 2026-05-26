package com.example.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object ImageStorageHelper {
    /**
     * Copies an image from a system Uri to a persistent private app file.
     * Returns the absolute file path, or null if an error occurs.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val directory = File(context.filesDir, "book_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Create unique filename
            val fileName = "book_${System.currentTimeMillis()}.jpg"
            val targetFile = File(directory, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream: OutputStream = FileOutputStream(targetFile)

            if (inputStream == null) return null

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.flush()
            outputStream.close()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
