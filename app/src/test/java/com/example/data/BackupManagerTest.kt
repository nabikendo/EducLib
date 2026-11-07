package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {
    @Test
    fun acceptsLegacyBackupAndMarksItAsLegacy() {
        val json = """
            {
              "books": [],
              "loans": []
            }
        """.trimIndent()

        val preview = BackupManager.preview(json)

        assertTrue(preview.isValid)
        assertTrue(preview.isLegacy)
        assertEquals(1, preview.formatVersion)
    }

    @Test
    fun rejectsUnsupportedFutureVersion() {
        val payload = BackupPayload(
            formatVersion = BackupManager.CURRENT_FORMAT_VERSION + 1,
            books = emptyList(),
            loans = emptyList()
        )

        assertTrue(BackupManager.validate(payload)?.contains("não é compatível") == true)
    }

    @Test
    fun rejectsLoansThatReferenceMissingBooks() {
        val payload = BackupPayload(
            formatVersion = 1,
            books = emptyList(),
            loans = listOf(
                LoanEntity(
                    id = 1,
                    bookId = 99,
                    bookTitle = "Livro ausente",
                    bookAuthor = "Autoria",
                    borrowerName = "Leitor",
                    borrowerClass = "Turma",
                    loanDate = 100,
                    dueDate = 200
                )
            )
        )

        assertTrue(BackupManager.validate(payload)?.contains("não corresponde") == true)
    }

    @Test
    fun portableJsonOmitsLocalBookAndBorrowerImages() {
        val snapshot = BackupSnapshot(
            books = listOf(
                BookEntity(
                    id = 1,
                    title = "O livro",
                    author = "Autoria",
                    genre = "Fantasia",
                    year = 2024,
                    location = "Estante A",
                    photoPath = "/data/local/capa.jpg",
                    totalQuantity = 2,
                    availableQuantity = 2
                )
            ),
            loans = emptyList(),
            borrowers = listOf(
                BorrowerEntity(
                    id = 2,
                    name = "Leitor",
                    type = "Aluno",
                    identifier = "7º A",
                    photoUri = "content://local/photo"
                )
            ),
            reservations = emptyList(),
            shelves = emptyList(),
            shelfPositions = emptyList(),
            weeklySuggestions = emptyList()
        )

        val json = BackupManager.toJson(snapshot)
        val restored = BackupManager.fromJson(json)

        assertTrue(json.contains("\"photoPath\":null"))
        assertTrue(json.contains("\"photoUri\":null"))
        assertEquals(BackupManager.CURRENT_FORMAT_VERSION, restored?.formatVersion)
        assertNull(restored?.let { BackupManager.validate(it) })
        assertNull(restored?.books?.singleOrNull()?.photoPath)
        assertNull(restored?.borrowers?.singleOrNull()?.photoUri)
        assertFalse(restored?.books.isNullOrEmpty())
    }

    @Test
    fun preservesHistoricalSuggestionsWhenReferencedBookWasRemoved() {
        val payload = BackupPayload(
            formatVersion = 2,
            books = emptyList(),
            loans = emptyList(),
            borrowers = emptyList(),
            reservations = emptyList(),
            shelves = emptyList(),
            shelfPositions = emptyList(),
            weeklySuggestions = listOf(
                WeeklySuggestionEntity(
                    id = 1,
                    bookId = 99,
                    bookTitle = "Livro ausente",
                    bookAuthor = "Autoria",
                    professorName = "Professora",
                    classRoom = "7º A",
                    recommendation = "Leitura recomendada"
                )
            )
        )

        assertNull(BackupManager.validate(payload))
    }

    @Test
    fun rejectsZeroIdsInPersistedBackupRecords() {
        val payload = BackupPayload(
            formatVersion = 1,
            books = listOf(
                BookEntity(
                    id = 0,
                    title = "Livro",
                    author = "Autoria",
                    genre = "Ficção",
                    year = 2024,
                    location = "Estante A",
                    photoPath = null,
                    totalQuantity = 1,
                    availableQuantity = 1
                )
            ),
            loans = emptyList()
        )

        assertTrue(BackupManager.validate(payload)?.contains("ID de livro") == true)
    }
}
