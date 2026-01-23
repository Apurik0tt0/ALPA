package com.alpa.ui.screens

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.alpa.utils.SummitViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: SummitViewModel) {
    val context = LocalContext.current

    // --- 1. DONNÉES ---
    val allSummits by viewModel.allSummits.collectAsState(initial = emptyList())
    val allGroups by viewModel.allGroups.collectAsState(initial = emptyList())

    // --- 2. ÉTATS DES FILTRES ---
    var showFilterSheet by remember { mutableStateOf(false) }
    var minAltitude by remember { mutableFloatStateOf(0f) }

    // null = "Tous" (Aucun filtre actif)
    // List<String> = Liste des groupes à afficher
    var selectedGroups by remember { mutableStateOf<List<String>?>(null) }

    // --- 3. LOGIQUE DE FILTRAGE ---
    val filteredSummits = remember(allSummits, minAltitude, selectedGroups) {
        allSummits.filter { summit ->
            val alt = summit.altitude ?: 0
            val matchAltitude = alt >= minAltitude

            // Si selectedGroups est null, on affiche tout.
            // Sinon, on vérifie si le groupe du sommet est dans la liste sélectionnée.
            val matchGroup = selectedGroups == null || (summit.groupName != null && selectedGroups!!.contains(summit.groupName))

            matchAltitude && matchGroup
        }
    }

    // Configuration OSM (Une seule fois)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "AlpaApp/1.0"
    }

    // --- 4. UI PRINCIPALE ---
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtres")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            // LA CARTE
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(9.0)
                        controller.setCenter(GeoPoint(45.9237, 6.8694)) // Chamonix par défaut
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    filteredSummits.forEach { summit ->
                        if (summit.latitude != null && summit.latitude != 0.0 && summit.longitude != null) {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(summit.latitude, summit.longitude)
                            marker.title = summit.name
                            marker.snippet = "${summit.altitude}m - ${summit.groupName ?: "Sans groupe"}"

                            marker.setOnMarkerClickListener { m, _ ->
                                m.showInfoWindow()
                                true
                            }

                            mapView.overlays.add(marker)
                        }
                    }
                    mapView.invalidate()
                }
            )

            // Feedback visuel nombre de sommets
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "${filteredSummits.size} sommets affichés",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    // --- 5. BOTTOM SHEET (MODALE DE FILTRES) ---
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filtrer la carte", style = MaterialTheme.typography.headlineSmall)

                HorizontalDivider()

                // -- Filtre Altitude --
                Text("Altitude minimum : ${minAltitude.toInt()} m", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = minAltitude,
                    onValueChange = { minAltitude = it },
                    valueRange = 0f..4810f,
                    steps = 47
                )

                // -- Filtre Groupe (Multi-sélection) --
                Text("Groupe / Massif", style = MaterialTheme.typography.titleMedium)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Puce "Tous"
                    item {
                        FilterChip(
                            selected = selectedGroups == null,
                            onClick = { selectedGroups = null }, // Reset à Tous
                            label = { Text("Tous") },
                            leadingIcon = if (selectedGroups == null) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    // Puces pour chaque groupe
                    items(allGroups.filterNotNull()) { group ->
                        val isSelected = selectedGroups?.contains(group) == true

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedGroups = if (selectedGroups == null) {
                                    // Cas 1 : On passe de "Tous" à "Ce groupe uniquement"
                                    listOf(group)
                                } else if (isSelected) {
                                    // Cas 2 : On retire le groupe
                                    val newList = selectedGroups!! - group
                                    // Si la liste devient vide, on repasse à "Tous" (null)
                                    if (newList.isEmpty()) null else newList
                                } else {
                                    // Cas 3 : On ajoute un groupe à la sélection existante
                                    selectedGroups!! + group
                                }
                            },
                            label = { Text(group) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // Boutons bas de page
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        minAltitude = 0f
                        selectedGroups = null
                    }) {
                        Text("Réinitialiser")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showFilterSheet = false }) {
                        Text("Voir la carte")
                    }
                }
            }
        }
    }
}