package com.example.ui

  import android.app.Application
  import android.content.Context
  import android.util.Log
  import androidx.lifecycle.*
  import com.example.data.*
  import com.example.network.GoogleBooksApi
  import kotlinx.coroutines.*
  import kotlinx.coroutines.flow.*

  class LibraryViewModel(application: Application) : AndroidViewModel(application) {

      private val repository = LibraryRepository(application)
      private val context = application.applicationContext

      // --- ESTADOS DE AUTENTICAÇÃO ---
      private val _isPasswordSet = MutableStateFlow<Boolean?>(null)
      val isPasswordSet: StateFlow<Boolean?> = _isPasswordSet

      private val _isLoggedIn = MutableStateFlow(false)
      val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

      // --- ESTADOS DE LIVROS ---
      val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

      // Estados para o formulário de cadastro de livros (usados para auto-preenchimento via ISBN)
      var bookTitle by mutableStateOf("")
      var bookAuthor by mutableStateOf("")
      var bookIsbn by mutableStateOf("")
      var bookYear by mutableStateOf("")
      var bookCoverUrl by mutableStateOf("")
      var bookCategory by mutableStateOf("")
      var bookDescription by mutableStateOf("")

      private val _isFetchingBook = MutableStateFlow(false)
      val isFetchingBook: StateFlow<Boolean> = _isFetchingBook

      // --- LÓGICA DE SCANNER E API ---

      /**
       * Esta função é chamada quando o scanner de código de barras retorna um ISBN.
       * Ela busca os dados no Google Books e preenche os campos da UI automaticamente.
       */
      fun processScannedIsbn(isbn: String) {
          viewModelScope.launch {
              _isFetchingBook.value = true
              bookIsbn = isbn

              val bookData = repository.fetchBookByIsbn(isbn)
              if (bookData != null) {
                  // Auto-preenchimento dos campos do formulário
                  bookTitle = bookData.title
                  bookAuthor = bookData.author
                  bookYear = bookData.year
                  bookCoverUrl = bookData.coverUrl
                  bookCategory = bookData.category
                  bookDescription = bookData.description
                  Log.d("LibViewModel", "Livro preenchido automaticamente via ISBN")
              } else {
                  Log.e("LibViewModel", "Livro não encontrado na API para o ISBN: $isbn")
              }
              _isFetchingBook.value = false
          }
      }

      fun saveCurrentBook() {
          viewModelScope.launch {
              val newBook = BookEntity(
                  title = bookTitle,
                  author = bookAuthor,
                  isbn = bookIsbn,
                  year = bookYear,
                  coverUrl = bookCoverUrl,
                  category = bookCategory,
                  description = bookDescription,
                  shelfId = 0,
                  status = "Available"
              )
              repository.addBook(newBook)
              clearBookForm()
          }
      }

      private fun clearBookForm() {
          bookTitle = ""
          bookAuthor = ""
          bookIsbn = ""
          bookYear = ""
          bookCoverUrl = ""
          bookCategory = ""
          bookDescription = ""
      }

      // --- AUTENTICAÇÃO ---

      fun checkPasswordStatus() {
          viewModelScope.launch {
              val pass = repository.getConfig("master_password")
              _isPasswordSet.value = pass != null
          }
      }

      suspend fun login(password: String): Boolean {
          val storedPass = repository.getConfig("master_password")
          return if (storedPass == password) {
              _isLoggedIn.value = true
              true
          } else {
              false
          }
      }

      suspend fun loginWithEmail(email: String, pass: String): Boolean {
          // Simulação de login por e-mail (integrar com Firebase no futuro)
          if (email.isNotEmpty() && pass.isNotEmpty()) {
              _isLoggedIn.value = true
              return true
          }
          return false
      }

      fun registerEmailAndPassword(email: String, pass: String, name: String) {
          viewModelScope.launch {
              repository.saveConfig("master_password", pass)
              repository.saveConfig("user_email", email)
              repository.saveConfig("user_name", name)
              _isPasswordSet.value = true
              _isLoggedIn.value = true
          }
      }

      fun loginWithGoogle(email: String, name: String, photo: String) {
          viewModelScope.launch {
              repository.saveConfig("user_email", email)
              repository.saveConfig("user_name", name)
              repository.saveConfig("user_photo", photo)
              _isLoggedIn.value = true
          }
      }

      fun guestLogin() {
          _isLoggedIn.value = true
      }

      // --- OUTRAS OPERAÇÕES ---

      fun deleteBook(book: BookEntity) {
          viewModelScope.launch { repository.deleteBook(book) }
      }

      fun searchBooks(query: String): LiveData<List<BookEntity>> {
          // Adaptado para retornar LiveData para compatibilidade com a UI atual
          return liveData {
              emit(repository.searchBooks(query))
          }
      }
  }
