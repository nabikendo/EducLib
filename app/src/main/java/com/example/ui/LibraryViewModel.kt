package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    // Login/Security states (disabled since app is exclusive, set to true by default)
    private val _isPasswordSet = MutableStateFlow<Boolean?>(true)
    val isPasswordSet: StateFlow<Boolean?> = _isPasswordSet.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _borrowerSearchQuery = MutableStateFlow("")
    val borrowerSearchQuery: StateFlow<String> = _borrowerSearchQuery.asStateFlow()

    // Reactive lists from Database
    @OptIn(ExperimentalCoroutinesApi::class)
    val books: StateFlow<List<BookEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allBooks
            } else {
                repository.searchBooks(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val borrowers: StateFlow<List<BorrowerEntity>> = _borrowerSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allBorrowers
            } else {
                repository.searchBorrowers(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBorrowers: StateFlow<List<BorrowerEntity>> = repository.allBorrowers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLoans: StateFlow<List<LoanEntity>> = repository.activeLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkIfPasswordConfigured()
        // Save automatic backup whenever books or loans change
        viewModelScope.launch {
            combine(books, loans) { b, l -> Pair(b, l) }
                .collectLatest { (b, l) ->
                    // Auto backup on separate threads to avoid locking layout
                    // We need a Context, will trigger backup when lists are modified in methods
                }
        }
    }

    private fun checkIfPasswordConfigured() {
        _isPasswordSet.value = true
    }

    fun registerMasterPassword(password: String) {
        viewModelScope.launch {
            if (password.isNotBlank()) {
                repository.setMasterPassword(password)
                _isPasswordSet.value = true
                _isLoggedIn.value = true
            }
        }
    }

    suspend fun login(password: String): Boolean {
        val pw = repository.getMasterPassword()
        return if (pw == password) {
            _isLoggedIn.value = true
            true
        } else {
            false
        }
    }

    suspend fun changePassword(oldPw: String, newPw: String): Boolean {
        val pw = repository.getMasterPassword()
        return if (pw == oldPw && newPw.isNotBlank()) {
            repository.setMasterPassword(newPw)
            true
        } else {
            false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    fun searchBooks(query: String) {
        _searchQuery.value = query
    }

    fun searchBorrowers(query: String) {
        _borrowerSearchQuery.value = query
    }

    fun addBorrower(
        context: Context,
        name: String,
        type: String, // "Aluno" or "Professor"
        identifier: String,
        phone: String,
        classRoom: String = ""
    ) {
        viewModelScope.launch {
            val borrower = BorrowerEntity(
                name = name,
                type = type,
                identifier = identifier,
                phone = phone,
                classRoom = classRoom
            )
            repository.insertBorrower(borrower)
            triggerAutoBackup(context)
        }
    }

    fun updateBorrower(
        context: Context,
        borrower: BorrowerEntity,
        name: String,
        type: String,
        identifier: String,
        phone: String,
        classRoom: String = ""
    ) {
        viewModelScope.launch {
            val updated = borrower.copy(
                name = name,
                type = type,
                identifier = identifier,
                phone = phone,
                classRoom = classRoom
            )
            repository.updateBorrower(updated)
            triggerAutoBackup(context)
        }
    }

    fun deleteBorrower(context: Context, borrower: BorrowerEntity) {
        viewModelScope.launch {
            repository.deleteBorrower(borrower)
            triggerAutoBackup(context)
        }
    }

    // --- Book Operations ---
    fun addBook(
        context: Context,
        title: String,
        author: String,
        genre: String,
        year: Int,
        location: String,
        photoPath: String?,
        totalQuantity: Int
    ) {
        viewModelScope.launch {
            val book = BookEntity(
                title = title,
                author = author,
                genre = genre,
                year = year,
                location = location,
                photoPath = photoPath,
                totalQuantity = totalQuantity,
                availableQuantity = totalQuantity
            )
            repository.insertBook(book)
            triggerAutoBackup(context)
        }
    }

    fun updateBook(
        context: Context,
        book: BookEntity,
        title: String,
        author: String,
        genre: String,
        year: Int,
        location: String,
        photoPath: String?,
        totalQuantity: Int
    ) {
        viewModelScope.launch {
            // Adjust available quantity: we add/sub the difference in total counts
            val difference = totalQuantity - book.totalQuantity
            val newAvailable = (book.availableQuantity + difference).coerceAtLeast(0)

            val updated = book.copy(
                title = title,
                author = author,
                genre = genre,
                year = year,
                location = location,
                photoPath = photoPath ?: book.photoPath,
                totalQuantity = totalQuantity,
                availableQuantity = newAvailable
            )
            repository.updateBook(updated)
            triggerAutoBackup(context)
        }
    }

    fun deleteBook(context: Context, book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            triggerAutoBackup(context)
        }
    }

    // --- Loan Operations ---
    fun loanBook(
        context: Context,
        bookId: Int,
        borrowerName: String,
        borrowerClass: String,
        quantity: Int = 1,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.createLoan(bookId, borrowerName, borrowerClass, quantity)
            onResult(success)
            if (success) {
                triggerAutoBackup(context)
            }
        }
    }

    fun renewLoan(context: Context, loanId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.renewLoan(loanId)
            onResult(success)
            if (success) {
                triggerAutoBackup(context)
            }
        }
    }

    fun returnBook(context: Context, loanId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.returnLoan(loanId)
            onResult(success)
            if (success) {
                triggerAutoBackup(context)
            }
        }
    }

    fun deleteLoan(context: Context, loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            triggerAutoBackup(context)
        }
    }

    // --- Backup & Restore ---
    fun triggerAutoBackup(context: Context) {
        viewModelScope.launch {
            BackupManager.saveAutoBackup(context, books.value, loans.value)
        }
    }

    fun getShareBackupIntent(context: Context): Intent? {
        return BackupManager.getShareIntent(context, books.value, loans.value)
    }

    fun restoreBackup(context: Context, jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val payload = BackupManager.fromJson(jsonString)
            if (payload == null) {
                onResult(false)
                return@launch
            }

            try {
                // To be robust, clear existing data and insert backup
                val db = AppDatabase.getDatabase(context)
                db.clearAllTables()

                payload.books?.forEach { book ->
                    // Re-insert safely
                    repository.insertBook(book)
                }

                payload.loans?.forEach { loan ->
                    // Re-insert safely
                    repository.createLoan(
                        bookId = loan.bookId,
                        borrowerName = loan.borrowerName,
                        borrowerClass = loan.borrowerClass,
                        loanDate = loan.loanDate
                    ).let { success ->
                        // If it is already marked returned, we update it to preserve status
                        if (loan.returnDate != null) {
                            // Find the newly inserted loan and update returnDate
                            val allNewLoans = repository.allLoans.first()
                            val inserted = allNewLoans.firstOrNull {
                                it.bookId == loan.bookId &&
                                it.borrowerName == loan.borrowerName &&
                                it.loanDate == loan.loanDate
                            }
                            if (inserted != null) {
                                repository.returnLoan(inserted.id, loan.returnDate)
                            }
                        }
                    }
                }
                
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    // Provider Factory for ViewModel
    class Factory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
