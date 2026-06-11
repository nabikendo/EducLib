package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.regex.Pattern

private val DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
private val WHITESPACE_REGEX = Regex("\\s+")

fun String.normalizeForSearch(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return DIACRITICS_PATTERN.matcher(temp).replaceAll("").lowercase().trim()
}

fun calculateLevenshteinDistance(s: String, t: String): Int {
    if (s == t) return 0
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length
    val d = IntArray(t.length + 1) { it }
    for (i in 1..s.length) {
        var prev = i
        var old = d[0]
        d[0] = i
        for (j in 1..t.length) {
            val cost = if (s[i - 1] == t[j - 1]) 0 else 1
            val temp = d[j]
            d[j] = minOf(d[j] + 1, prev + 1, old + cost)
            old = temp
            prev = d[j]
        }
    }
    return d[t.length]
}

fun isWordFuzzyMatch(qWord: String, tWord: String): Boolean {
    if (tWord.contains(qWord) || qWord.contains(tWord)) return true
    val lenDiff = Math.abs(qWord.length - tWord.length)
    val maxAllowed = when {
        qWord.length <= 3 -> 0
        qWord.length <= 6 -> 1
        else -> 2
    }
    if (lenDiff > maxAllowed) return false
    val dist = calculateLevenshteinDistance(qWord, tWord)
    return dist <= maxAllowed
}

