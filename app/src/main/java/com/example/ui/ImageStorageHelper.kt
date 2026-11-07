package com.example.ui

import android.content.Context
import android.net.Uri
import java.io.File

object ImageStorageHelper {
    /** Copies a selected image into persistent private app storage and returns its absolute path. */
    fun saveImageToInternalStorage(
        context: Context,
        uri: Uri,
        directoryName: String = "book_photos",
        filePrefix: String = "book"
    ): String? = runCatching {
        val directory = File(context.filesDir, directoryName).apply {
            check(isDirectory || mkdirs()) { "Não foi possível preparar o armazenamento de fotos." }
        }
        val targetFile = File(directory, "${filePrefix}_${System.nanoTime()}.jpg")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Não foi possível abrir a imagem selecionada.")
            input.use { source ->
                targetFile.outputStream().use { target -> source.copyTo(target) }
            }
            targetFile.absolutePath
        } catch (exception: Exception) {
            targetFile.delete()
            throw exception
        }
    }.getOrElse { exception ->
        android.util.Log.w("ImageStorageHelper", "Could not persist selected image.", exception)
        null
    }
}
