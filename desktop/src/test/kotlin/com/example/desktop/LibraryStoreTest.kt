package com.example.desktop

import com.example.data.BackupFormat
import com.example.data.BackupSnapshot
import com.example.data.BookEntity
import com.example.data.BorrowerEntity
import com.example.data.DAY_MS
import com.example.data.ShelfEntity
import com.example.data.ShelfPositionEntity
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryStoreTest {
    private val dir: Path = Files.createTempDirectory("educlib-store")
    private var now = 1_000L * DAY_MS
    private fun store() = LibraryStore(dir.resolve("biblioteca.json")) { now }

    @AfterTest
    fun cleanUp() = dir.toFile().deleteRecursively().let { }

    private fun book(total: Int = 2) = BookEntity(title = "Dom Casmurro", author = "Machado de Assis", genre = "Romance", year = 1899, location = "", photoPath = null, totalQuantity = total, availableQuantity = total)

    @Test
    fun lendReturnAndPersistAcrossRestarts() {
        val s = store()
        val bookId = s.saveBook(book())
        val ana = s.saveBorrower(BorrowerEntity(name = "Ana", type = "Aluno", identifier = "1", classRoom = "7º A"))
        s.lend(bookId, ana, 2)
        assertEquals(0, s.data.value.books.single().availableQuantity)
        val loan = s.data.value.loans.single()
        assertEquals("Aluno - 7º A", loan.borrowerClass)
        assertEquals(now + 7 * DAY_MS, loan.dueDate)

        s.returnLoan(loan.id, 1) // devolução parcial
        assertEquals(1, s.data.value.books.single().availableQuantity)
        assertEquals(2, s.data.value.loans.size)

        val reopened = store()
        assertEquals(s.data.value, reopened.data.value)
        // O arquivo de dados é um backup EducLib válido.
        assertNull(BackupFormat.validate(BackupFormat.fromJson(Files.readString(dir.resolve("biblioteca.json")))!!))
    }

    @Test
    fun lateReturnBlocksReaderAndBlockedReaderCannotBorrow() {
        val s = store()
        val bookId = s.saveBook(book())
        val ana = s.saveBorrower(BorrowerEntity(name = "Ana", type = "Aluno", identifier = "1"))
        s.lend(bookId, ana)
        now += 10 * DAY_MS // 3 dias de atraso
        s.returnLoan(s.data.value.loans.single().id)
        assertEquals(now + 3 * DAY_MS, s.data.value.borrowers.single().blockedUntil)
        assertFailsWith<StoreException> { s.lend(bookId, ana) }
        s.clearBlock(ana)
        s.lend(bookId, ana)
    }

    @Test
    fun cannotDeleteLentBookOrReaderButHistorySurvives() {
        val s = store()
        val bookId = s.saveBook(book())
        val ana = s.saveBorrower(BorrowerEntity(name = "Ana", type = "Aluno", identifier = "1"))
        s.lend(bookId, ana)
        assertFailsWith<StoreException> { s.deleteBook(bookId) }
        assertFailsWith<StoreException> { s.deleteBorrower(ana) }
        s.returnLoan(s.data.value.loans.single().id)
        s.deleteBook(bookId)
        assertTrue(s.data.value.books.isEmpty())
        assertEquals(1, s.data.value.loans.size)
        assertNull(BackupFormat.validate(BackupFormat.fromJson(s.exportJson())!!))
    }

    @Test
    fun totalCannotDropBelowCopiesOnLoan() {
        val s = store()
        val bookId = s.saveBook(book(total = 3))
        val ana = s.saveBorrower(BorrowerEntity(name = "Ana", type = "Aluno", identifier = "1"))
        s.lend(bookId, ana, 2)
        assertEquals(3, s.availableFor(bookId, 5))
        assertFailsWith<StoreException> { s.availableFor(bookId, 1) }
    }

    @Test
    fun duplicateReaderNamesAreRejectedBecauseLoansLinkByName() {
        val s = store()
        s.saveBorrower(BorrowerEntity(name = "Ana", type = "Aluno", identifier = "1"))
        assertFailsWith<StoreException> { s.saveBorrower(BorrowerEntity(name = "ana", type = "Aluno", identifier = "2")) }
    }

    @Test
    fun deletingShelfDetachesBooks() {
        val s = store()
        val shelf = s.saveShelf(ShelfEntity(name = "Infantil"))
        val pos = s.addPosition(shelf, "Prateleira 2", 3)
        val bookId = s.saveBook(book())
        s.placeBook(bookId, pos)
        s.deleteShelf(shelf)
        assertNull(s.data.value.books.single().posicaoEstanteId)
        assertTrue(s.data.value.positions.isEmpty())
    }

    @Test
    fun importsAnAndroidBackupAndDropsOrphanPositionLinks() {
        val android = BackupSnapshot(
            books = listOf(book().copy(id = 7, posicaoEstanteId = 99), book().copy(id = 8, title = "Iracema", posicaoEstanteId = 3)),
            loans = emptyList(),
            borrowers = listOf(BorrowerEntity(id = 4, name = "Ana", type = "Aluno", identifier = "1")),
            reservations = emptyList(),
            shelves = listOf(ShelfEntity(id = 2, name = "A", mapX = 1.0, mapZ = 2.0, layoutSource = "ar")),
            shelfPositions = listOf(ShelfPositionEntity(id = 3, shelfId = 2, accessibilityLevel = 3, coordinate = "Prateleira 1")),
            weeklySuggestions = emptyList()
        )
        val payload = BackupFormat.fromJson(BackupFormat.toJson(android))!!
        val s = store()
        s.saveBook(book()) // dados atuais serão substituídos
        s.importBackup(payload)
        val books = s.data.value.books.associateBy { it.id }
        assertEquals(setOf(7, 8), books.keys)
        assertNull(books.getValue(7).posicaoEstanteId)
        assertEquals(3, books.getValue(8).posicaoEstanteId)
        assertEquals("ar", s.data.value.shelves.single().layoutSource)
        // Novos registros continuam a numeração do backup.
        assertEquals(9, s.saveBook(book()))
    }

    @Test
    fun corruptDataFileIsReportedNotSilentlyReset() {
        Files.writeString(dir.resolve("biblioteca.json"), "{ isto não é json")
        val error = assertFailsWith<StoreException> { store() }
        assertNotNull(error.message)
        assertTrue(Files.readString(dir.resolve("biblioteca.json")).startsWith("{ isto"))
    }
}
