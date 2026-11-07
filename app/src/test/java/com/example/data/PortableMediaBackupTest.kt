package com.example.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PortableMediaBackupTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun zipRoundTripRestoresBookAndBorrowerPhotosAndKeepsPresetAvatar() {
        val workDir = File(context.cacheDir, "portable_test_${System.nanoTime()}").apply { mkdirs() }
        val bookPhotoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02, 0xFF.toByte(), 0xD9.toByte())
        val borrowerPhotoBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)
        val bookPhoto = File(workDir, "cover.jpg").apply { writeBytes(bookPhotoBytes) }
        val borrowerPhoto = File(workDir, "reader.jpg").apply { writeBytes(borrowerPhotoBytes) }
        val snapshot = snapshot(bookPhoto.absolutePath, borrowerPhoto.absolutePath)
        val archive = File(workDir, "backup.zip")
        var imported: PortableMediaBackup.Imported? = null
        val createdCopies = mutableListOf<File>()

        try {
            archive.outputStream().use { PortableMediaBackup.write(context, snapshot, it) }
            ZipFile(archive).use { zip ->
                assertTrue(zip.getEntry("manifest.json") != null)
                assertTrue(zip.getEntry("media-index.json") != null)
                assertTrue(zip.getEntry("media/books/1") != null)
                assertTrue(zip.getEntry("media/borrowers/2") != null)
            }

            val restored = PortableMediaBackup.read(context, Uri.fromFile(archive))
            imported = restored
            assertEquals(setOf(1), restored.bookPhotos.keys)
            assertEquals(setOf(2), restored.borrowerPhotos.keys)
            assertEquals("avatar:blue", restored.payload.borrowers?.first { it.id == 3 }?.photoUri)

            val (bookPaths, borrowerPaths) = PortableMediaBackup.copyToPermanent(context, restored)
            createdCopies += bookPaths.values.map(::File)
            createdCopies += borrowerPaths.values.map(::File)
            assertArrayEquals(bookPhotoBytes, File(bookPaths.getValue(1)).readBytes())
            assertArrayEquals(borrowerPhotoBytes, File(borrowerPaths.getValue(2)).readBytes())
        } finally {
            imported?.stagingDir?.deleteRecursively()
            createdCopies.forEach { it.delete() }
            workDir.deleteRecursively()
        }
    }

    @Test
    fun rejectsArchiveWhoseMediaIndexDeclaresAnOmittedPhoto() {
        val workDir = File(context.cacheDir, "portable_incomplete_${System.nanoTime()}").apply { mkdirs() }
        val photo = File(workDir, "cover.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val manifest = BackupManager.toJson(snapshot(photo.absolutePath, null))
        val index = JSONObject()
            .put("bookPhotoIds", JSONArray(listOf(1)))
            .put("borrowerPhotoIds", JSONArray())
            .toString()
        val archive = File(workDir, "incomplete.zip")

        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("media-index.json"))
                zip.write(index.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            val error = runCatching { PortableMediaBackup.read(context, Uri.fromFile(archive)) }.exceptionOrNull()
            assertTrue(error?.message.orEmpty().contains("faltam fotografias"))
        } finally {
            workDir.deleteRecursively()
        }
    }

    @Test
    fun restoresRecordsAndPhotoReferencesInsideRoomTransaction() = runBlocking {
        val workDir = File(context.cacheDir, "portable_room_${System.nanoTime()}").apply { mkdirs() }
        val bookPhoto = File(workDir, "cover.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
        val readerPhoto = File(workDir, "reader.jpg").apply { writeBytes(byteArrayOf(7, 8, 9)) }
        val source = snapshot(bookPhoto.absolutePath, readerPhoto.absolutePath)
        val archive = File(workDir, "backup.zip")
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        var imported: PortableMediaBackup.Imported? = null
        val createdCopies = mutableListOf<File>()

        try {
            archive.outputStream().use { PortableMediaBackup.write(context, source, it) }
            val restored = PortableMediaBackup.read(context, Uri.fromFile(archive))
            imported = restored
            val (bookPaths, borrowerPaths) = PortableMediaBackup.copyToPermanent(context, restored)
            createdCopies += bookPaths.values.map(::File)
            createdCopies += borrowerPaths.values.map(::File)

            LibraryRepository(database).restoreBackup(restored.payload, bookPaths, borrowerPaths)

            val restoredBook = database.bookDao().getAllBooksSnapshot().single()
            val restoredReaders = database.borrowerDao().getAllBorrowersSnapshot().associateBy { it.id }
            assertEquals("Livro de teste", restoredBook.title)
            assertEquals(bookPaths.getValue(1), restoredBook.photoPath)
            assertEquals(borrowerPaths.getValue(2), restoredReaders.getValue(2).photoUri)
            assertEquals("avatar:blue", restoredReaders.getValue(3).photoUri)
        } finally {
            imported?.stagingDir?.deleteRecursively()
            createdCopies.forEach { it.delete() }
            database.close()
            workDir.deleteRecursively()
        }
    }

    @Test
    fun failsExportInsteadOfSilentlyDroppingAnInaccessibleLocalPhoto() {
        val snapshot = snapshot("${context.filesDir}/missing-cover-${System.nanoTime()}.jpg", null)
        val error = runCatching {
            PortableMediaBackup.write(context, snapshot, java.io.ByteArrayOutputStream())
        }.exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("não está mais acessível"))
    }

    private fun snapshot(bookPhotoPath: String?, readerPhotoPath: String?): BackupSnapshot = BackupSnapshot(
        books = listOf(
            BookEntity(
                id = 1,
                title = "Livro de teste",
                author = "Autoria",
                genre = "Ficção",
                year = 2024,
                location = "Estante A",
                photoPath = bookPhotoPath,
                totalQuantity = 1,
                availableQuantity = 1
            )
        ),
        loans = emptyList(),
        borrowers = listOfNotNull(
            readerPhotoPath?.let {
                BorrowerEntity(id = 2, name = "Leitora", type = "Aluno", identifier = "7º A", photoUri = it)
            },
            BorrowerEntity(id = 3, name = "Leitor", type = "Aluno", identifier = "7º B", photoUri = "avatar:blue")
        ),
        reservations = emptyList(),
        shelves = emptyList(),
        shelfPositions = emptyList(),
        weeklySuggestions = emptyList()
    )
}
