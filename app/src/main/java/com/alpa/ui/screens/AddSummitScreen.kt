package com.alpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

// On crée une petite classe de données temporaire pour les résultats de la "fausse" recherche API
data class ApiResult(val name: String, val lat: Double, val lon: Double, val altitude: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSummitScreen(
    onBack: () -> Unit,
    onSummitAdded: (String, Int, String?) -> Unit // Callback pour valider l'ajout (Nom, Alt, Groupe)
) {
    // État de l'onglet sélectionné (0 = Manuel, 1 = Carte)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Manuel", "Carte")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un sommet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- 1. LES ONGLETS (TABS) ---
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                if (index == 0) Icons.Default.Edit else Icons.Default.Map,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            // --- 2. LE CONTENU ---
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (selectedTabIndex == 0) {
                    ManualEntryContent(onAddClick = onSummitAdded)
                } else {
                    MapSearchContent(onAddFromApi = onSummitAdded)
                }
            }
        }
    }
}

// --- CONTENU ONGLET 1 : SAISIE MANUELLE ---
@Composable
fun ManualEntryContent(onAddClick: (String, Int, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var altitude by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Informations du sommet", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom du sommet") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = altitude,
            onValueChange = { if (it.all { char -> char.isDigit() }) altitude = it },
            label = { Text("Altitude (m)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        OutlinedTextField(
            value = group,
            onValueChange = { group = it },
            label = { Text("Groupe / Massif (Optionnel)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isNotBlank() && altitude.isNotBlank()) {
                    onAddClick(name, altitude.toInt(), group.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && altitude.isNotBlank()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter à ma liste")
        }
    }
}

// --- CONTENU ONGLET 2 : CARTE & RECHERCHE ---
@Composable
fun MapSearchContent(onAddFromApi: (String, Int, String?) -> Unit) {
    var searchRadius by remember { mutableFloatStateOf(5f) } // 5km par défaut
    var isSearching by remember { mutableStateOf(false) }

    // Liste simulée des résultats
    val searchResults = remember { mutableStateListOf<ApiResult>() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // --- LA CARTE (Placeholder Carré Vert) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(color = Color(0xFF4CAF50), shape = RoundedCornerShape(12.dp)), // Vert Material Design
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text(
                    "Carte OpenStreetMap",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "(Cliquer pour placer un point)",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // --- CONTRÔLES DE RECHERCHE ---
        Text("Rayon de recherche : ${searchRadius.toInt()} km", style = MaterialTheme.typography.labelLarge)

        Slider(
            value = searchRadius,
            onValueChange = { searchRadius = it },
            valueRange = 1f..50f,
            steps = 49
        )

        Button(
            onClick = {
                // Simulation d'un appel API
                isSearching = true
                searchResults.clear()
                // On simule un délai réseau imaginaire
                // (Dans la réalité, on lancerait une coroutine ici)
                searchResults.add(ApiResult("Pic Simulé 1", 45.0, 6.0, 2400))
                searchResults.add(ApiResult("Mont Test", 45.1, 6.1, 3100))
                searchResults.add(ApiResult("Aiguille du Code", 45.2, 6.2, 1850))
                isSearching = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rechercher les sommets alentours")
        }

        Divider()

        // --- RÉSULTATS ---
        if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Sélectionnez une zone et lancez la recherche.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { result ->
                    ListItem(
                        headlineContent = { Text(result.name) },
                        supportingContent = { Text("Alt: ${result.altitude}m") },
                        leadingContent = { Icon(Icons.Default.Map, null) },
                        trailingContent = {
                            IconButton(onClick = {
                                // Ajout direct depuis la liste
                                onAddFromApi(result.name, result.altitude, "Import Carte")
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}