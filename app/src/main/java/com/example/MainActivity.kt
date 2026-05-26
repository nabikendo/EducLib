package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.LibraryRepository
import com.example.ui.LibraryApp
import com.example.ui.LibraryViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize standard offline local storage components
    val database = AppDatabase.getDatabase(this)
    val repository = LibraryRepository(database)
    val viewModelFactory = LibraryViewModel.Factory(repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[LibraryViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      LibraryApp(viewModel = viewModel)
    }
  }
}
