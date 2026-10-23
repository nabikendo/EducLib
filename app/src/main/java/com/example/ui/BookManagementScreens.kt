package com.example.ui

  import androidx.activity.compose.rememberLauncherForActivityResult
  import androidx.compose.foundation.*
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.layout.ContentScale
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import coil.compose.AsyncImage
  import com.example.data.BookEntity
  import com.example.ui.theme.*
  import com.journeyapps.barcodescanner.ScanContract
  import com.journeyapps.barcodescanner.ScanOptions

  @Composable
  fun BookManagementScreen(viewModel: LibraryViewModel, onBack: () -> Unit) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFFF8FAFC))
              .verticalScroll(rememberScrollState())
              .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          // Header
          Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
          ) {
              IconButton(onClick = onBack) {
                  Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = ThemePurplePrimary)
              }
              Text(
                  text = "Adicionar Novo Livro",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = ThemePurplePrimary
              )
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Card de Cadastro
          Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(24.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
              Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                  // Preview da Capa do Livro
                  Box(
                      modifier = Modifier
                          .size(150.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(Color(0xFFE2E8F0)),
                      contentAlignment = Alignment.Center
                  ) {
                      if (viewModel.bookCoverUrl.isNotEmpty()) {
                          AsyncImage(
                              model = viewModel.bookCoverUrl,
                              contentDescription = "Capa do Livro",
                              modifier = Modifier.fillMaxSize(),
                              contentScale = ContentScale.Crop
                          )
                      } else {
                          Icon(
                              Icons.Default.Book,
                              contentDescription = null,
                              tint = Color(0xFF94A3B8),
                              modifier = Modifier.size(50.dp)
                          )
                      }
                  }

                  // Botão de Scan ISBN
                  Button(
                      onClick = { /* Triggered by the launcher below */ },
                      modifier = Modifier.fillMaxWidth(),
                      colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                      shape = RoundedCornerShape(12.dp),
                      enabled = !viewModel.isFetchingBook.collectAsState().value
                  ) {
                      // O trigger real acontece no launcher สำหรับactivity result
                  }

                  // Launcher para abrir a Câmera (ZXing)
                  val scanLauncher = rememberLauncherForActivityResult(
                      contract = ScanContract(),
                      onResult = { result ->
                          if (result.contents != null) {
                              viewModel.processScannedIsbn(result.contents)
                          }
                      }
                  )

                  // Botão de Scan Real
                  OutlinedButton(
                      onClick = {
                          val options = ScanOptions().apply {
                              setDesiredBarcodeFormats(ScanOptions.ISBN_10)
                              setDesiredBarcodeFormats(ScanOptions.ISBN_13)
                              setPrompt("Posicione o código de barras do livro")
                              setBeepEnabled(true)
                              setOrientationLocked(false)
                          }
                          scanLauncher.launch(options)
                      },
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, ThemePurplePrimary)
                  ) {
                      Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = ThemePurplePrimary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Escanear ISBN", color = ThemePurplePrimary, fontWeight = FontWeight.Bold)
                  }

                  if (viewModel.isFetchingBook.collectAsState().value) {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ThemePurplePrimary)
                  }

                  Divider(color = Color(0xFFE2E8F0))

                  // Campos de Texto
                  BookInputField(label = "Título", value = viewModel.bookTitle, onValueChange = { viewModel.bookTitle = it })
                  BookInputField(label = "Autor", value = viewModel.bookAuthor, onValueChange = { viewModel.bookAuthor = it })
                  BookInputField(label = "ISBN", value = viewModel.bookIsbn, onValueChange = { viewModel.bookIsbn = it })
                  BookInputField(label = "Ano", value = viewModel.bookYear, onValueChange = { viewModel.bookYear = it })
                  BookInputField(label = "Categoria", value = viewModel.bookCategory, onValueChange = { viewModel.bookCategory =
  it })

                  OutlinedTextField(
                      value = viewModel.bookDescription,
                      onValueChange = { viewModel.bookDescription = it },
                      label = { Text("Descrição") },
                      modifier = Modifier.fillMaxWidth().height(120.dp),
                      shape = RoundedCornerShape(12.dp),
                      maxLines = 5
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  Button(
                      onClick = { viewModel.saveCurrentBook() },
                      modifier = Modifier.fillMaxWidth().height(50.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                      shape = RoundedCornerShape(12.dp)
                  ) {
                      Text("Salvar Livro no Acervo", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                  }
              }
          }
      }
  }

  @Composable
  fun BookInputField(label: String, value: String, onValueChange: (String) -> Unit) {
      OutlinedTextField(
          value = value,
          onValueChange = onValueChange,
          label = { Text(label) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ThemePurplePrimary,
              unfocusedBorderColor = Color(0xFFE2E8F0)
          )
      )
  }
