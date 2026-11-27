package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShelfIntegrityTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = LibraryRepository(database)
    }

    @After
    fun tearDown() = database.close()

    private fun book(id: Int, positionId: Int?) = BookEntity(
        id = id, title = "Livro $id", author = "Autoria", genre = "Ficção", year = 2024,
        location = "", photoPath = null, totalQuantity = 1, availableQuantity = 1, posicaoEstanteId = positionId
    )

    private suspend fun seedShelfWithBook(): Pair<ShelfEntity, ShelfPositionEntity> {
        val shelf = ShelfEntity(id = 1, name = "Infantil")
        database.shelfDao().insertShelf(shelf)
        val position = ShelfPositionEntity(id = 10, shelfId = 1, accessibilityLevel = 3, coordinate = "Prateleira 2")
        database.shelfDao().insertPosition(position)
        database.bookDao().insertBook(book(1, positionId = 10))
        database.bookDao().insertBook(book(2, positionId = null))
        return shelf to position
    }

    @Test
    fun deletingShelfDetachesBooksAndKeepsBackupRestorable() = runBlocking {
        val (shelf, _) = seedShelfWithBook()

        repository.deleteShelf(shelf)

        assertTrue(database.bookDao().getAllBooksSnapshot().all { it.posicaoEstanteId == null })
        assertEquals(2, database.bookDao().getAllBooksSnapshot().size)
        val json = BackupManager.toJson(repository.createBackupSnapshot())
        assertNull(BackupManager.validate(BackupManager.fromJson(json)!!))
    }

    @Test
    fun deletingPositionDetachesOnlyItsBooks() = runBlocking {
        val (_, position) = seedShelfWithBook()
        val other = ShelfPositionEntity(id = 11, shelfId = 1, accessibilityLevel = 2, coordinate = "Prateleira 3")
        database.shelfDao().insertPosition(other)
        database.bookDao().insertBook(book(3, positionId = 11))

        repository.deletePosition(position)

        val byId = database.bookDao().getAllBooksSnapshot().associateBy { it.id }
        assertNull(byId.getValue(1).posicaoEstanteId)
        assertEquals(11, byId.getValue(3).posicaoEstanteId)
    }

    @Test
    fun backupWithOrphanPositionLinkFromOlderVersionsRestoresWithoutTheLink() = runBlocking {
        val payload = BackupPayload(
            formatVersion = BackupManager.CURRENT_FORMAT_VERSION,
            books = listOf(book(1, positionId = 42)),
            loans = emptyList(),
            borrowers = emptyList(),
            reservations = emptyList(),
            shelves = emptyList(),
            shelfPositions = emptyList(),
            weeklySuggestions = emptyList()
        )
        assertNull(BackupManager.validate(payload))

        repository.restoreBackup(payload)

        assertNull(database.bookDao().getAllBooksSnapshot().single().posicaoEstanteId)
    }

    @Test
    fun shelfLayoutSurvivesJsonAndLegacyShelvesGetDefaults() {
        val measured = ShelfEntity(id = 1, name = "A", mapX = 1.5, mapZ = -2.0, rotationDeg = 90.0, widthM = 2.4, levelCount = 6, columnCount = 3, layoutSource = "ar")
        val snapshot = BackupSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), listOf(measured), emptyList(), emptyList())
        val restored = BackupManager.fromJson(BackupManager.toJson(snapshot))!!
        assertEquals(measured, restored.shelves!!.single())

        val legacyJson = """
            {"formatVersion":2,"books":[],"loans":[],"borrowers":[],"reservations":[],
             "shelves":[{"id":7,"name":"Antiga"}],"shelfPositions":[],"weeklySuggestions":[]}
        """.trimIndent()
        val legacy = BackupManager.fromJson(legacyJson)!!
        val shelf = legacy.shelves!!.single()
        assertNull(shelf.mapX)
        assertEquals(ShelfEntity.DEFAULT_LEVEL_COUNT, shelf.levelCount)
        assertNull(BackupManager.validate(legacy))
    }

    @Test
    fun deletingABookKeepsReturnedHistoryDropsReservationsAndKeepsBackupRestorable() = runBlocking {
        database.bookDao().insertBook(book(1, positionId = null))
        database.bookDao().insertBook(book(2, positionId = null))
        database.loanDao().insertLoan(LoanEntity(id = 1, bookId = 1, bookTitle = "Livro 1", bookAuthor = "Autoria", borrowerName = "Ana", borrowerClass = "Aluno - 7º A", loanDate = 0, dueDate = 1, returnDate = 2))
        database.loanDao().insertLoan(LoanEntity(id = 2, bookId = 2, bookTitle = "Livro 2", bookAuthor = "Autoria", borrowerName = "Ana", borrowerClass = "Aluno - 7º A", loanDate = 0, dueDate = 1))
        database.reservationDao().insertReservation(ReservationEntity(id = 1, bookId = 1, bookTitle = "Livro 1", borrowerName = "Bia", borrowerClass = "8º B"))

        assertNull(repository.deleteBook(book(1, positionId = null)))
        assertNotNull("livro emprestado não pode ser excluído", repository.deleteBook(book(2, positionId = null)))

        assertEquals(listOf(2), database.bookDao().getAllBooksSnapshot().map { it.id })
        assertEquals(2, database.loanDao().getAllLoansSnapshot().size)
        assertTrue(database.reservationDao().getAllReservationsSnapshot().isEmpty())
        val json = BackupManager.toJson(repository.createBackupSnapshot())
        assertNull(BackupManager.validate(BackupManager.fromJson(json)!!))
    }

    @Test
    fun rejectsShelvesWithImpossibleMeasurements() {
        val payload = BackupPayload(
            formatVersion = BackupManager.CURRENT_FORMAT_VERSION,
            books = emptyList(), loans = emptyList(), borrowers = emptyList(), reservations = emptyList(),
            shelves = listOf(ShelfEntity(id = 1, name = "X", levelCount = 0)),
            shelfPositions = emptyList(), weeklySuggestions = emptyList()
        )
        assertNotNull(BackupManager.validate(payload))
    }
}
