package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** Parte Android do backup (arquivos, compartilhamento). O formato e a validação ficam em [BackupFormat]. */
object BackupManager {
    const val CURRENT_FORMAT_VERSION = BackupFormat.CURRENT_FORMAT_VERSION
    const val MAX_BACKUP_BYTES = BackupFormat.MAX_BACKUP_BYTES

    fun toJson(snapshot: BackupSnapshot): String = BackupFormat.toJson(snapshot)

    fun fromJson(json: String): BackupPayload? = BackupFormat.fromJson(json)

    fun preview(json: String): BackupPreview = BackupFormat.preview(json)

    fun validate(payload: BackupPayload): String? = BackupFormat.validate(payload)

    fun readBackupText(context: Context, uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BACKUP_BYTES) { "O arquivo de backup é maior que 10 MB." }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }

    fun saveAutoBackup(context: Context, snapshot: BackupSnapshot) {
        val destination = File(context.filesDir, "library_backup_auto.json")
        val staging = File(context.filesDir, "library_backup_auto.tmp")
        try {
            staging.writeText(toJson(snapshot))
            check(staging.renameTo(destination)) { "Não foi possível substituir o backup automático anterior com segurança." }
        } catch (exception: Exception) {
            staging.delete()
            android.util.Log.e("BackupManager", "Automatic backup failed; retaining the previous backup.", exception)
        }
    }

    fun getShareIntent(context: Context, snapshot: BackupSnapshot): Intent? = runCatching {
        val file = File(context.cacheDir, "EducLib_backup_v2.json")
        file.writeText(toJson(snapshot))
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup do EducLib")
            putExtra(Intent.EXTRA_TEXT, "Cópia dos dados da biblioteca. Este arquivo contém dados de leitores; compartilhe apenas em local privado.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()

    fun getShareBooksCsvIntent(context: Context, books: List<BookEntity>): Intent? = runCatching {
        val sb = StringBuilder()
        sb.append("ID,Titulo,Autor,Genero,Ano,Quantidade Total,Quantidade Disponivel,Localizacao\n")
        books.forEach { book ->
            val title = book.title.replace(",", " ").replace("\"", "")
            val author = book.author.replace(",", " ").replace("\"", "")
            val genre = book.genre.replace(",", " ")
            val location = book.location.replace(",", " ")
            sb.append("${book.id},\"$title\",\"$author\",\"$genre\",${book.year},${book.totalQuantity},${book.availableQuantity},\"$location\"\n")
        }
        val file = File(context.cacheDir, "acervo_livros.csv")
        file.writeText(sb.toString())
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório do Acervo - EducLib")
            putExtra(Intent.EXTRA_TEXT, "Arquivo CSV do acervo de livros.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()
}
