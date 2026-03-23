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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RaidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.sync()
        setContent {
            MyApplicationTheme {
                RaidApp(viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────── //
//  Écran principal
// ─────────────────────────────────────────────────────────────────────────── //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaidApp(viewModel: RaidViewModel) {
    val raids     = viewModel.raids
    val isLoading = viewModel.isLoading
    val errorMsg  = viewModel.errorMessage
    val success   = viewModel.syncSuccess

    // État du dialogue (null = fermé, Raid vide = création, Raid existant = édition)
    var dialogRaid by remember { mutableStateOf<Raid?>(null) }
    var deleteTarget by remember { mutableStateOf<Raid?>(null) }

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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Ouvre le dialogue de création avec un Raid vide
                dialogRaid = Raid()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un raid")
            }
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
                    Text("Aucun raid. Appuyez sur + pour en créer un.")
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(raids, key = { it.id }) { raid ->
                        RaidCard(
                            raid     = raid,
                            onEdit   = { dialogRaid = raid },
                            onDelete = { deleteTarget = raid }
                        )
                    }
                }
            }
        }
    }

    // ── Dialogue Ajout / Édition ────────────────────────────────────────── //
    dialogRaid?.let { raid ->
        RaidDialog(
            raid     = raid,
            isNew    = raid.id == 0,
            onDismiss = { dialogRaid = null },
            onConfirm = { edited ->
                if (raid.id == 0) viewModel.create(edited)
                else              viewModel.edit(edited)
                dialogRaid = null
            }
        )
    }

    // ── Dialogue Confirmation Suppression ──────────────────────────────── //
    deleteTarget?.let { raid ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Supprimer ?") },
            text    = { Text("Voulez-vous supprimer « ${raid.nom} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(raid.id)
                    deleteTarget = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Annuler") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────── //
//  Card raid avec boutons Éditer / Supprimer
// ─────────────────────────────────────────────────────────────────────────── //

@Composable
fun RaidCard(raid: Raid, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = raid.nom,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("📍 ${raid.lieu}",  style = MaterialTheme.typography.bodyMedium)
            Text("📅 Du ${raid.dateDebut} au ${raid.dateFin}",
                style = MaterialTheme.typography.bodySmall)
            Text("✉️ ${raid.mail}", style = MaterialTheme.typography.bodySmall)
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

// ─────────────────────────────────────────────────────────────────────────── //
//  Dialogue Ajout / Édition
// ─────────────────────────────────────────────────────────────────────────── //

@Composable
fun RaidDialog(
    raid: Raid,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Raid) -> Unit
) {
    var nom       by remember { mutableStateOf(raid.nom) }
    var lieu      by remember { mutableStateOf(raid.lieu) }
    var dateDebut by remember { mutableStateOf(raid.dateDebut) }
    var dateFin   by remember { mutableStateOf(raid.dateFin) }
    var mail      by remember { mutableStateOf(raid.mail) }
    var status    by remember { mutableStateOf(raid.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nouveau raid" else "Modifier le raid") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nom,       onValueChange = { nom = it },
                    label = { Text("Nom") },       modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lieu,      onValueChange = { lieu = it },
                    label = { Text("Lieu") },      modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dateDebut, onValueChange = { dateDebut = it },
                    label = { Text("Date début (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dateFin,   onValueChange = { dateFin = it },
                    label = { Text("Date fin (YYYY-MM-DD)") },   modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mail,      onValueChange = { mail = it },
                    label = { Text("Mail") },      modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = status,    onValueChange = { status = it },
                    label = { Text("Statut") },    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick  = {
                    onConfirm(raid.copy(
                        nom       = nom,
                        lieu      = lieu,
                        dateDebut = dateDebut,
                        dateFin   = dateFin,
                        mail      = mail,
                        status    = status
                    ))
                },
                enabled = nom.isNotBlank() && lieu.isNotBlank()
            ) { Text(if (isNew) "Créer" else "Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}