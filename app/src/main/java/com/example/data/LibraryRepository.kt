package com.example.data

import kotlinx.coroutines.flow.Flow
import java.io.File

class LibraryRepository(private val database: AppDatabase) {
    private val bookDao = database.bookDao()
    private val loanDao = database.loanDao()
    private val configDao = database.configDao()
    private val borrowerDao = database.borrowerDao()
    private val reservationDao = database.reservationDao()
    private val shelfDao = database.shelfDao()
    private val weeklySuggestionDao = database.weeklySuggestionDao()

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

        val studentLoanDays = configDao.getConfig("student_loan_days")?.value?.toLongOrNull() ?: 7L
        val teacherLoanDays = configDao.getConfig("teacher_loan_days")?.value?.toLongOrNull() ?: 15L
        val defaultDays = if (borrowerClass.startsWith("Prof", ignoreCase = true)) teacherLoanDays else studentLoanDays
        val dueDate = loanDate + (defaultDays * 24 * 60 * 60 * 1000)
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

        // Decrement available quantity by the requested amount and update usage statistics
        val updatedBook = book.copy(
            availableQuantity = book.availableQuantity - quantity,
            totalLoans = book.totalLoans + quantity,
            useFrequency = book.useFrequency + quantity,
            lastLoanDate = loanDate
        )
        bookDao.updateBook(updatedBook)

