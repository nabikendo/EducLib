package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/*
 * Formato portátil de backup do EducLib (JSON v2), sem dependência de Android.
 * Compartilhado pelo app Android e pelo app de desktop: um backup feito em um abre no outro.
 * O contrato está documentado em IOS_PORTING_GUIDE.md.
 */

/** Snapshot portátil dos dados da biblioteca; credenciais e configurações privadas ficam fora. */
data class BackupSnapshot(
    val books: List<BookEntity>,
    val loans: List<LoanEntity>,
    val borrowers: List<BorrowerEntity>,
    val reservations: List<ReservationEntity>,
    val shelves: List<ShelfEntity>,
    val shelfPositions: List<ShelfPositionEntity>,
    val weeklySuggestions: List<WeeklySuggestionEntity>
)

data class BackupPayload(
    val formatVersion: Int? = null,
    val createdAt: Long? = null,
    val books: List<BookEntity>? = null,
    val loans: List<LoanEntity>? = null,
    val borrowers: List<BorrowerEntity>? = null,
    val reservations: List<ReservationEntity>? = null,
    val shelves: List<ShelfEntity>? = null,
    val shelfPositions: List<ShelfPositionEntity>? = null,
    val weeklySuggestions: List<WeeklySuggestionEntity>? = null
)

data class BackupPreview(
    val formatVersion: Int,
    val createdAt: Long?,
    val bookCount: Int,
    val loanCount: Int,
    val borrowerCount: Int,
    val reservationCount: Int,
    val shelfCount: Int,
    val isLegacy: Boolean,
    val isValid: Boolean,
    val problem: String? = null
)

object BackupFormat {
    const val CURRENT_FORMAT_VERSION = 2
    const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    fun toJson(snapshot: BackupSnapshot, createdAt: Long = System.currentTimeMillis()): String {
        val portableSnapshot = snapshot.copy(
            books = snapshot.books.map { it.copy(photoPath = null) },
            borrowers = snapshot.borrowers.map { borrower ->
                borrower.copy(photoUri = borrower.photoUri?.takeIf { it.startsWith("avatar:") })
            }
        )
        val payload = BackupPayload(
            formatVersion = CURRENT_FORMAT_VERSION,
            createdAt = createdAt,
            books = portableSnapshot.books,
            loans = portableSnapshot.loans,
            borrowers = portableSnapshot.borrowers,
            reservations = portableSnapshot.reservations,
            shelves = portableSnapshot.shelves,
            shelfPositions = portableSnapshot.shelfPositions,
            weeklySuggestions = portableSnapshot.weeklySuggestions
        )
        return adapter.serializeNulls().toJson(payload)
    }

    fun fromJson(json: String): BackupPayload? = try {
        adapter.fromJson(json)
    } catch (_: Exception) {
        null
    }

    fun preview(json: String): BackupPreview {
        val payload = fromJson(json)
            ?: return BackupPreview(0, null, 0, 0, 0, 0, 0, true, false, "O arquivo não contém um backup JSON válido.")
        return preview(payload)
    }

    fun preview(payload: BackupPayload): BackupPreview {

        val version = payload.formatVersion ?: 1
        val books = payload.books.orEmpty()
        val loans = payload.loans.orEmpty()
        val legacy = version < CURRENT_FORMAT_VERSION
        val issue = validate(payload)
        return BackupPreview(
            formatVersion = version,
            createdAt = payload.createdAt,
            bookCount = books.size,
            loanCount = loans.size,
            borrowerCount = payload.borrowers.orEmpty().size,
            reservationCount = payload.reservations.orEmpty().size,
            shelfCount = payload.shelves.orEmpty().size,
            isLegacy = legacy,
            isValid = issue == null,
            problem = issue
        )
    }

