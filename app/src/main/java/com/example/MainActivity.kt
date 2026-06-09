package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.LibraryRepository
import com.example.ui.LibraryApp
import com.example.ui.LibraryViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    try {
      // Initialize standard offline local storage components
      val database = AppDatabase.getDatabase(this)
      val repository = LibraryRepository(database)
      val viewModelFactory = LibraryViewModel.Factory(repository)
      val viewModel = ViewModelProvider(this, viewModelFactory)[LibraryViewModel::class.java]

      enableEdgeToEdge(
        statusBarStyle = androidx.activity.SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
        ),
        navigationBarStyle = androidx.activity.SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
        )
      )
      setContent {
        LibraryApp(viewModel = viewModel)
      }
    } catch (t: Throwable) {
      android.util.Log.e("MainActivity", "Fatal initialization crash!", t)
      val stackTrace = android.util.Log.getStackTraceString(t)
      enableEdgeToEdge(
        statusBarStyle = androidx.activity.SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
        ),
        navigationBarStyle = androidx.activity.SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
        )
      )
      setContent {
        MyApplicationTheme {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error icon",
                tint = Color(0xFFBA1A1A),
                modifier = Modifier.size(64.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Erro Crítico de Inicialização",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Ocorreu um erro ao iniciar os componentes locais offline. Relatório clínico detalhado:",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(16.dp))
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = Color(0xFFFFF0F0)
                ),
                border = BorderStroke(1.dp, Color(0xFFFFB2B2)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = stackTrace,
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFF8B0000),
                  modifier = Modifier.padding(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

