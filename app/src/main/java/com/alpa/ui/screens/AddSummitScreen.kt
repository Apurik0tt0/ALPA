package com.alpa.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alpa.utils.SummitViewModel

import com.alpa.utils.SummitEntity

// On crée une petite classe de données temporaire pour les résultats de la "fausse" recherche API
data class ApiResult(val name: String, val lat: Double, val lon: Double, val altitude: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSummitScreen(
    onBack: () -> Unit,
    onSummitAdded: () -> Unit, // Callback pour valider l'ajout (Nom, Alt, Groupe)
    viewModel: SummitViewModel
) {
    // État de l'onglet sélectionné (0 = Manuel, 1 = Carte)
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
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
                    ManualEntryContent(onAddClick = onSummitAdded, viewModel)
                } else {
                    MapSearchContent(onAddFromApi = onSummitAdded)
                }
            }
        }
    }
}

// --- CONTENU ONGLET 1 : SAISIE MANUELLE ---
@Composable
fun ManualEntryContent(onAddClick: () -> Unit, viewModel: SummitViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var altitude by rememberSaveable { mutableStateOf("") }
    var selectedGroupName by rememberSaveable { mutableStateOf("") }
    var locationName by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }

    // Récupération des groupes existants depuis le ViewModel
    val existingGroups by viewModel.allGroups.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize() // Prend toute la place
            .verticalScroll(scrollState) // Rend le contenu scrollable
            .padding(bottom = 16.dp), // Évite que le bouton colle au bord
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        // Champ Lieux (Simple texte)
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Lieu") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Champs Latitude et Longitude côte à côte
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = latitude,
                onValueChange = { input ->
                    // Autorise les chiffres, le point décimal et le signe moins
                    if (input.isEmpty() || input.matches(Regex("""^-?\d*\.?\d*$"""))) {
                        latitude = input
                    }
                },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = longitude,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("""^-?\d*\.?\d*$"""))) {
                        longitude = input
                    }
                },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }

        GroupSelector(
            existingGroups = existingGroups,
            onGroupSelected = { name ->
                selectedGroupName = name
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isNotBlank() && altitude.isNotBlank()) {
                    Log.d("D","$name, $altitude, $selectedGroupName")
                    viewModel.addSummit(SummitEntity(name = name, altitude = altitude.toIntOrNull(), groupName = (selectedGroupName.ifBlank { null }), location = locationName, latitude = latitude.toDoubleOrNull(), longitude = longitude.toDoubleOrNull()))
                    //Ajouter toast et retour à la liste
                    Toast.makeText(
                        context,
                        "Sommet \"$name\" ajouté !",
                        Toast.LENGTH_SHORT
                    ).show()
                    onAddClick()
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelector(
    existingGroups: List<String>,
    onGroupSelected: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredGroups = existingGroups.filter { group ->
        //?. pour ne pas crasher si group est null
        group?.contains(textValue, ignoreCase = true) == true
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it } // On laisse Material gérer l'ouverture standard
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                // On n'ouvre le menu que s'il y a quelque chose à montrer
                expanded = it.isNotEmpty()
                onGroupSelected(it)
            },
            label = { Text("Sélectionner un groupe") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    expanded = false
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true) // Très important pour lier la popup au champ
                .fillMaxWidth()
        )

        // On ne dessine le menu que s'il y a des suggestions
        if (filteredGroups.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            textValue = group
                            expanded = false
                            onGroupSelected(group)
                            focusManager.clearFocus()
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}


// --- CONTENU ONGLET 2 : CARTE & RECHERCHE ---
@Composable
fun MapSearchContent(onAddFromApi: () -> Unit) {
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
                                // Retour sur la liste
                                onAddFromApi()
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