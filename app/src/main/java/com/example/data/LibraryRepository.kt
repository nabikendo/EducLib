package com.example.data

import kotlinx.coroutines.flow.Flow
import java.io.File

class LibraryRepository(private val database: AppDatabase) {
    private val bookDao = database.bookDao()
    private val loanDao = database.loanDao()
    private val configDao = database.configDao()
    private val borrowerDao = database.borrowerDao()

    // --- Borrower Operations ---
    val allBorrowers: Flow<List<BorrowerEntity>> = borrowerDao.getAllBorrowers()

    fun searchBorrowers(query: String): Flow<List<BorrowerEntity>> {
        return borrowerDao.searchBorrowers("%$query%")
    }

    suspend fun insertBorrower(borrower: BorrowerEntity): Long {
        return borrowerDao.insertBorrower(borrower)
    }

    suspend fun updateBorrower(borrower: BorrowerEntity) {
        borrowerDao.updateBorrower(borrower)
    }

    suspend fun deleteBorrower(borrower: BorrowerEntity) {
        borrowerDao.deleteBorrower(borrower)
    }

    // --- Book Operations ---
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun searchBooks(query: String): Flow<List<BookEntity>> {
        return bookDao.searchBooks("%$query%")
    }

    suspend fun getBookById(id: Int): BookEntity? {
        return bookDao.getBookById(id)
    }

    suspend fun insertBook(book: BookEntity): Long {
        return bookDao.insertBook(book)
    }

    suspend fun updateBook(book: BookEntity) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: BookEntity) {
        bookDao.deleteBook(book)
    }

    // --- Loan Operations ---
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()
    val activeLoans: Flow<List<LoanEntity>> = loanDao.getActiveLoans()

    suspend fun getLoanById(id: Int): LoanEntity? {
        return loanDao.getLoanById(id)
    }

    /**
     * Attempts to create a new loan. Returns true if successful, false if the book is not available.
     */
    suspend fun createLoan(
        bookId: Int,
        borrowerName: String,
        borrowerClass: String,
        quantity: Int = 1,
        loanDate: Long = System.currentTimeMillis()
    ): Boolean {
        val book = bookDao.getBookById(bookId) ?: return false
        if (book.availableQuantity < quantity) {
            return false // Not enough copies available to loan
        }

        // Create the loan with a default period of 7 days (1 week)
        val dueDate = loanDate + (7L * 24 * 60 * 60 * 1000)
        val loan = LoanEntity(
            bookId = bookId,
            bookTitle = book.title,
            bookAuthor = book.author,
            borrowerName = borrowerName,
            borrowerClass = borrowerClass,
            loanDate = loanDate,
            dueDate = dueDate,
            quantity = quantity
        )

        loanDao.insertLoan(loan)

        // Decrement available quantity by the requested amount
        val updatedBook = book.copy(availableQuantity = book.availableQuantity - quantity)
        bookDao.updateBook(updatedBook)

        return true
    }

    /**
     * Marks a borrowed book as returned.
     */
    suspend fun returnLoan(loanId: Int, returnDate: Long = System.currentTimeMillis()): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.returnDate != null) {
            return false // Already returned
        }

        // Update the loan return date
        val updatedLoan = loan.copy(returnDate = returnDate)
        loanDao.updateLoan(updatedLoan)

        // Increment book available quantity by the borrowed amount
        val book = bookDao.getBookById(loan.bookId)
        if (book != null) {
            // Keep it bounded with totalQuantity
            val newAvailable = (book.availableQuantity + loan.quantity).coerceAtMost(book.totalQuantity)
            val updatedBook = book.copy(availableQuantity = newAvailable)
            bookDao.updateBook(updatedBook)
        }

        return true
    }

    /**
     * Deletes a loan record. If it was active, returns the book copies.
     */
    suspend fun deleteLoan(loan: LoanEntity) {
        loanDao.deleteLoan(loan)
        if (loan.returnDate == null) {
            val book = bookDao.getBookById(loan.bookId)
            if (book != null) {
                val newAvailable = (book.availableQuantity + loan.quantity).coerceAtMost(book.totalQuantity)
                val updatedBook = book.copy(availableQuantity = newAvailable)
                bookDao.updateBook(updatedBook)
            }
        }
    }

    /**
     * Renews a loan, adding 1 additional week (7 days) to its due date.
     */
    suspend fun renewLoan(loanId: Int): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.returnDate != null) {
            return false // Cannot renew returned student/teacher books
        }
        val oneWeekInMillis = 7L * 24 * 60 * 60 * 1000
        val updated = loan.copy(dueDate = loan.dueDate + oneWeekInMillis)
        loanDao.updateLoan(updated)
        return true
    }

    // --- Master Password / Login Operations ---
    suspend fun getMasterPassword(): String? {
        return configDao.getConfig("master_password")?.value
    }

    suspend fun setMasterPassword(password: String) {
        configDao.insertConfig(ConfigEntity("master_password", password))
    }
}
