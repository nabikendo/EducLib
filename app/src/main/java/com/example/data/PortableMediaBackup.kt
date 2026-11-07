package com.example.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Arquivo .edulib.zip: manifesto JSON v2, índice de mídia e fotografias locais.
 * URLs remotas e avatares predefinidos continuam como referências; fotos locais são copiadas.
 */
object PortableMediaBackup {
    private const val MAX_TOTAL_BYTES = 200L * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 12L * 1024 * 1024
    private const val MAX_ENTRIES = 1000
    private const val MEDIA_INDEX_NAME = "media-index.json"

    data class Imported(
        val payload: BackupPayload,
        val bookPhotos: Map<Int, File>,
        val borrowerPhotos: Map<Int, File>,
        val stagingDir: File
    )

    fun write(context: Context, snapshot: BackupSnapshot, output: OutputStream) {
        val manifest = BackupManager.toJson(snapshot).toByteArray(Charsets.UTF_8)
        require(manifest.size <= MAX_ENTRY_BYTES) { "O manifesto do backup excede 12 MB." }

        var totalBytes = 0L
        var entryCount = 0
        val bookPhotoIds = mutableListOf<Int>()
        val borrowerPhotoIds = mutableListOf<Int>()

        ZipOutputStream(output.buffered()).use { zip ->
            fun addEntry(name: String, bytes: ByteArray) {
                require(bytes.size <= MAX_ENTRY_BYTES) { "Uma entrada do backup excede 12 MB." }
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "O backup contém arquivos demais." }
                totalBytes += bytes.size
                require(totalBytes <= MAX_TOTAL_BYTES) { "O backup excede o limite de 200 MB." }
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }

            addEntry("manifest.json", manifest)
            snapshot.books.forEach { book ->
                readPhoto(context, book.photoPath, "livro ${book.id}")?.let { bytes ->
                    addEntry("media/books/${book.id}", bytes)
                    bookPhotoIds += book.id
                }
            }
            snapshot.borrowers.forEach { borrower ->
                readPhoto(context, borrower.photoUri, "leitor ${borrower.id}")?.let { bytes ->
                    addEntry("media/borrowers/${borrower.id}", bytes)
                    borrowerPhotoIds += borrower.id
                }
            }
            val index = JSONObject()
                .put("bookPhotoIds", JSONArray(bookPhotoIds))
                .put("borrowerPhotoIds", JSONArray(borrowerPhotoIds))
                .toString()
                .toByteArray(Charsets.UTF_8)
            addEntry(MEDIA_INDEX_NAME, index)
        }
    }

    private fun readPhoto(context: Context, path: String?, owner: String): ByteArray? {
        if (path.isNullOrBlank() || path.startsWith("http://", ignoreCase = true) ||
            path.startsWith("https://", ignoreCase = true) || path.startsWith("avatar:")
        ) return null

        val input = try {
            when {
                path.startsWith("content://", ignoreCase = true) ->
                    context.contentResolver.openInputStream(Uri.parse(path))
                path.startsWith("file://", ignoreCase = true) ->
                    Uri.parse(path).path?.let(::File)?.takeIf { it.isFile }?.inputStream()
                else -> File(path).takeIf { it.isFile }?.inputStream()
            }
        } catch (exception: Exception) {
            throw IllegalStateException("Não foi possível abrir a foto de $owner. Confira o acesso à imagem e tente novamente.", exception)
        } ?: throw IllegalStateException("A foto local de $owner não está mais acessível. Atualize ou remova essa foto antes de exportar.")

        input.use {
            val bytes = readBounded(it, MAX_ENTRY_BYTES)
            require(bytes.isNotEmpty()) { "A foto local de $owner está vazia." }
            return bytes
        }
    }

    fun read(context: Context, uri: Uri): Imported {
        val staging = File(context.cacheDir, "restore_${System.nanoTime()}").apply { mkdirs() }
        try {
            var manifest: String? = null
            var mediaIndex: String? = null
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
                    val allowed = name == "manifest.json" || name == MEDIA_INDEX_NAME ||
                        Regex("media/(books|borrowers)/[0-9]+").matches(name)
                    require(allowed) { "O backup contém uma entrada inválida." }
                    val bytes = readBounded(zip, MAX_ENTRY_BYTES)
                    total += bytes.size
                    require(total <= MAX_TOTAL_BYTES) { "O backup excede o limite de 200 MB." }
                    when {
                        name == "manifest.json" -> {
                            require(manifest == null) { "Manifesto duplicado." }
                            manifest = bytes.toString(Charsets.UTF_8)
                        }
                        name == MEDIA_INDEX_NAME -> {
                            require(mediaIndex == null) { "Índice de mídia duplicado." }
                            mediaIndex = bytes.toString(Charsets.UTF_8)
                        }
                        else -> {
                            val id = name.substringAfterLast('/').toInt()
                            require(id > 0) { "O backup contém um ID de fotografia inválido." }
                            val file = File(staging, name.replace('/', '_'))
                            require(!file.exists()) { "Fotografia duplicada." }
                            file.writeBytes(bytes)
                            if (name.startsWith("media/books/")) books[id] = file else borrowers[id] = file
                        }
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
            mediaIndex?.let { indexJson ->
                val index = JSONObject(indexJson)
                val expectedBookPhotos = parseIds(index.optJSONArray("bookPhotoIds"), "bookPhotoIds")
                val expectedBorrowerPhotos = parseIds(index.optJSONArray("borrowerPhotoIds"), "borrowerPhotoIds")
                require(expectedBookPhotos == books.keys && expectedBorrowerPhotos == borrowers.keys) {
                    "O backup está incompleto: faltam fotografias indicadas no índice."
                }
                require(expectedBookPhotos.all { it in bookIds } && expectedBorrowerPhotos.all { it in borrowerIds }) {
                    "O índice contém fotografias sem cadastro correspondente."
                }
            }
            return Imported(payload, books, borrowers, staging)
        } catch (exception: Exception) {
            staging.deleteRecursively()
            throw exception
        }
    }

    private fun parseIds(values: JSONArray?, field: String): Set<Int> {
        require(values != null) { "Índice de mídia inválido: campo $field ausente." }
        val ids = (0 until values.length()).map { index ->
            values.getInt(index).also { require(it > 0) { "Índice de mídia contém um ID inválido." } }
        }
        require(ids.distinct().size == ids.size) { "Índice de mídia contém IDs duplicados." }
        return ids.toSet()
    }

    fun copyToPermanent(context: Context, imported: Imported): Pair<Map<Int, String>, Map<Int, String>> {
        val directory = File(context.filesDir, "restored_media").apply { mkdirs() }
        val createdFiles = mutableListOf<File>()
        try {
            fun copy(map: Map<Int, File>, prefix: String): Map<Int, String> = map.mapValues { (id, source) ->
                val destination = File(directory, "${prefix}_${id}_${System.nanoTime()}.jpg")
                source.copyTo(destination)
                createdFiles += destination
                destination.absolutePath
            }
            return copy(imported.bookPhotos, "book") to copy(imported.borrowerPhotos, "reader")
        } catch (exception: Exception) {
            createdFiles.forEach { it.delete() }
            throw exception
        }
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
