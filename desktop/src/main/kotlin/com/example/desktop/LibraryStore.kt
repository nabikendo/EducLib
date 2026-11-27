package com.example.desktop

import com.example.data.BackupFormat
import com.example.data.BackupPayload
import com.example.data.BackupSnapshot
import com.example.data.BookEntity
import com.example.data.BorrowerEntity
import com.example.data.LoanEntity
import com.example.data.ReservationEntity
import com.example.data.ShelfEntity
import com.example.data.ShelfPositionEntity
import com.example.data.WeeklySuggestionEntity
import com.example.data.afterLoan
import com.example.data.afterReturn
import com.example.data.applyReturn
import com.example.data.bookDeletionRefusal
import com.example.data.loanDueDate
import com.example.data.loanRefusal
import com.example.data.penaltyBlockedUntil
import com.example.data.DAY_MS
import com.example.data.DEFAULT_STUDENT_LOAN_DAYS
import com.example.data.DEFAULT_TEACHER_LOAN_DAYS
import com.example.data.RENEWAL_DAYS
import com.example.data.loanClassLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

/** Todo o acervo em memória. As coleções são as mesmas do backup v2 do app Android. */
data class LibraryData(
    val books: List<BookEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val borrowers: List<BorrowerEntity> = emptyList(),
    val reservations: List<ReservationEntity> = emptyList(),
    val shelves: List<ShelfEntity> = emptyList(),
    val positions: List<ShelfPositionEntity> = emptyList(),
    val suggestions: List<WeeklySuggestionEntity> = emptyList()
) {
    fun toSnapshot() = BackupSnapshot(books, loans, borrowers, reservations, shelves, positions, suggestions)

    companion object {
        fun from(payload: BackupPayload) = LibraryData(
            books = payload.books.orEmpty(),
            loans = payload.loans.orEmpty(),
            borrowers = payload.borrowers.orEmpty(),
            reservations = payload.reservations.orEmpty(),
            shelves = payload.shelves.orEmpty(),
            positions = payload.shelfPositions.orEmpty(),
            suggestions = payload.weeklySuggestions.orEmpty()
        )
    }
}

class StoreException(message: String) : Exception(message)

/**
 * Armazenamento do EducLib para desktop. O arquivo de dados é um backup EducLib v2 válido, gravado
 * de forma atômica a cada alteração (com uma cópia `.bak` da versão anterior). Assim os dados podem ir
 * do computador para o celular e vice-versa pela importação/exportação de backup.
 */
