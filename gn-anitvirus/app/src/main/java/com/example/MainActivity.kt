package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AntivirusDatabase
import com.example.data.AntivirusRepository
import com.example.ui.AntivirusViewModel
import com.example.ui.AntivirusViewModelFactory
import com.example.ui.screens.AntivirusMainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Connect Room Database and Repository Layers (Constructor DI)
    val database = AntivirusDatabase.getDatabase(applicationContext)
    val dao = database.antivirusDao()
    val repository = AntivirusRepository(dao)
    
    // Instantiate Antivirus View Model securely
    val factory = AntivirusViewModelFactory(applicationContext, repository)
    val viewModel = ViewModelProvider(this, factory)[AntivirusViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AntivirusMainScreen(viewModel = viewModel)
      }
    }
  }
}

