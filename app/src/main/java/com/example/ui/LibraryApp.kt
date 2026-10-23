package com.example.ui

  import android.content.Context
  import android.util.Log
  import android.content.Intent
  import android.net.Uri
  import android.widget.Toast
  import android.app.Activity
  import android.content.ContextWrapper
  import android.content.pm.ActivityInfo
  import androidx.activity.compose.rememberLauncherForActivityResult
  import androidx.activity.result.PickVisualMediaRequest
  import androidx.activity.result.contract.ActivityResultContracts
  import androidx.fragment.app.FragmentActivity
  import com.journeyapps.barcodescanner.ScanContract
  import com.journeyapps.barcodescanner.ScanOptions
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.animation.*
  import androidx.compose.animation.core.*
  import androidx.compose.foundation.*
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.text.KeyboardOptions
  import androidx.compose.ui.text.input.KeyboardType
  import androidx.compose.foundation.lazy.grid.GridCells
  import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
  import androidx.compose.foundation.lazy.grid.items
  import androidx.compose.foundation.lazy.items
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.automirrored.filled.ArrowBack
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.runtime.saveable.rememberSaveable
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.ui.graphics.graphicsLayer
  import com.example.data.WeeklySuggestionEntity
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.draw.shadow
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.graphics.drawscope.Stroke
  import androidx.compose.ui.layout.ContentScale
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.platform.testTag
  import androidx.compose.ui.res.painterResource
  import androidx.compose.ui.res.stringResource
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.text.font.FontFamily
  import androidx.compose.ui.text.font.FontStyle
  import androidx.compose.foundation.lazy.rememberLazyListState
  import androidx.compose.ui.text.input.PasswordVisualTransformation
  import androidx.compose.ui.text.input.VisualTransformation
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.text.style.TextOverflow
  import androidx.compose.ui.text.style.TextDecoration
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import androidx.compose.ui.window.Dialog
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import coil.compose.AsyncImage
  import com.example.R
  import com.example.data.BookEntity
  import com.example.data.LoanEntity
  import com.example.data.BorrowerEntity
  import com.example.data.ReservationEntity
  import com.example.data.ShelfEntity
  import com.example.data.ShelfPositionEntity
  import com.example.ui.theme.*
  import kotlinx.coroutines.launch
  import java.io.File
  import java.text.SimpleDateFormat
  import java.util.*

  @OptIn(ExperimentalAnimationApi::class)
  @Composable
  fun LibraryApp(viewModel: LibraryViewModel) {
      val isPasswordSet by viewModel.isPasswordSet.collectAsStateWithLifecycle()
      val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

      MyApplicationTheme {
          Box(modifier = Modifier.fillMaxSize()) {
              when (isPasswordSet) {
                  null -> {
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                          CircularProgressIndicator(color = ThemePurplePrimary)
                      }
                  }
                  false -> {
                      UnifiedAuthScreen(viewModel = viewModel, isSetupMode = true)
                  }
                  true -> {
                      if (isLoggedIn) {
                          MainLibraryAppContent(viewModel)
                      } else {
                          UnifiedAuthScreen(viewModel = viewModel, isSetupMode = false)
                      }
                  }
              }
          }
      }
  }

  @Composable
  fun MainLibraryAppContent(viewModel: LibraryViewModel) {
      var showAddBookScreen by remember { mutableStateOf(false) }
      val books by viewModel.allBooks.collectAsStateWithLifecycle()
      var searchQuery by remember { mutableStateOf("") }

      Scaffold(
          topBar = {
              CenterAlignedTopAppBar(
                  title = { Text("BiblioTech", fontWeight = FontWeight.ExtraBold, color = ThemePurplePrimary) },
                  colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
              )
          },
          floatingActionButton = {
              FloatingActionButton(
                  onClick = { showAddBookScreen = true },
                  containerColor = ThemePurplePrimary,
                  contentColor = Color.White,
                  shape = RoundedCornerShape(16.dp)
              ) {
                  Icon(Icons.Default.Add, contentDescription = "Adicionar Livro")
              }
          }
      ) { paddingValues ->
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(paddingValues)
                  .background(Color(0xFFF8FAFC))
          ) {
              Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                  OutlinedTextField(
                      value = searchQuery,
                      onValueChange = { searchQuery = it },
                      label = { Text("Buscar livro ou autor...") },
                      modifier = Modifier.fillMaxWidth(),
                      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                      shape = RoundedCornerShape(12.dp),
                      colors = OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = ThemePurplePrimary,
                          unfocusedBorderColor = Color(0xFFE2E8F0)
                      )
                  )

                  Spacer(modifier = Modifier.height(20.dp))

                  LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                      items(books) { book ->
                          BookItemCard(book = book, onDelete = { viewModel.deleteBook(book) })
                      }
                  }
              }

              if (showAddBookScreen) {
                  BookManagementScreen(
                      viewModel = viewModel,
                      onBack = { showAddBookScreen = false }
                  )
              }
          }
      }
  }

  @Composable
  fun BookItemCard(book: BookEntity, onDelete: () -> Unit) {
      Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
          Row(
              modifier = Modifier.padding(12.dp).fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
          ) {
              AsyncImage(
                  model = book.coverUrl,
                  contentDescription = null,
                  modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                  contentScale = ContentScale.Crop
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                  Text(text = book.title, fontWeight = FontWeight.Bold, maxLines = 1)
                  Text(text = book.author, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
              }
              IconButton(onClick = onDelete) {
                  Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
              }
          }
      }
  }
  // Funções auxiliares de UI para manter a compatibilidade com outras partes do app
  @Composable
  fun verticalScrollWithKeyboard(
      scrollState: androidx.compose.foundation.rememberScrollState(),
      content: @Composable () -> Unit
  ) {
      Box(modifier = Modifier.verticalScroll(scrollState)) {
          content()
      }
  }

  // Se você tiver outras funções de UI específicas que usava no LibraryApp.kt,
  // elas devem ser movidas para arquivos de componentes.
  // Esta versão do LibraryApp agora atua como o Orquestrador Principal.

  @Composable
  fun ApplicationStatusBar(userEmail: String) {
      Surface(
          color = ThemePurplePrimary.copy(alpha = 0.1f),
          modifier = Modifier.fillMaxWidth()
      ) {
          Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              Icon(
                  Icons.Default.AccountCircle,
                  contentDescription = null,
                  tint = ThemePurplePrimary,
                  modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  text = userEmail,
                  style = MaterialTheme.typography.bodySmall,
                  color = ThemePurplePrimary,
                  fontWeight = FontWeight.Medium
              )
          }
      }
  }
