package com.example.reminders

import com.example.data.LoanEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class LoanDueSummaryTest {
    @Test
    fun countsDueTodayAndOverdueWithoutCountingReturnedLoans() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dueToday = startToday + 12 * 60 * 60 * 1000L
        val overdue = startToday - 24 * 60 * 60 * 1000L
        val dueTomorrow = startToday + 36 * 60 * 60 * 1000L
        val loans = listOf(
            loan(id = 1, dueDate = dueToday),
            loan(id = 2, dueDate = overdue),
            loan(id = 3, dueDate = dueTomorrow),
            loan(id = 4, dueDate = overdue, returnDate = now)
        )

        val result = summarizeActiveLoans(loans, now)

        assertEquals(1, result.dueToday)
        assertEquals(1, result.overdue)
        assertEquals(1, result.upcoming)
    }

    private fun loan(id: Int, dueDate: Long, returnDate: Long? = null) = LoanEntity(
        id = id,
        bookId = id,
        bookTitle = "Livro $id",
        bookAuthor = "Autoria",
        borrowerName = "Leitor",
        borrowerClass = "Turma",
        loanDate = dueDate - 7 * 24 * 60 * 60 * 1000L,
        dueDate = dueDate,
        returnDate = returnDate
    )
}