    fun validate(payload: BackupPayload): String? {
        val version = payload.formatVersion ?: 1
        if (version !in 1..CURRENT_FORMAT_VERSION) return "Esta versão do backup não é compatível com o app."
        val books = payload.books ?: return "O arquivo não tem a lista de livros necessária."
        val loans = payload.loans ?: return "O arquivo não tem o histórico de empréstimos necessário."
        if (books.any { it.id <= 0 } || loans.any { it.id <= 0 }) return "O backup contém um ID de livro ou empréstimo inválido."
        if (books.map { it.id }.distinct().size != books.size) return "O backup contém IDs de livros repetidos."
        if (loans.map { it.id }.distinct().size != loans.size) return "O backup contém IDs de empréstimos repetidos."
        if (books.any { it.title.isBlank() || it.author.isBlank() || it.totalQuantity < 0 || it.availableQuantity !in 0..it.totalQuantity }) {
            return "Há um livro com dados incompletos ou quantidade inválida."
        }
        if (loans.any { it.quantity <= 0 }) return "Há um empréstimo com quantidade inválida."
        val bookIds = books.map { it.id }.toSet()
        // Empréstimos devolvidos são histórico (guardam título e autor) e podem sobreviver à exclusão do livro.
        if (loans.any { it.returnDate == null && it.bookId !in bookIds }) return "Há um empréstimo ativo que não corresponde a nenhum livro do arquivo."

        if (version >= CURRENT_FORMAT_VERSION) {
            val borrowers = payload.borrowers ?: return "O backup está incompleto: falta a lista de leitores."
            val reservations = payload.reservations ?: return "O backup está incompleto: falta a lista de reservas."
            val shelves = payload.shelves ?: return "O backup está incompleto: falta a lista de estantes."
            val positions = payload.shelfPositions ?: return "O backup está incompleto: faltam as posições das estantes."
            if (payload.weeklySuggestions == null) return "O backup está incompleto: falta uma seção de dados."
            val suggestions = payload.weeklySuggestions.orEmpty()
            if (borrowers.any { it.id <= 0 } || reservations.any { it.id <= 0 } || shelves.any { it.id <= 0 } ||
                positions.any { it.id <= 0 } || suggestions.any { it.id <= 0 }
            ) return "O backup contém um ID de leitor, reserva, estante, posição ou sugestão inválido."
            if (borrowers.map { it.id }.distinct().size != borrowers.size) return "O backup contém IDs de leitores repetidos."
            if (reservations.map { it.id }.distinct().size != reservations.size) return "O backup contém IDs de reservas repetidos."
            if (shelves.map { it.id }.distinct().size != shelves.size) return "O backup contém IDs de estantes repetidos."
            if (positions.map { it.id }.distinct().size != positions.size) return "O backup contém IDs de posições repetidos."
            if (suggestions.map { it.id }.distinct().size != suggestions.size) return "O backup contém IDs de sugestões repetidos."
            val shelfIds = shelves.map { it.id }.toSet()
            if (positions.any { it.shelfId !in shelfIds }) return "Há uma posição sem estante correspondente no arquivo."
            // Versões até a 16.5 apagavam o livro e mantinham as reservas dele; a restauração descarta essas
            // reservas órfãs em vez de recusar o arquivo.
            // `books.posicaoEstanteId` não é FK: versões anteriores deixavam vínculos órfãos ao apagar
            // uma estante. A restauração descarta esses vínculos em vez de recusar o backup inteiro.
            if (shelves.any { !it.hasValidLayout() }) return "Há uma estante com medidas inválidas no arquivo."
        }
        return null
    }

    private fun ShelfEntity.hasValidLayout(): Boolean {
        fun Double.inRange(min: Double, max: Double) = !isNaN() && this > min && this <= max
        val coordsOk = listOfNotNull(mapX, mapZ).all { !it.isNaN() && kotlin.math.abs(it) <= 1_000.0 }
        return coordsOk && !rotationDeg.isNaN() &&
            widthM.inRange(0.0, ShelfEntity.MAX_DIMENSION_M) &&
            depthM.inRange(0.0, ShelfEntity.MAX_DIMENSION_M) &&
            heightM.inRange(0.0, ShelfEntity.MAX_DIMENSION_M) &&
            levelCount in 1..ShelfEntity.MAX_LEVELS &&
            columnCount in 1..ShelfEntity.MAX_COLUMNS
    }
}