class LibraryStore(
    private val dataFile: Path,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val settingsFile: Path = dataFile.resolveSibling("configuracoes.properties")
    private val _data = MutableStateFlow(load())
    val data: StateFlow<LibraryData> = _data.asStateFlow()

    var studentLoanDays: Long = DEFAULT_STUDENT_LOAN_DAYS
        private set
    var teacherLoanDays: Long = DEFAULT_TEACHER_LOAN_DAYS
        private set

    init {
        loadSettings()
    }

    private fun load(): LibraryData {
        if (!Files.exists(dataFile)) return LibraryData()
        val json = Files.readString(dataFile)
        val payload = BackupFormat.fromJson(json)
            ?: throw StoreException("O arquivo de dados ${dataFile.fileName} está corrompido. A cópia anterior está em ${dataFile.fileName}.bak.")
        BackupFormat.validate(payload)?.let { throw StoreException("Dados inválidos em ${dataFile.fileName}: $it") }
        return LibraryData.from(payload).sanitized()
    }

    private fun loadSettings() {
        if (!Files.exists(settingsFile)) return
        val props = Properties().apply { Files.newInputStream(settingsFile).use { load(it) } }
        props.getProperty("student_loan_days")?.toLongOrNull()?.takeIf { it in 1..365 }?.let { studentLoanDays = it }
        props.getProperty("teacher_loan_days")?.toLongOrNull()?.takeIf { it in 1..365 }?.let { teacherLoanDays = it }
    }

    fun setLoanDays(student: Long, teacher: Long) {
        require(student in 1..365 && teacher in 1..365) { "Prazos devem ficar entre 1 e 365 dias." }
        studentLoanDays = student
        teacherLoanDays = teacher
        val props = Properties().apply {
            setProperty("student_loan_days", student.toString())
            setProperty("teacher_loan_days", teacher.toString())
        }
        Files.createDirectories(settingsFile.parent)
        Files.newOutputStream(settingsFile).use { props.store(it, "EducLib desktop") }
    }

    /** Aplica a mudança e grava; se a gravação falhar, nada muda em memória. */
    @Synchronized
    private fun commit(change: (LibraryData) -> LibraryData) {
        val next = change(_data.value)
        save(next)
        _data.value = next
    }

    private fun save(data: LibraryData) {
        Files.createDirectories(dataFile.toAbsolutePath().parent)
        val json = BackupFormat.toJson(data.toSnapshot(), createdAt = clock())
        val tmp = dataFile.resolveSibling(dataFile.fileName.toString() + ".tmp")
        Files.writeString(tmp, json)
        if (Files.exists(dataFile)) Files.copy(dataFile, dataFile.resolveSibling(dataFile.fileName.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING)
        try {
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun <T : Any> nextId(items: List<T>, id: (T) -> Int) = (items.maxOfOrNull(id) ?: 0) + 1

    // ---------------------------------------------------------------- Livros

    fun saveBook(book: BookEntity): Int {
        val title = book.title.trim()
        val author = book.author.trim()
        if (title.isEmpty() || author.isEmpty()) throw StoreException("Informe título e autoria.")
        if (book.totalQuantity < 0 || book.availableQuantity !in 0..book.totalQuantity) {
            throw StoreException("A quantidade disponível deve ficar entre 0 e o total de exemplares.")
        }
        var savedId = book.id
        commit { d ->
            if (book.id == 0) {
                savedId = nextId(d.books) { it.id }
                d.copy(books = d.books + book.copy(id = savedId, title = title, author = author))
            } else {
                d.copy(books = d.books.map { if (it.id == book.id) book.copy(title = title, author = author) else it })
            }
        }
        return savedId
    }

    /** Ajusta o total de exemplares mantendo os emprestados (disponível = total − emprestados). */
    fun availableFor(bookId: Int, newTotal: Int): Int {
        val lent = _data.value.loans.filter { it.bookId == bookId && it.returnDate == null }.sumOf { it.quantity }
        if (newTotal < lent) throw StoreException("Há $lent exemplar(es) emprestado(s); o total não pode ser menor que isso.")
        return newTotal - lent
    }

    fun deleteBook(bookId: Int) {
        val d = _data.value
        val book = d.books.firstOrNull { it.id == bookId } ?: return
        bookDeletionRefusal(book, d.loans)?.let { throw StoreException(it) }
        commit { it.copy(books = it.books.filterNot { b -> b.id == bookId }, reservations = it.reservations.filterNot { r -> r.bookId == bookId }) }
    }

    // ---------------------------------------------------------------- Leitores

    fun saveBorrower(borrower: BorrowerEntity): Int {
        val name = borrower.name.trim()
        if (name.isEmpty()) throw StoreException("Informe o nome do leitor.")
        val d = _data.value
        if (d.borrowers.any { it.id != borrower.id && it.name.equals(name, ignoreCase = true) }) {
            throw StoreException("Já existe um leitor chamado “$name”. Os empréstimos são ligados pelo nome; use um nome diferente.")
        }
        var savedId = borrower.id
        commit { cur ->
            if (borrower.id == 0) {
                savedId = nextId(cur.borrowers) { it.id }
                cur.copy(borrowers = cur.borrowers + borrower.copy(id = savedId, name = name))
            } else {
                cur.copy(borrowers = cur.borrowers.map { if (it.id == borrower.id) borrower.copy(name = name) else it })
            }
        }
        return savedId
    }

    fun deleteBorrower(borrowerId: Int) {
        val d = _data.value
        val borrower = d.borrowers.firstOrNull { it.id == borrowerId } ?: return
        if (d.loans.any { it.borrowerName == borrower.name && it.returnDate == null }) {
            throw StoreException("${borrower.name} tem empréstimos ativos. Registre as devoluções antes de excluir.")
        }
        commit { it.copy(borrowers = it.borrowers.filterNot { b -> b.id == borrowerId }) }
    }

    fun clearBlock(borrowerId: Int) = commit { d ->
        d.copy(borrowers = d.borrowers.map { if (it.id == borrowerId) it.copy(blockedUntil = null) else it })
    }

    // ---------------------------------------------------------------- Empréstimos

    fun lend(bookId: Int, borrowerId: Int, quantity: Int = 1) {
        val now = clock()
        val d = _data.value
        val book = d.books.firstOrNull { it.id == bookId }
        val borrower = d.borrowers.firstOrNull { it.id == borrowerId } ?: throw StoreException("Leitor não encontrado.")
        loanRefusal(book, quantity, borrower, d.loans, now)?.let { throw StoreException(it) }
        book!!
        val borrowerClass = borrower.loanClassLabel()
        commit { cur ->
            val loan = LoanEntity(
                id = nextId(cur.loans) { it.id },
                bookId = book.id,
                bookTitle = book.title,
                bookAuthor = book.author,
                borrowerName = borrower.name,
                borrowerClass = borrowerClass,
                loanDate = now,
                dueDate = loanDueDate(borrowerClass, now, studentLoanDays, teacherLoanDays),
                quantity = quantity
            )
            cur.copy(
                loans = cur.loans + loan,
                books = cur.books.map { if (it.id == book.id) it.afterLoan(quantity, now) else it }
            )
        }
    }

    fun returnLoan(loanId: Int, quantity: Int = -1) {
        val now = clock()
        val d = _data.value
        val loan = d.loans.firstOrNull { it.id == loanId } ?: throw StoreException("Empréstimo não encontrado.")
        val outcome = loan.applyReturn(quantity, now) ?: throw StoreException("Este empréstimo já foi devolvido.")
        commit { cur ->
            var loans = cur.loans.map { if (it.id == loanId) outcome.updatedLoan else it }
            outcome.returnedRecord?.let { loans = loans + it.copy(id = nextId(loans) { l -> l.id }) }
            val borrowers = cur.borrowers.map { b ->
                if (b.name != loan.borrowerName) b
                else penaltyBlockedUntil(loan, now, b.blockedUntil, now)?.let { b.copy(blockedUntil = it) } ?: b
            }
            cur.copy(
                loans = loans,
                books = cur.books.map { if (it.id == loan.bookId) it.afterReturn(outcome.returnedQuantity) else it },
                borrowers = borrowers
            )
        }
    }

    fun renewLoan(loanId: Int) {
        val loan = _data.value.loans.firstOrNull { it.id == loanId } ?: throw StoreException("Empréstimo não encontrado.")
        if (loan.returnDate != null) throw StoreException("Empréstimos devolvidos não podem ser renovados.")
        commit { d -> d.copy(loans = d.loans.map { if (it.id == loanId) it.copy(dueDate = it.dueDate + RENEWAL_DAYS * DAY_MS) else it }) }
    }

    // ---------------------------------------------------------------- Estantes

    fun saveShelf(shelf: ShelfEntity): Int {
        if (shelf.name.isBlank()) throw StoreException("Informe o nome da estante.")
        var savedId = shelf.id
        commit { d ->
            if (shelf.id == 0) {
                savedId = nextId(d.shelves) { it.id }
                d.copy(shelves = d.shelves + shelf.copy(id = savedId, name = shelf.name.trim()))
            } else {
                d.copy(shelves = d.shelves.map { if (it.id == shelf.id) shelf else it })
            }
        }
        return savedId
    }

    fun deleteShelf(shelfId: Int) = commit { d ->
        val removed = d.positions.filter { it.shelfId == shelfId }.map { it.id }.toSet()
        d.copy(
            shelves = d.shelves.filterNot { it.id == shelfId },
            positions = d.positions.filterNot { it.shelfId == shelfId },
            books = d.books.map { if (it.posicaoEstanteId in removed) it.copy(posicaoEstanteId = null) else it }
        )
    }

    fun addPosition(shelfId: Int, coordinate: String, accessibilityLevel: Int): Int {
        if (coordinate.isBlank()) throw StoreException("Informe a posição.")
        if (_data.value.shelves.none { it.id == shelfId }) throw StoreException("Estante não encontrada.")
        var id = 0
        commit { d ->
            id = nextId(d.positions) { it.id }
            d.copy(positions = d.positions + ShelfPositionEntity(id = id, shelfId = shelfId, accessibilityLevel = accessibilityLevel.coerceIn(1, 3), coordinate = coordinate.trim()))
        }
        return id
    }

    fun deletePosition(positionId: Int) = commit { d ->
        d.copy(
            positions = d.positions.filterNot { it.id == positionId },
            books = d.books.map { if (it.posicaoEstanteId == positionId) it.copy(posicaoEstanteId = null) else it }
        )
    }

    fun placeBook(bookId: Int, positionId: Int?) {
        if (positionId != null && _data.value.positions.none { it.id == positionId }) throw StoreException("Posição não encontrada.")
        commit { d -> d.copy(books = d.books.map { if (it.id == bookId) it.copy(posicaoEstanteId = positionId) else it }) }
    }

    // ---------------------------------------------------------------- Backup

    fun exportJson(): String = BackupFormat.toJson(_data.value.toSnapshot(), createdAt = clock())

    /** Substitui todos os dados pelos do backup (mesma regra da restauração v2 do Android). */
    fun importBackup(payload: BackupPayload) {
        BackupFormat.validate(payload)?.let { throw StoreException(it) }
        val version = payload.formatVersion ?: 1
        commit { cur ->
            if (version >= BackupFormat.CURRENT_FORMAT_VERSION) LibraryData.from(payload).sanitized()
            else cur.copy(books = payload.books.orEmpty(), loans = payload.loans.orEmpty()).sanitized()
        }
    }

    /** Fotos de outro aparelho não existem aqui; vínculos de posição órfãos são descartados. */
    private fun LibraryData.sanitized(): LibraryData {
        val positionIds = positions.map { it.id }.toSet()
        return copy(
            books = books.map { b ->
                val photo = b.photoPath?.takeIf { runCatching { Files.exists(Path.of(it)) }.getOrDefault(false) }
                val pos = b.posicaoEstanteId?.takeIf { it in positionIds }
                if (photo != b.photoPath || pos != b.posicaoEstanteId) b.copy(photoPath = photo, posicaoEstanteId = pos) else b
            }
        )
    }
}
