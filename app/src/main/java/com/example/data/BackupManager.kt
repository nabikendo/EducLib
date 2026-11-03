package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.io.File

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

object BackupManager {
    const val CURRENT_FORMAT_VERSION = 2
    const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    fun toJson(snapshot: BackupSnapshot): String {
        val portableSnapshot = snapshot.copy(
            books = snapshot.books.map { it.copy(photoPath = null) },
            borrowers = snapshot.borrowers.map { it.copy(photoUri = null) }
        )
        val payload = BackupPayload(
            formatVersion = CURRENT_FORMAT_VERSION,
            createdAt = System.currentTimeMillis(),
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

    fun readBackupText(context: Context, uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BACKUP_BYTES) { "O arquivo de backup é maior que 10 MB." }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }

    fun preview(json: String): BackupPreview {
        val payload = fromJson(json)
        if (payload == null) {
            return BackupPreview(0, null, 0, 0, 0, 0, 0, true, false, "O arquivo não contém um backup JSON válido.")
        }

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
        if (books.map { it.id }.distinct().size != books.size) return "O backup contém IDs de livros repetidos."
        if (loans.map { it.id }.distinct().size != loans.size) return "O backup contém IDs de empréstimos repetidos."
        if (books.any { it.title.isBlank() || it.author.isBlank() || it.totalQuantity < 0 || it.availableQuantity !in 0..it.totalQuantity }) {
            return "Há um livro com dados incompletos ou quantidade inválida."
        }
        if (loans.any { it.quantity <= 0 }) return "Há um empréstimo com quantidade inválida."
        val bookIds = books.map { it.id }.toSet()
        if (loans.any { it.bookId !in bookIds }) return "Há um empréstimo que não corresponde a nenhum livro do arquivo."

        if (version >= CURRENT_FORMAT_VERSION) {
            val borrowers = payload.borrowers ?: return "O backup está incompleto: falta a lista de leitores."
            val reservations = payload.reservations ?: return "O backup está incompleto: falta a lista de reservas."
            val shelves = payload.shelves ?: return "O backup está incompleto: falta a lista de estantes."
            val positions = payload.shelfPositions ?: return "O backup está incompleto: faltam as posições das estantes."
            if (payload.weeklySuggestions == null) return "O backup está incompleto: falta uma seção de dados."
            if (borrowers.map { it.id }.distinct().size != borrowers.size) return "O backup contém IDs de leitores repetidos."
            if (reservations.map { it.id }.distinct().size != reservations.size) return "O backup contém IDs de reservas repetidos."
            if (shelves.map { it.id }.distinct().size != shelves.size) return "O backup contém IDs de estantes repetidos."
            if (positions.map { it.id }.distinct().size != positions.size) return "O backup contém IDs de posições repetidos."
            val suggestions = payload.weeklySuggestions.orEmpty()
            if (suggestions.map { it.id }.distinct().size != suggestions.size) return "O backup contém IDs de sugestões repetidos."
            val shelfIds = shelves.map { it.id }.toSet()
            val positionIds = positions.map { it.id }.toSet()
            if (positions.any { it.shelfId !in shelfIds }) return "Há uma posição sem estante correspondente no arquivo."
            if (reservations.any { it.bookId !in bookIds }) return "Há uma reserva sem livro correspondente no arquivo."
            if (books.any { it.posicaoEstanteId != null && it.posicaoEstanteId !in positionIds }) {
                return "Há um livro vinculado a uma posição de estante que não está no arquivo."
            }
        }
        return null
    }

    fun saveAutoBackup(context: Context, snapshot: BackupSnapshot) {
        runCatching {
            val destination = File(context.filesDir, "library_backup_auto.json")
            val staging = File(context.filesDir, "library_backup_auto.tmp")
            staging.writeText(toJson(snapshot))
            if (!staging.renameTo(destination)) {
                staging.copyTo(destination, overwrite = true)
                staging.delete()
            }
        }
    }

    fun getShareIntent(context: Context, snapshot: BackupSnapshot): Intent? = runCatching {
        val file = File(context.cacheDir, "EducLib_backup_v2.json")
        file.writeText(toJson(snapshot))
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup do EducLib")
            putExtra(Intent.EXTRA_TEXT, "Cópia dos dados da biblioteca. Este arquivo contém dados de leitores; compartilhe apenas em local privado.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()

    fun getShareBooksCsvIntent(context: Context, books: List<BookEntity>): Intent? = runCatching {
        val sb = StringBuilder()
        sb.append("ID,Titulo,Autor,Genero,Ano,Quantidade Total,Quantidade Disponivel,Localizacao\n")
        books.forEach { book ->
            val title = book.title.replace(",", " ").replace("\"", "")
            val author = book.author.replace(",", " ").replace("\"", "")
            val genre = book.genre.replace(",", " ")
            val location = book.location.replace(",", " ")
            sb.append("${book.id},\"$title\",\"$author\",\"$genre\",${book.year},${book.totalQuantity},${book.availableQuantity},\"$location\"\n")
        }
        val file = File(context.cacheDir, "acervo_livros.csv")
        file.writeText(sb.toString())
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório do Acervo - EducLib")
            putExtra(Intent.EXTRA_TEXT, "Arquivo CSV do acervo de livros.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()
}
