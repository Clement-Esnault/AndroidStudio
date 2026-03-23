package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RaidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                // Compose observe les StateFlow du ViewModel
                val raids     = viewModel.raids.collectAsState()
                val isLoading = viewModel.isLoading.collectAsState()
                val errorMsg  = viewModel.errorMessage.collectAsState()
                val success   = viewModel.syncSuccess.collectAsState()

                RaidApp(
                    viewModel = viewModel,
                    raids     = raids.value,
                    isLoading = isLoading.value,
                    errorMsg  = errorMsg.value,
                    success   = success.value
                )
            }
        }

        // Synchronisation initiale
        viewModel.sync()
    }
}