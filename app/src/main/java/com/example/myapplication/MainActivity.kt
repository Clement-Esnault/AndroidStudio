package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.Result.Companion.success


class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = RaidRepository(this)

        val viewModel: RaidViewModel by viewModels { RaidViewModelFactory(repository) }

        setContent {
            MyApplicationTheme {
                // Compose observe les StateFlow du ViewModel
                val raids by viewModel.raids.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMsg by viewModel.errorMessage.collectAsState()
                val success by viewModel.syncSuccess.collectAsState()

                RaidApp(
                    viewModel = viewModel,
                    raids     = raids,
                    isLoading = isLoading,
                    errorMsg  = errorMsg,
                    success   = success
                )
            }
        }

        // Synchronisation initiale

    }
}
class RaidViewModelFactory(private val repository: RaidRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RaidViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RaidViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}