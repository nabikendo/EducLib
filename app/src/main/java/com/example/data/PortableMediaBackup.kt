package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Arquivo .edulib.zip: manifest.json (formato JSON v2) e fotografias locais.
 * URLs remotas não são incluídas. Mantém compatibilidade com backups JSON existentes.
 */
object PortableMediaBackup {
    private const val MAX_TOTAL_BYTES = 200L * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 12L * 1024 * 1024
    private const val MAX_ENTRIES = 1000

    data class Imported(
        val payload: BackupPayload,
        val bookPhotos: Map<Int, File>,
        val borrowerPhotos: Map<Int, File>,
        val stagingDir: File
    )

    fun write(context: Context, snapshot: BackupSnapshot, output: OutputStream) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(BackupManager.toJson(snapshot).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val names = mutableSetOf<String>()
            snapshot.books.forEach { book ->
                addPhoto(context, zip, book.photoPath, "media/books/${book.id}", names)
            }
            snapshot.borrowers.forEach { borrower ->
                addPhoto(context, zip, borrower.photoUri, "media/borrowers/${borrower.id}", names)
            }
        }
    }

    private fun addPhoto(context: Context, zip: ZipOutputStream, path: String?, name: String, names: MutableSet<String>) {
        if (path.isNullOrBlank() || path.startsWith("http://") || path.startsWith("https://")) return
        val stream = runCatching {
            if (path.startsWith("content://")) context.contentResolver.openInputStream(Uri.parse(path))
            else File(path.removePrefix("file://")).takeIf { it.isFile }?.inputStream()
        }.getOrNull() ?: return
        stream.use { input ->
            val data = readBounded(input, MAX_ENTRY_BYTES)
            if (data.isEmpty() || !names.add(name)) return
            zip.putNextEntry(ZipEntry(name))
            zip.write(data)
            zip.closeEntry()
        }
    }

    fun read(context: Context, uri: Uri): Imported {
        val staging = File(context.cacheDir, "restore_${System.nanoTime()}").apply { mkdirs() }
        try {
            var manifest: String? = null
            var total = 0L
            var count = 0
            val books = mutableMapOf<Int, File>()
            val borrowers = mutableMapOf<Int, File>()
            val source = context.contentResolver.openInputStream(uri) ?: error("Arquivo inacessível.")
            ZipInputStream(source.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count++
                    require(count <= MAX_ENTRIES) { "O backup contém arquivos demais." }
                    require(!entry.isDirectory) { "O backup contém uma pasta inesperada." }
                    val name = entry.name
                    val allowed = name == "manifest.json" ||
                        Regex("media/(books|borrowers)/[0-9]+").matches(name)
                    require(allowed) { "O backup contém uma entrada inválida." }
                    val bytes = readBounded(zip, MAX_ENTRY_BYTES)
                    total += bytes.size
                    require(total <= MAX_TOTAL_BYTES) { "O backup excede o limite de 200 MB." }
                    if (name == "manifest.json") {
                        require(manifest == null) { "Manifesto duplicado." }
                        manifest = bytes.toString(Charsets.UTF_8)
                    } else {
                        val id = name.substringAfterLast('/').toInt()
                        val file = File(staging, name.replace('/', '_'))
                        require(!file.exists()) { "Fotografia duplicada." }
                        file.writeBytes(bytes)
                        if (name.startsWith("media/books/")) books[id] = file else borrowers[id] = file
                    }
                    zip.closeEntry()
                }
            }
            val payload = BackupManager.fromJson(manifest ?: error("Manifesto ausente."))
                ?: error("Manifesto inválido.")
            require(BackupManager.validate(payload) == null) { "Dados do backup inválidos." }
            val bookIds = payload.books.orEmpty().map { it.id }.toSet()
            val borrowerIds = payload.borrowers.orEmpty().map { it.id }.toSet()
            require(books.keys.all { it in bookIds } && borrowers.keys.all { it in borrowerIds }) {
                "O backup contém fotografias sem cadastro correspondente."
            }
            return Imported(payload, books, borrowers, staging)
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }
    }

    fun copyToPermanent(context: Context, imported: Imported): Pair<Map<Int, String>, Map<Int, String>> {
        val directory = File(context.filesDir, "restored_media").apply { mkdirs() }
        fun copy(map: Map<Int, File>, prefix: String): Map<Int, String> = map.mapValues { (id, source) ->
            val destination = File(directory, "${prefix}_${id}_${System.nanoTime()}.jpg")
            source.copyTo(destination)
            destination.absolutePath
        }
        return copy(imported.bookPhotos, "book") to copy(imported.borrowerPhotos, "reader")
    }

    private fun readBounded(input: InputStream, max: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            count += n
            require(count <= max) { "Fotografia ou manifesto excede o limite permitido." }
            output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }
}
