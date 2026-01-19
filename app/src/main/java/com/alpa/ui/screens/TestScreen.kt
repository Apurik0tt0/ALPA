package com.alpa.ui.screens

import androidx.compose.foundation.lazy.items
import kotlin.collections.component1
import kotlin.collections.component2

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

import com.alpa.utils.SummitViewModel
import com.alpa.utils.SummitEntity

// --- 3. ÉCRAN PRINCIPAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(viewModel: SummitViewModel) {
    // 1. On "collecte" le flux de données.
    // La liste se mettra à jour automatiquement dès que la BDD change.
    val summits by viewModel.allSummits.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tous les sommets (${summits.size})") }
            )
        }
    ) { padding ->
        // 2. Affichage simple via LazyColumn
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summits, key = { it.id }) { summit ->
                SimpleSummitItem(summit)
            }
        }
    }
}

@Composable
fun SimpleSummitItem(summit: SummitEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = summit.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${summit.altitude} m • ${summit.groupName ?: "Aucun groupe"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Indicateur visuel (Validé ou non)
            if (summit.isValidated) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Validé",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "À faire",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}