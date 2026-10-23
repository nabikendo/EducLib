package com.example.data

  import android.content.Context
  import android.util.Log
  import androidx.room.Room
  import com.example.network.GoogleBooksApi
  import com.example.network.ApiClient
  import kotlinx.coroutines.flow.*
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import java.io.File
  import java.text.SimpleDateFormat
  import java.util.*

  class LibraryRepository(private val context: Context) {

      private val db = Room.databaseBuilder(
          context,
          AppDatabase::class.java, "library_db"
      ).build()

      private val bookDao = db.bookDao()
      private val borrowerDao = db.borrowerDao()
      private val loanDao = db.loanDao()
      private val reservationDao = db.reservationDao()
      private val shelfDao = db.shelfDao()
      private val configDao = db.configDao()
      private val suggestionDao = db.weeklySuggestionDao()

      private val googleBooksApi = ApiClient.googleBooksApi

      // --- LIVROS ---
      val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

      suspend fun addBook(book: BookEntity) = withContext(Dispatchers.IO) {
          bookDao.insertBook(book)
      }

      suspend fun updateBook(book: BookEntity) = withContext(Dispatchers.IO) {
          bookDao.updateBook(book)
      }

      suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
          bookDao.deleteBook(book)
      }

      suspend fun searchBooks(query: String): List<BookEntity> = withContext(Dispatchers.IO) {
          bookDao.searchBooks(query)
      }

      /**
       * Busca dados de um livro via ISBN na Google Books API
       * Retorna um BookEntity preenchido com os dados da web para auto-preenchimento na UI
       */
      suspend fun fetchBookByIsbn(isbn: String): BookEntity? = withContext(Dispatchers.IO) {
          try {
              Log.d("LibraryRepo", "Buscando livro ISBN: $isbn")
              val response = googleBooksApi.getBookByIsbn(isbn)

              if (response.items != null && response.items.isNotEmpty()) {
                  val volumeInfo = response.items[0].volumeInfo
                  Log.d("LibraryRepo", "Livro encontrado: ${volumeInfo.title}")

                  return@withContext BookEntity(
                      title = volumeInfo.title ?: "Título desconhecido",
                      author = volumeInfo.authors?.joinToString(", ") ?: "Autor desconhecido",
                      isbn = isbn,
                      year = volumeInfo.publishedDate?.take(4) ?: "N/A",
                      coverUrl = volumeInfo.imageLinks?.thumbnail ?: "",
                      category = volumeInfo.categories?.firstOrNull() ?: "Geral",
                      description = volumeInfo.description ?: "",
                      shelfId = 0,
                      status = "Available"
                  )
              }
          } catch (e: Exception) {
              Log.e("LibraryRepo", "Erro ao buscar ISBN: ${e.message}")
          }
          null
      }

      // --- EMPRÉSTIMOS ---
      val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()

      suspend fun createLoan(loan: LoanEntity) = withContext(Dispatchers.IO) {
          loanDao.insertLoan(loan)
      }

      suspend fun returnBook(loanId: Long) = withContext(Dispatchers.IO) {
          loanDao.markAsReturned(loanId)
      }

      // --- USUÁRIOS/LEITORES ---
      val allBorrowers: Flow<List<BorrowerEntity>> = borrowerDao.getAllBorrowers()

      suspend fun addBorrower(borrower: BorrowerEntity) = withContext(Dispatchers.IO) {
          borrowerDao.insertBorrower(borrower)
      }

      // --- RESERVAS ---
      val allReservations: Flow<List<ReservationEntity>> = reservationDao.getAllReservations()

      suspend fun addReservation(reservation: ReservationEntity) = withContext(Dispatchers.IO) {
          reservationDao.insertReservation(reservation)
      }

      // --- PRATELEIRAS ---
      val allShelves: Flow<List<ShelfEntity>> = shelfDao.getAllShelves()

      suspend fun addShelf(shelf: ShelfEntity) = withContext(Dispatchers.IO) {
          shelfDao.insertShelf(shelf)
      }

      // --- CONFIGURAÇÕES E BACKUP ---
      suspend fun saveConfig(key: String, value: String) = withContext(Dispatchers.IO) {
          configDao.insertConfig(ConfigEntity(key = key, value = value))
      }

      suspend fun getConfig(key: String): String? = withContext(Dispatchers.IO) {
          configDao.getConfig(key)?.value
      }

      // --- SUGESTÕES SEMANAIS (IA) ---
      suspend fun saveWeeklySuggestion(suggestion: WeeklySuggestionEntity) = withContext(Dispatchers.IO) {
          suggestionDao.insertSuggestion(suggestion)
      }

      val weeklySuggestions: Flow<List<WeeklySuggestionEntity>> = suggestionDao.getAllSuggestions()
  }

  ---
