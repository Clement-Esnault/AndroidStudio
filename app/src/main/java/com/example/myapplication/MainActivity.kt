package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.Raid
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.RaidViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RaidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sync au démarrage
        viewModel.sync()

        setContent {
            MyApplicationTheme {   // ← utilise ton thème, pas MaterialTheme directement
                RaidApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaidApp(viewModel: RaidViewModel) {
    val raids     = viewModel.raids
    val isLoading = viewModel.isLoading
    val errorMsg  = viewModel.errorMessage
    val success   = viewModel.syncSuccess

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raids") },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(32.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Synchroniser")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Bandeau succès
            AnimatedVisibility(
                visible = success,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "✅ ${raids.size} raid(s) synchronisé(s)",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Bandeau erreur
            errorMsg?.let {
                Surface(
                    color    = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = it,
                        modifier = Modifier.padding(12.dp),
                        color    = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (raids.isEmpty() && !isLoading) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun raid. Appuyez sur ↻ pour synchroniser.")
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(raids, key = { it.id }) { raid ->
                        RaidCard(raid)
                    }
                }
            }
        }
    }
}

@Composable
fun RaidCard(raid: Raid) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = raid.nom,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("📍 ${raid.lieu}",   style = MaterialTheme.typography.bodyMedium)
            Text("📅 Du ${raid.dateDebut} au ${raid.dateFin}",
                style = MaterialTheme.typography.bodySmall)
            Text("✉️ ${raid.mail}",   style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = when (raid.status) {
                    "VALIDE"     -> MaterialTheme.colorScheme.primaryContainer
                    "EN_ATTENTE" -> MaterialTheme.colorScheme.tertiaryContainer
                    else         -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text     = raid.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style    = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
