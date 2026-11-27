package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanRulesTest {
    private val book = BookEntity(id = 1, title = "Livro", author = "Autoria", genre = "", year = 2020, location = "", photoPath = null, totalQuantity = 3, availableQuantity = 3)
    private val student = BorrowerEntity(id = 1, name = "Ana", type = "Aluno", identifier = "123", classRoom = "7º A")
    private val teacher = BorrowerEntity(id = 2, name = "Bia", type = "Professor", identifier = "História")
    private fun loan(qty: Int = 2, due: Long = 10 * DAY_MS, returned: Long? = null) =
        LoanEntity(id = 5, bookId = 1, bookTitle = "Livro", bookAuthor = "Autoria", borrowerName = "Ana", borrowerClass = "Aluno - 7º A", loanDate = 0, dueDate = due, returnDate = returned, quantity = qty)

    @Test
    fun loanClassAndDueDateMatchTheAndroidApp() {
        assertEquals("Aluno - 7º A", student.loanClassLabel())
        assertEquals("Prof - Sem Turma", teacher.loanClassLabel())
        assertEquals(7 * DAY_MS, loanDueDate(student.loanClassLabel(), 0, 7, 15))
        assertEquals(15 * DAY_MS, loanDueDate(teacher.loanClassLabel(), 0, 7, 15))
    }

    @Test
    fun refusesUnavailableBlockedOrOverdueBorrowers() {
        assertNull(loanRefusal(book, 2, student, emptyList(), now = 0))
        assertNotNull(loanRefusal(book, 4, student, emptyList(), now = 0))
        assertNotNull(loanRefusal(book, 0, student, emptyList(), now = 0))
        assertNotNull(loanRefusal(book, 1, student.copy(blockedUntil = 100), emptyList(), now = 50))
        assertNotNull(loanRefusal(book, 1, student, listOf(loan(due = 10)), now = 50))
        assertNull(loanRefusal(book, 1, student, listOf(loan(due = 10, returned = 20)), now = 50))
    }

    @Test
    fun partialReturnKeepsRemainderActiveAndRecordsHistory() {
        val partial = loan(qty = 3).applyReturn(requestedQuantity = 1, returnDate = 99)!!
        assertEquals(2, partial.updatedLoan.quantity)
        assertNull(partial.updatedLoan.returnDate)
        assertEquals(1, partial.returnedRecord!!.quantity)
        assertEquals(99L, partial.returnedRecord!!.returnDate)
        assertEquals(0, partial.returnedRecord!!.id)

        val full = loan(qty = 3).applyReturn(requestedQuantity = -1, returnDate = 99)!!
        assertEquals(3, full.returnedQuantity)
        assertNull(full.returnedRecord)
        assertNull(loan(returned = 5).applyReturn(1, 99))
        assertEquals(3, book.copy(availableQuantity = 2).afterReturn(5).availableQuantity)
    }

    @Test
    fun lateReturnBlocksOneDayPerDayLate() {
        val l = loan(due = 10 * DAY_MS)
        assertNull(penaltyBlockedUntil(l, returnDate = 10 * DAY_MS, currentBlockedUntil = null, now = 0))
        assertEquals(13 * DAY_MS + 3 * DAY_MS, penaltyBlockedUntil(l, returnDate = 13 * DAY_MS, currentBlockedUntil = null, now = 13 * DAY_MS))
    }

    @Test
    fun bookWithActiveLoanCannotBeDeleted() {
        assertNotNull(bookDeletionRefusal(book, listOf(loan())))
        assertNull(bookDeletionRefusal(book, listOf(loan(returned = 3))))
    }

    @Test
    fun backupAcceptsReturnedHistoryAndOrphanReservationsButNotActiveOrphanLoans() {
        fun payload(loans: List<LoanEntity>, reservations: List<ReservationEntity> = emptyList()) = BackupPayload(
            formatVersion = BackupFormat.CURRENT_FORMAT_VERSION, books = emptyList(), loans = loans, borrowers = emptyList(),
            reservations = reservations, shelves = emptyList(), shelfPositions = emptyList(), weeklySuggestions = emptyList()
        )
        assertNull(BackupFormat.validate(payload(listOf(loan(returned = 3)))))
        assertNull(BackupFormat.validate(payload(emptyList(), listOf(ReservationEntity(id = 1, bookId = 9, bookTitle = "X", borrowerName = "Ana", borrowerClass = "7º A")))))
        assertTrue(BackupFormat.validate(payload(listOf(loan())))!!.contains("não corresponde"))
    }
}