        return true
    }

    /**
     * Marks a borrowed book as returned (fully or partially).
     */
    suspend fun returnLoan(
        loanId: Int,
        returnedQuantity: Int = -1,
        returnDate: Long = System.currentTimeMillis()
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.returnDate != null) {
            return false // Already returned
        }

        val actualReturned = if (returnedQuantity <= 0 || returnedQuantity >= loan.quantity) {
            loan.quantity
        } else {
            returnedQuantity
        }

        if (actualReturned == loan.quantity) {
            // Full return
            val updatedLoan = loan.copy(returnDate = returnDate)
            loanDao.updateLoan(updatedLoan)
        } else {
            // Partial return!
            // Update the quantity of the current active loan
            val updatedLoan = loan.copy(quantity = loan.quantity - actualReturned)
            loanDao.updateLoan(updatedLoan)

            // Insert a new LoanEntity for the returned ones so we keep history
            val historicReturnedLoan = loan.copy(
                id = 0, // autoGenerate new ID
                quantity = actualReturned,
                returnDate = returnDate
            )
            loanDao.insertLoan(historicReturnedLoan)
        }

        // Increment book available quantity by the returned amount
        val book = bookDao.getBookById(loan.bookId)
        if (book != null) {
            // Keep it bounded with totalQuantity
            val newAvailable = (book.availableQuantity + actualReturned).coerceAtMost(book.totalQuantity)
            val updatedBook = book.copy(availableQuantity = newAvailable)
            bookDao.updateBook(updatedBook)
        }

        // Apply block logic if late
        if (returnDate > loan.dueDate) {
            val overdueDays = ((returnDate - loan.dueDate) / (1000 * 60 * 60 * 24)).toLong()
            if (overdueDays > 0) {
                // To fetch borrower we might need borrowerDao
                val borrower = borrowerDao.getBorrowerByName(loan.borrowerName)
                if (borrower != null) {
                    val currentBlockedUntil = borrower.blockedUntil ?: System.currentTimeMillis()
                    val newBlockedUntil = maxOf(currentBlockedUntil, System.currentTimeMillis()) + (overdueDays * 24 * 60 * 60 * 1000)
                    borrowerDao.updateBorrower(borrower.copy(blockedUntil = newBlockedUntil))
                }
            }
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

    suspend fun getUserEmail(): String? {
        return configDao.getConfig("user_email")?.value
    }

    suspend fun setUserEmail(email: String) {
        configDao.insertConfig(ConfigEntity("user_email", email))
    }

    suspend fun getUserPassword(): String? {
        return configDao.getConfig("user_password")?.value
    }

    suspend fun setUserPassword(password: String) {
        configDao.insertConfig(ConfigEntity("user_password", password))
    }

    suspend fun getUserName(): String? {
        return configDao.getConfig("user_name")?.value
    }

    suspend fun setUserName(name: String) {
        configDao.insertConfig(ConfigEntity("user_name", name))
    }

    suspend fun getUserPhotoUrl(): String? {
        return configDao.getConfig("user_photo_url")?.value
    }

    suspend fun setUserPhotoUrl(url: String) {
        configDao.insertConfig(ConfigEntity("user_photo_url", url))
    }

    suspend fun getLoginMethod(): String? {
        return configDao.getConfig("login_method")?.value
    }

    suspend fun setLoginMethod(method: String) {
        configDao.insertConfig(ConfigEntity("login_method", method))
    }

    suspend fun getGoogleEmail(): String? {
        return configDao.getConfig("google_email")?.value
    }

    suspend fun setGoogleEmail(email: String) {
        configDao.insertConfig(ConfigEntity("google_email", email))
    }

    suspend fun getStudentLoanDays(): Long {
        return configDao.getConfig("student_loan_days")?.value?.toLongOrNull() ?: 7L
    }

    suspend fun setStudentLoanDays(days: Long) {
        configDao.insertConfig(ConfigEntity("student_loan_days", days.toString()))
    }

    suspend fun getTeacherLoanDays(): Long {
        return configDao.getConfig("teacher_loan_days")?.value?.toLongOrNull() ?: 15L
    }

    suspend fun setTeacherLoanDays(days: Long) {
        configDao.insertConfig(ConfigEntity("teacher_loan_days", days.toString()))
    }

    // --- Reservation Operations ---
    val allReservations: Flow<List<ReservationEntity>> = reservationDao.getAllReservations()

    fun getReservationsForBook(bookId: Int): Flow<List<ReservationEntity>> {
        return reservationDao.getReservationsForBook(bookId)
    }

    suspend fun createReservation(bookId: Int, borrowerName: String, borrowerClass: String): Boolean {
        val book = bookDao.getBookById(bookId) ?: return false
        val reservation = ReservationEntity(
            bookId = bookId,
            bookTitle = book.title,
            borrowerName = borrowerName,
            borrowerClass = borrowerClass
        )
        reservationDao.insertReservation(reservation)
        return true
    }

    suspend fun deleteReservationById(id: Int) {
        reservationDao.deleteReservationById(id)
    }

    // --- Shelf & Position Operations ---
    val allShelves: Flow<List<ShelfEntity>> = shelfDao.getAllShelves()
    val allPositionsFlow: Flow<List<ShelfPositionEntity>> = shelfDao.getAllPositionsFlow()

    suspend fun getAllPositions(): List<ShelfPositionEntity> {
        return shelfDao.getAllPositions()
    }

    fun getPositionsForShelfFlow(shelfId: Int): Flow<List<ShelfPositionEntity>> {
        return shelfDao.getPositionsForShelfFlow(shelfId)
    }

    suspend fun getPositionsForShelf(shelfId: Int): List<ShelfPositionEntity> {
        return shelfDao.getPositionsForShelf(shelfId)
    }

    suspend fun getShelfById(id: Int): ShelfEntity? {
        return shelfDao.getShelfById(id)
    }

    suspend fun getPositionById(id: Int): ShelfPositionEntity? {
        return shelfDao.getPositionById(id)
    }

    suspend fun insertShelf(shelf: ShelfEntity): Long {
        return shelfDao.insertShelf(shelf)
    }

    suspend fun updateShelf(shelf: ShelfEntity) {
        shelfDao.updateShelf(shelf)
    }

    suspend fun deleteShelf(shelf: ShelfEntity) {
        shelfDao.deleteShelf(shelf)
    }

    suspend fun insertPosition(position: ShelfPositionEntity): Long {
        return shelfDao.insertPosition(position)
    }

    suspend fun updatePosition(position: ShelfPositionEntity) {
        shelfDao.updatePosition(position)
    }

    suspend fun deletePosition(position: ShelfPositionEntity) {
        shelfDao.deletePosition(position)
    }

    // --- Weekly Suggestions Operations ---
    val allSuggestions: Flow<List<WeeklySuggestionEntity>> = weeklySuggestionDao.getAllSuggestions()

    suspend fun insertSuggestion(suggestion: WeeklySuggestionEntity): Long {
        return weeklySuggestionDao.insertSuggestion(suggestion)
    }

    suspend fun deleteSuggestion(suggestion: WeeklySuggestionEntity) {
        weeklySuggestionDao.deleteSuggestion(suggestion)
    }
}