fun isFuzzyMatch(query: String, target: String): Boolean {
    val qNorm = query.normalizeForSearch()
    if (qNorm.isEmpty()) return true
    val targetNorm = target.normalizeForSearch()
    if (targetNorm.contains(qNorm)) return true
    
    val queryWords = qNorm.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    if (queryWords.isEmpty()) return true
    val targetWords = targetNorm.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    
    // Each query word must fuzzy-match at least one target word
    return queryWords.all { qWord ->
        targetWords.any { tWord ->
            isWordFuzzyMatch(qWord, tWord)
        }
    }
}

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    private val _studentLoanDays = MutableStateFlow(7L)
    val studentLoanDays: StateFlow<Long> = _studentLoanDays.asStateFlow()

    private val _teacherLoanDays = MutableStateFlow(15L)
    val teacherLoanDays: StateFlow<Long> = _teacherLoanDays.asStateFlow()

    // Login/Security states (restored proper secure startup flow)
    private val _isPasswordSet = MutableStateFlow<Boolean?>(null)
    val isPasswordSet: StateFlow<Boolean?> = _isPasswordSet.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow<String?>(null)
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl.asStateFlow()

    private val _loginMethod = MutableStateFlow<String?>(null)
    val loginMethod: StateFlow<String?> = _loginMethod.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _borrowerSearchQuery = MutableStateFlow("")
    val borrowerSearchQuery: StateFlow<String> = _borrowerSearchQuery.asStateFlow()

    // Reactive lists from Database
    val books: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        repository.allShelves,
        repository.allPositionsFlow,
        _searchQuery
    ) { allBooks, allShelves, allPositions, query ->
        if (query.isBlank()) {
            allBooks
        } else {
            allBooks.filter { book ->
                val titleMatch = isFuzzyMatch(query, book.title)
                val authorMatch = isFuzzyMatch(query, book.author)
                val genreMatch = isFuzzyMatch(query, book.genre)
                val locationMatch = isFuzzyMatch(query, book.location)
                
                val cleanQuery = query.replace("-", "").trim()
                val cleanIsbn = book.isbn.replace("-", "").trim()
                val isbnMatch = cleanIsbn.isNotBlank() && cleanQuery.isNotBlank() && (cleanIsbn.contains(cleanQuery) || cleanQuery.contains(cleanIsbn))
                
                var shelfMatch = false
                var coordMatch = false
                if (book.posicaoEstanteId != null) {
                    val position = allPositions.find { it.id == book.posicaoEstanteId }
                    if (position != null) {
                        coordMatch = isFuzzyMatch(query, position.coordinate)
                        val shelf = allShelves.find { it.id == position.shelfId }
                        if (shelf != null) {
                            shelfMatch = isFuzzyMatch(query, shelf.name)
                        }
                    }
                }
                
                titleMatch || authorMatch || genreMatch || locationMatch || shelfMatch || coordMatch || isbnMatch
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val borrowers: StateFlow<List<BorrowerEntity>> = combine(
        repository.allBorrowers,
        _borrowerSearchQuery
    ) { all, query ->
        if (query.isBlank()) {
            all
        } else {
            all.filter { borrower ->
                isFuzzyMatch(query, borrower.name) ||
                isFuzzyMatch(query, borrower.identifier) ||
                isFuzzyMatch(query, borrower.classRoom) ||
                isFuzzyMatch(query, borrower.type)
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBorrowers: StateFlow<List<BorrowerEntity>> = repository.allBorrowers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLoans: StateFlow<List<LoanEntity>> = repository.activeLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reservations: StateFlow<List<ReservationEntity>> = repository.allReservations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shelves: StateFlow<List<ShelfEntity>> = repository.allShelves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val positions: StateFlow<List<ShelfPositionEntity>> = repository.allPositionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestions: StateFlow<List<WeeklySuggestionEntity>> = repository.allSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkIfPasswordConfigured()
        viewModelScope.launch {
            _studentLoanDays.value = repository.getStudentLoanDays()
            _teacherLoanDays.value = repository.getTeacherLoanDays()
        }
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
        viewModelScope.launch {
            val masterPassword = repository.getMasterPassword()
            val userEmail = repository.getUserEmail()
            val userPassword = repository.getUserPassword()
            val googleEmail = repository.getGoogleEmail()

            val hasConfig = !masterPassword.isNullOrBlank() || 
                             (!userEmail.isNullOrBlank() && !userPassword.isNullOrBlank()) || 
                             !googleEmail.isNullOrBlank()
            
            _isPasswordSet.value = hasConfig

            if (hasConfig) {
                _userEmail.value = repository.getUserEmail() ?: repository.getGoogleEmail()
                _userName.value = repository.getUserName() ?: ""
                _userPhotoUrl.value = repository.getUserPhotoUrl() ?: ""
                _loginMethod.value = repository.getLoginMethod() ?: "master_password"
            }
        }
    }

    fun registerMasterPassword(password: String) {
        viewModelScope.launch {
            if (password.isNotBlank()) {
                repository.setMasterPassword(password)
                repository.setLoginMethod("master_password")
                _isPasswordSet.value = true
                _isLoggedIn.value = true
                _loginMethod.value = "master_password"
                _userName.value = "Administrador"
            }
        }
    }

    fun registerEmailAndPassword(email: String, password: String, name: String) {
        viewModelScope.launch {
            if (email.isNotBlank() && password.isNotBlank()) {
                repository.setUserEmail(email)
                repository.setUserPassword(password)
                repository.setUserName(name)
                repository.setLoginMethod("email")
                
                _userEmail.value = email
                _userName.value = name
                _loginMethod.value = "email"
                _isPasswordSet.value = true
                _isLoggedIn.value = true
            }
        }
    }

    fun registerViaGoogle(email: String, name: String, photoUrl: String?) {
        viewModelScope.launch {
            if (email.isNotBlank()) {
                repository.setGoogleEmail(email)
                repository.setUserName(name)
                repository.setUserPhotoUrl(photoUrl ?: "")
                repository.setLoginMethod("google")
                
                _userEmail.value = email
                _userName.value = name
                _userPhotoUrl.value = photoUrl ?: ""
                _loginMethod.value = "google"
                _isPasswordSet.value = true
                _isLoggedIn.value = true
            }
        }
    }

    suspend fun login(password: String): Boolean {
        val pw = repository.getMasterPassword()
        val userPw = repository.getUserPassword()
        return if ((!pw.isNullOrBlank() && pw == password) || (!userPw.isNullOrBlank() && userPw == password)) {
            _isLoggedIn.value = true
            _loginMethod.value = if (!userPw.isNullOrBlank() && userPw == password) "email" else "master_password"
            repository.setLoginMethod(_loginMethod.value ?: "master_password")
            true
        } else {
            false
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Boolean {
        val userEmail = repository.getUserEmail()
        val userPw = repository.getUserPassword()
        return if (!userEmail.isNullOrBlank() && userEmail.equals(email, ignoreCase = true) && !userPw.isNullOrBlank() && userPw == password) {
            _isLoggedIn.value = true
            _userEmail.value = userEmail
            _userName.value = repository.getUserName() ?: ""
            _loginMethod.value = "email"
            repository.setLoginMethod("email")
            true
        } else {
            // Support fallback to master password if they input email and password
            val masterPassword = repository.getMasterPassword()
            if (!masterPassword.isNullOrBlank() && email.contains("@") && masterPassword == password) {
                repository.setUserEmail(email)
                repository.setUserPassword(password)
                repository.setUserName("Administrador")
                repository.setLoginMethod("email")
                
                _isLoggedIn.value = true
                _userEmail.value = email
                _userName.value = "Administrador"
                _loginMethod.value = "email"
                true
            } else {
                false
            }
        }
    }

    suspend fun loginWithGoogle(email: String, name: String, photoUrl: String?): Boolean {
        repository.setGoogleEmail(email)
        repository.setUserName(name)
        repository.setUserPhotoUrl(photoUrl ?: "")
        repository.setLoginMethod("google")
        
        _isLoggedIn.value = true
        _userEmail.value = email
        _userName.value = name
        _userPhotoUrl.value = photoUrl ?: ""
        _loginMethod.value = "google"
        _isPasswordSet.value = true
        return true
    }

    suspend fun changePassword(oldPw: String, newPw: String): Boolean {
        val pw = repository.getMasterPassword()
        val userPw = repository.getUserPassword()
        return if (!pw.isNullOrBlank() && pw == oldPw && newPw.isNotBlank()) {
            repository.setMasterPassword(newPw)
            true
        } else if (!userPw.isNullOrBlank() && userPw == oldPw && newPw.isNotBlank()) {
            repository.setUserPassword(newPw)
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
        classRoom: String = "",
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val borrower = BorrowerEntity(
                name = name,
                type = type,
                identifier = identifier,
                phone = phone,
                classRoom = classRoom,
                photoUri = photoUri
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
        classRoom: String = "",
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val updated = borrower.copy(
                name = name,
                type = type,
                identifier = identifier,
                phone = phone,
                classRoom = classRoom,
                photoUri = photoUri
            )
            repository.updateBorrower(updated)
            triggerAutoBackup(context)
        }
    }

    fun unblockBorrower(context: Context, borrower: BorrowerEntity) {
        viewModelScope.launch {
            val updated = borrower.copy(blockedUntil = null)
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

    fun updateBorrowerPhone(context: Context, borrowerName: String, phoneStr: String) {
        viewModelScope.launch {
            val borrower = allBorrowers.value.find { it.name.trim().equals(borrowerName.trim(), ignoreCase = true) }
            if (borrower != null) {
                val updated = borrower.copy(phone = phoneStr)
                repository.updateBorrower(updated)
                triggerAutoBackup(context)
            }
        }
    }

    // --- Weekly Suggestions Operations ---
    fun addSuggestion(
        context: Context,
        bookId: Int,
        bookTitle: String,
        bookAuthor: String,
        professorName: String,
        classRoom: String,
        recommendation: String
    ) {
        viewModelScope.launch {
            val suggestion = WeeklySuggestionEntity(
                bookId = bookId,
                bookTitle = bookTitle,
                bookAuthor = bookAuthor,
                professorName = professorName,
                classRoom = classRoom,
                recommendation = recommendation
            )
            repository.insertSuggestion(suggestion)
            triggerAutoBackup(context)
        }
    }

    fun removeSuggestion(context: Context, suggestion: WeeklySuggestionEntity) {
        viewModelScope.launch {
            repository.deleteSuggestion(suggestion)
            triggerAutoBackup(context)
        }
    }

    fun setStudentLoanDays(days: Long) {
        viewModelScope.launch {
            repository.setStudentLoanDays(days)
            _studentLoanDays.value = days
        }
    }

    fun setTeacherLoanDays(days: Long) {
        viewModelScope.launch {
            repository.setTeacherLoanDays(days)
            _teacherLoanDays.value = days
        }
    }

    // --- Book Operations ---
    fun addBook(
        context: Context,
        title: String,
        author: String,
        isbn: String,
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
                isbn = isbn,
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
        isbn: String,
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
                isbn = isbn,
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

    fun returnBook(context: Context, loanId: Int, returnedQuantity: Int = -1, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.returnLoan(loanId, returnedQuantity)
            onResult(success)
            if (success) {
                triggerAutoBackup(context)
            }
        }
    }

    // --- Reservation Methods ---
    fun createReservation(
        context: Context,
        bookId: Int,
        borrowerName: String,
        borrowerClass: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.createReservation(bookId, borrowerName, borrowerClass)
            onResult(success)
            if (success) {
                triggerAutoBackup(context)
            }
        }
    }

    fun deleteReservation(context: Context, reservationId: Int) {
        viewModelScope.launch {
            repository.deleteReservationById(reservationId)
            triggerAutoBackup(context)
        }
    }

    fun promoteReservationToLoan(context: Context, reservation: ReservationEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.createLoan(
                bookId = reservation.bookId,
                borrowerName = reservation.borrowerName,
                borrowerClass = reservation.borrowerClass,
                quantity = 1
            )
            if (success) {
                repository.deleteReservationById(reservation.id)
                triggerAutoBackup(context)
            }
            onResult(success)
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            BackupManager.saveAutoBackup(context, books.value, loans.value)
        }
    }

    fun getShareBackupIntent(context: Context): Intent? {
        return BackupManager.getShareIntent(context, books.value, loans.value)
    }

    fun getShareBooksCsvIntent(context: Context): Intent? {
        return BackupManager.getShareBooksCsvIntent(context, books.value)
    }

    fun exportBackupToJson(): String {
        return BackupManager.toJson(books.value, loans.value)
    }

    fun restoreBackup(context: Context, jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val payload = BackupManager.fromJson(jsonString) ?: return@withContext false
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.clearAllTables()

                    payload.books?.forEach { book ->
                        repository.insertBook(book)
                    }

                    payload.loans?.forEach { loan ->
                        repository.createLoan(
                            bookId = loan.bookId,
                            borrowerName = loan.borrowerName,
                            borrowerClass = loan.borrowerClass,
                            loanDate = loan.loanDate
                        ).let {
                            if (loan.returnDate != null) {
                                val allNewLoans = repository.allLoans.first()
                                val inserted = allNewLoans.firstOrNull {
                                    it.bookId == loan.bookId &&
                                    it.borrowerName == loan.borrowerName &&
                                    it.loanDate == loan.loanDate
                                }
                                if (inserted != null) {
                                    repository.returnLoan(inserted.id, returnDate = loan.returnDate ?: System.currentTimeMillis())
                                }
                            }
                        }
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            onResult(success)
        }
    }

    fun importBooksFromCsv(context: Context, csvString: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val lines = csvString.split('\n')
                    if (lines.isEmpty()) {
                        return@withContext Pair(false, 0)
                    }

                    val firstLine = lines.firstOrNull() ?: ""
                    val headers = parseCsvLine(firstLine).map { it.trim().lowercase() }

                    var titleIdx = headers.indexOfFirst { it.contains("titulo") || it.contains("título") || it == "title" }
                    var authorIdx = headers.indexOfFirst { it.contains("autor") || it == "author" }
                    var genreIdx = headers.indexOfFirst { it.contains("genero") || it.contains("gênero") || it == "genre" }
                    var yearIdx = headers.indexOfFirst { it.contains("ano") || it == "year" }
                    var quantityIdx = headers.indexOfFirst { it.contains("quantidade") || it.contains("qtd") || it.contains("quantity") || it == "total" || it.contains("total") }
                    var locationIdx = headers.indexOfFirst { it.contains("localizacao") || it.contains("localização") || it == "location" }

                    if (titleIdx == -1 && authorIdx == -1) {
                        titleIdx = 0
                        authorIdx = 1
                        genreIdx = 2
                        yearIdx = 3
                        quantityIdx = 4
                        locationIdx = 5
                    }

                    var importCount = 0
                    val startRow = if (headers.any { it.contains("titulo") || it.contains("título") || it == "title" || it == "autor" || it == "author" }) 1 else 0

                    for (i in startRow until lines.size) {
                        val line = lines[i]
                        if (line.isBlank()) continue
                        val columns = parseCsvLine(line)
                        if (columns.size <= maxOf(titleIdx, authorIdx)) continue

                        val title = columns.getOrNull(titleIdx)?.trim()?.removeSurrounding("\"")?.ifBlank { null }
                        val author = columns.getOrNull(authorIdx)?.trim()?.removeSurrounding("\"")?.ifBlank { null }

                        if (title == null || author == null) continue

                        val genre = if (genreIdx != -1) columns.getOrNull(genreIdx)?.trim()?.removeSurrounding("\"")?.ifBlank { "Outros" } ?: "Outros" else "Outros"
                        val yearStr = if (yearIdx != -1) columns.getOrNull(yearIdx)?.trim()?.removeSurrounding("\"") else null
                        val year = yearStr?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        val qtyStr = if (quantityIdx != -1) columns.getOrNull(quantityIdx)?.trim()?.removeSurrounding("\"") else null
                        val totalQty = qtyStr?.toIntOrNull() ?: 1
                        val location = if (locationIdx != -1) columns.getOrNull(locationIdx)?.trim()?.removeSurrounding("\"")?.ifBlank { "Geral" } ?: "Geral" else "Geral"

                        val book = com.example.data.BookEntity(
                            title = title,
                            author = author,
                            genre = genre,
                            year = year,
                            location = location,
                            photoPath = null,
                            totalQuantity = totalQty,
                            availableQuantity = totalQty
                        )
                        repository.insertBook(book)
                        importCount++
                    }

                    if (importCount > 0) {
                        triggerAutoBackup(context)
                        Pair(true, importCount)
                    } else {
                        Pair(false, 0)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, 0)
                }
            }
            onResult(result.first, result.second)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var curVal = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',' || ch == ';') {
                    tokens.add(curVal.toString())
                    curVal = java.lang.StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        tokens.add(curVal.toString())
        return tokens
    }

    // --- JTA & ROOF Custom Integrations & Algorithms ---

    fun updateBookJtaMetadata(book: BookEntity, category: String, tipoObra: String, publicoAlvo: String) {
        viewModelScope.launch {
            val updated = book.copy(
                category = category,
                tipoObra = tipoObra,
                publicoAlvo = publicoAlvo
            )
            repository.updateBook(updated)
        }
    }

    fun updateBookCustomJustification(book: BookEntity, text: String) {
        viewModelScope.launch {
            val updated = book.copy(textoJustificativa = text)
            repository.updateBook(updated)
        }
    }

    fun associateBookToPosition(book: BookEntity, positionId: Int?) {
        viewModelScope.launch {
            val updated = book.copy(posicaoEstanteId = positionId)
            repository.updateBook(updated)
        }
    }

    // Shelf CRUD functions
    fun addShelf(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertShelf(ShelfEntity(name = name))
            }
        }
    }

    fun deleteShelf(shelf: ShelfEntity) {
        viewModelScope.launch {
            repository.deleteShelf(shelf)
        }
    }

    fun addPosition(shelfId: Int, coordinate: String, accessibilityLevel: Int) {
        viewModelScope.launch {
            if (coordinate.isNotBlank()) {
                repository.insertPosition(
                    ShelfPositionEntity(
                        shelfId = shelfId,
                        coordinate = coordinate,
                        accessibilityLevel = accessibilityLevel
                    )
                )
            }
        }
    }

    fun deletePosition(position: ShelfPositionEntity) {
        viewModelScope.launch {
            repository.deletePosition(position)
        }
    }

    // JTA Calculation Results Structure & Logic
    data class JtaAnalysis(
        val book: BookEntity,
        val classification: String,
        val justification: String,
        val recommendation: String,
        val level: String // "Crítica", "Média", "Baixa" or "Normal"
    )

    fun runJtaAnalysisForBooks(allBooksList: List<BookEntity>): List<JtaAnalysis> {
        return allBooksList.map { book ->
            val totalLoans = book.totalLoans
            val lastLoan = book.lastLoanDate
            
            val daysSinceLastLoan = if (lastLoan != null) {
                (System.currentTimeMillis() - lastLoan) / (1000L * 60 * 60 * 24)
            } else {
                365L // if never loaned, treat as 1 year
            }
            
            // Criteria: Low circulation is defined as less than 3 loans or over 120 days since last borrow
            val isLowCirculation = totalLoans < 3 || daysSinceLastLoan > 120
            
            if (!isLowCirculation) {
                JtaAnalysis(
                    book = book,
                    classification = "Normal",
                    justification = "O livro mantém fluxo saudável de circulação institucional. Mantém frequência de empréstimo satisfatória no acervo da biblioteca.",
                    recommendation = "Manter monitoramento rotineiro regular.",
                    level = "Normal"
                )
            } else {
                val resolvedCategory = book.category.ifBlank { book.genre }
                when {
                    book.tipoObra.equals("Referência", ignoreCase = true) || 
                    book.tipoObra.equals("Técnico", ignoreCase = true) || 
                    book.genre.contains("Referência", ignoreCase = true) || 
                    book.genre.contains("Bíblia", ignoreCase = true) || 
                    book.genre.contains("Dicionário", ignoreCase = true) ||
                    resolvedCategory.contains("Enciclopédia", ignoreCase = true) -> {
                        JtaAnalysis(
                            book = book,
                            classification = "Baixa circulação estrutural",
                            justification = "Obra técnica ou de consulta rápida direta no recinto da biblioteca. O perfil curricular determina baixa frequência de empréstimo externo (para domicílio), mas exibe alta relevância pedagógica estrutural interna na instituição de ensino ou acervo público. Justifica-se integralmente sua permanência física.",
                            recommendation = "Manter no acervo permanente institucional. Estimular uso na própria sala de estudos através de dinâmicas didáticas coordenadas.",
                            level = "Baixa"
                        )
                    }
                    book.tipoObra.equals("Didático", ignoreCase = true) || 
                    book.publicoAlvo.contains("Infantil", ignoreCase = true) ||
                    book.publicoAlvo.contains("Juvenil", ignoreCase = true) -> {
                        JtaAnalysis(
                            book = book,
                            classification = "Baixa circulação situacional",
                            justification = "Material com perfil sazonal atrelado diretamente às dinâmicas letivas anuais. A circulação é altamente vinculada a bimestres escolares específicos, cronogramas de projetos escolares de leitura ou ementas específicas, registrando ociosidade temporária em certas épocas.",
                            recommendation = "Manter no acervo como suporte estratégico de prontidão de conteúdo. Promover destaque focal no balcão de exibição no período letivo correspondente.",
                            level = "Média"
                        )
                    }
                    else -> {
                        JtaAnalysis(
                            book = book,
                            classification = "Baixa circulação crítica",
                            justification = "Tópico de ficção ou conteúdo geral ausente de saídas recentes por mais de $daysSinceLastLoan dias, acumulando pouca aderência de busca na estante física atual.",
                            recommendation = "Reavaliar seu posicionamento físico utilizando a otimização ROOF, realizar exposições com curadoria de destaque temático quinzenal, ou em último caso encaminhar para triagem de descarte orientado.",
                            level = "Alta"
                        )
                    }
                }
            }
        }
    }

    // Index of Defensibility (IDA)
    fun calculateAssetDefensibilityIndex(allBooksList: List<BookEntity>): Int {
        val analysis = runJtaAnalysisForBooks(allBooksList)
        val lowCirculationBooks = analysis.filter { it.classification != "Normal" }
        if (lowCirculationBooks.isEmpty()) return 100
        
        // Defended means either 'estrutural' or 'situacional' (since they are justified inherently)
        // or has a custom text justification entered by the librarian.
        val defendedBooks = lowCirculationBooks.count { 
            it.classification == "Baixa circulação estrutural" || 
            it.classification == "Baixa circulação situacional" || 
            it.book.textoJustificativa?.isNotBlank() == true 
        }
        return (defendedBooks * 100) / lowCirculationBooks.size
    }

    // ROOF Optimization Suggestions Engine
    data class RoofSuggestion(
        val book: BookEntity,
        val currentPosition: ShelfPositionEntity?,
        val recommendedPosition: ShelfPositionEntity,
        val reason: String
    )

    fun optimizeShelvesROOF(
        shelfBooks: List<BookEntity>,
        shelfPositions: List<ShelfPositionEntity>
    ): List<RoofSuggestion> {
        if (shelfBooks.isEmpty() || shelfPositions.isEmpty()) return emptyList()

        // 1. Sort books by frequency of use descending
        val sortedBooks = shelfBooks.sortedByDescending { it.useFrequency }

        // 2. Sort available positions by accessibility descending (Level 3 = High, 2 = Medium, 1 = Low)
        val sortedPositions = shelfPositions.sortedByDescending { it.accessibilityLevel }

        // 3. Map ideal pairings
        val suggestions = mutableListOf<RoofSuggestion>()
        sortedBooks.forEachIndexed { index, book ->
            val idealPos = sortedPositions.getOrNull(index % sortedPositions.size) ?: return@forEachIndexed
            val currentPos = shelfPositions.find { it.id == book.posicaoEstanteId }

            if (currentPos == null || currentPos.id != idealPos.id) {
                // There is room for improvement!
                val reason = when {
                    book.useFrequency > 3 && idealPos.accessibilityLevel == 3 -> {
                        "Livro '${book.title}' altamente demandado (freq: ${book.useFrequency}). Recomendamos mover para a posição '${idealPos.coordinate}' (Zona dos Olhos - Alta Acessibilidade) para agilizar as retiradas diárias."
                    }
                    book.useFrequency == 0 && idealPos.accessibilityLevel == 1 -> {
                        "Livro '${book.title}' sem saídas registradas. Deve ser remanejado para a posição '${idealPos.coordinate}' (Zona Térrea/Superior - Baixa Acessibilidade), liberando espaço estratégico central."
                    }
                    else -> {
                        "Ajustar livro '${book.title}' para a posição ideal '${idealPos.coordinate}' alinhando sua circulação física real à acessibilidade da prateleira correspondente."
                    }
                }

                suggestions.add(
                    RoofSuggestion(
                        book = book,
                        currentPosition = currentPos,
                        recommendedPosition = idealPos,
                        reason = reason
                    )
                )
            }
        }

        return suggestions
    }

    // Calculate shelf efficiency index
    fun calculateShelfEfficiencyIndex(
        shelfBooks: List<BookEntity>,
        shelfPositions: List<ShelfPositionEntity>
    ): Int {
        val assignedBooks = shelfBooks.filter { it.posicaoEstanteId != null }
        if (assignedBooks.isEmpty()) return 100

        // Sort all possible shelf books by frequency to establish quartiles/tiers
        val sortedByFreq = shelfBooks.sortedBy { it.useFrequency }
        val totalCount = sortedByFreq.size

        var totalScore = 0
        var evaluatedCount = 0

        assignedBooks.forEach { book ->
            val position = shelfPositions.find { it.id == book.posicaoEstanteId } ?: return@forEach
            val rankPos = sortedByFreq.indexOfFirst { it.id == book.id }
            val percentile = if (totalCount > 1) rankPos.toFloat() / (totalCount - 1) else 1f

            val expectedAcess = when {
                percentile >= 0.7f -> 3 // Very busy -> high access
                percentile >= 0.3f -> 2 // Mid busy -> medium access
                else -> 1 // Low busy -> low access
            }

            // Delta difference
            val delta = kotlin.math.abs(expectedAcess - position.accessibilityLevel)
            val score = when (delta) {
                0 -> 100
                1 -> 70
                else -> 30
            }
            totalScore += score
            evaluatedCount++
        }

        if (evaluatedCount == 0) return 100
        return totalScore / evaluatedCount
    }

    // Generate formal audit text
    fun generateJtaAuditReport(analysisList: List<JtaAnalysis>): String {
        val sb = java.lang.StringBuilder()
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val dateString = format.format(java.util.Date())

        sb.append("========================================================================\n")
        sb.append("       RELATÓRIO DE JUSTIFICATIVA TÉCNICA E AUDITORIA DO ACERVO JTA\n")
        sb.append("   SISTEMA DE GESTÃO DE BIBLIOTECAS PÚBLICAS E ESCOLARES (OFFLINE)\n")
        sb.append("   Gerado em: $dateString\n")
        sb.append("========================================================================\n\n")

        val lowCirc = analysisList.filter { it.classification != "Normal" }
        sb.append("1. COMPOSIÇÃO DE DEFENSORIA DO ACERVO\n")
        val defValue = if (lowCirc.isEmpty()) 100 else {
            val defs = lowCirc.count { it.classification == "Baixa circulação estrutural" || it.classification == "Baixa circulação situacional" || it.book.textoJustificativa?.isNotBlank() == true }
            (defs * 100) / lowCirc.size
        }
        sb.append("   - Índice de Defensabilidade do Acervo (IDA): $defValue%\n")
        sb.append("   - Quantidade de livros mapeados com baixa circulação: ${lowCirc.size} de ${analysisList.size} livros totais\n\n")

        sb.append("2. DETALHAMENTO DE OBRAS REGISTRADAS COMO BAIXA CIRCULAÇÃO:\n")
        sb.append("------------------------------------------------------------------------\n")

        lowCirc.forEachIndexed { i, entry ->
            val book = entry.book
            sb.append("${i + 1}. TÍTULO: ${book.title}\n")
            sb.append("   AUTOR: ${book.author} | GÊNERO: ${book.genre}\n")
            sb.append("   TIPO DE OBRA: ${book.tipoObra.uppercase()} | CATEGORIA: ${book.category.ifBlank { "Não Atribuída" }}\n")
            sb.append("   PÚBLICO-ALVO: ${book.publicoAlvo}\n")
            sb.append("   EMPRÉSTIMOS TOTAIS: ${book.totalLoans} | CLASSIFICAÇÃO: ${entry.classification.uppercase()}\n")
            sb.append("   TID/GRAU CRITICIDADE: ${entry.level.uppercase()}\n")
            sb.append("   JUSTIFICATIVA TÉCNICA AUTOMÁTICA OFICIAL JTA:\n")
            sb.append("   \"${entry.justification}\"\n")
            if (book.textoJustificativa?.isNotBlank() == true) {
                sb.append("   ANOTAÇÕES COMPLEMENTARES DO BIBLIOTECÁRIO:\n")
                sb.append("   \"${book.textoJustificativa}\"\n")
            }
            sb.append("   RECOMENDAÇÃO TÉCNICA DE MANUTENÇÃO:\n")
            sb.append("   ${entry.recommendation}\n")
            sb.append("------------------------------------------------------------------------\n\n")
        }

        sb.append("FIM DE RELATÓRIO OFICIAL DE AUDITORIA EXTERNA E LEI DO ACERVO\n")
        sb.append("Assinatura do Bibliotecário Responsável: _______________________________\n")
        return sb.toString()
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
