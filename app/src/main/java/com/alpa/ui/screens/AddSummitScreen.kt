package com.alpa.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Save
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
import com.alpa.utils.ApiSummit
import com.alpa.utils.SummitViewModel

import com.alpa.utils.SummitEntity
import com.alpa.utils.testOverpassCall
//------------
import android.preference.PreferenceManager
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility

import androidx.compose.material.icons.filled.*

import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

// IMPORTS OSMDROID
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.*


fun distanceInKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadius = 6371.0 // Rayon de la Terre en km

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

// On crée une petite classe de données temporaire pour les résultats de la "fausse" recherche API


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
                    MapSearchContent(onFinished = onSummitAdded, viewModel = viewModel)
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



// ... (Le code de AddSummitScreen, ManualEntryContent et GroupSelector reste inchangé) ...

// --- CONTENU ONGLET 2 : CARTE & RECHERCHE ---
@Composable
fun MapSearchContent(
    viewModel: SummitViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    // Configuration OSM (Chargement des prefs + User Agent)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "AlpaApp/1.0"
        viewModel.fetchLocation(context)
    }

    // --- ÉTATS ---
    val existingGroups by viewModel.allGroups.collectAsState(initial = emptyList())
    var isPanelExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Valeurs par défaut (ex: un point dans les Alpes)
    var latitude by rememberSaveable { mutableStateOf("45.9237") } // Chamonix
    var longitude by rememberSaveable { mutableStateOf("6.8694") }
    var searchRadius by rememberSaveable { mutableFloatStateOf(5f) } // km

    var isSearching by remember { mutableStateOf(false) }
    var searchResults by rememberSaveable { mutableStateOf<List<ApiSummit>>(emptyList()) }
    val selectedResults = remember { mutableStateListOf<ApiSummit>() }
    var selectedGroupName by remember { mutableStateOf("") }

    LaunchedEffect(selectedResults.size) {
        if (selectedResults.size == 1) {
            isPanelExpanded = false
        }
    }

    // --- INTERFACE ---
    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (selectedResults.isNotEmpty()) 88.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. LA CARTE INTERACTIVE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp), // On agrandit un peu pour le confort
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(13.0)

                                val startPoint = GeoPoint(
                                    viewModel.userLocation?.latitude ?: latitude.toDouble(),
                                    viewModel.userLocation?.longitude ?: longitude.toDouble()
                                )

                                controller.setCenter(startPoint)
                                latitude = startPoint.latitude.toString()
                                longitude = startPoint.longitude.toString()


                                // On empêche la LazyColumn de voler le focus quand on touche la carte
                                setOnTouchListener { v, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            // L'utilisateur pose le doigt : on interdit au parent (LazyColumn) de scroller
                                            v.parent.requestDisallowInterceptTouchEvent(true)
                                        }

                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            // L'utilisateur lève le doigt : on rend le contrôle au parent
                                            v.parent.requestDisallowInterceptTouchEvent(false)
                                        }
                                    }
                                    // On retourne false pour laisser la MapView traiter le mouvement (déplacement carte)
                                    false
                                }
                                // Gestionnaire d'événements (Clic sur la carte)
                                val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        // Mise à jour des TextFields quand on clique sur la carte
                                        latitude = p.latitude.toString()
                                        longitude = p.longitude.toString()
                                        return true
                                    }

                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                })
                                overlays.add(eventsOverlay)
                            }
                        },
                        update = { mapView ->
                            // Cette partie est rappelée quand latitude, longitude ou radius changent
                            val lat = latitude.toDoubleOrNull()
                            val lon = longitude.toDoubleOrNull()

                            if (lat != null && lon != null) {
                                val centerPoint = GeoPoint(lat, lon)

                                // 1. Centrer la carte (seulement si le déplacement est significatif pour éviter de bloquer le scroll)
                                // Ici on ne centre pas forcémenent à chaque frame pour laisser l'utilisateur explorer,
                                // mais on peut le faire si on veut suivre le point.
                                // mapView.controller.animateTo(centerPoint)

                                // 2. Nettoyer les overlays dynamiques (on garde l'overlay d'événements)
                                if (mapView.overlays.size > 1) {
                                    mapView.overlays.subList(1, mapView.overlays.size).clear()
                                }

                                // 3. Marker du centre
                                val centerMarker = Marker(mapView).apply {
                                    position = centerPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Centre de recherche"
                                }
                                mapView.overlays.add(centerMarker)

                                // 4. Cercle de rayon
                                val circle = Polygon().apply {
                                    points = Polygon.pointsAsCircle(centerPoint, searchRadius * 1000.0)
                                    fillColor = 0x12121212
                                    strokeColor = 0xFF0000FF.toInt()
                                    strokeWidth = 2f
                                }
                                mapView.overlays.add(circle)
                                val selectedSet = selectedResults.toSet()
                                // 5. MARKERS DES SOMMETS
                                searchResults.forEach { summit ->

                                    val latS = summit.lat
                                    val lonS = summit.lon

                                    if (latS != null && lonS != null) {
                                        val isSelected = selectedResults.contains(summit)
                                        val summitMarker = Marker(mapView).apply {
                                            alpha = if (isSelected) 1.0f else 0.4f
                                            position = GeoPoint(latS, lonS)
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = summit.name
                                            snippet = "Alt: ${summit.altitude ?: "?"} m"
                                            setOnMarkerClickListener { _, _ ->
                                                if (selectedResults.contains(summit)) {
                                                    selectedResults.remove(summit)
                                                } else {
                                                    selectedResults.add(summit)
                                                }
                                                true
                                            }
                                        }

                                        mapView.overlays.add(summitMarker)
                                    }
                                }


                                mapView.invalidate()

                            }
                        }
                    )
                }
                Text(
                    text = "Touchez la carte pour définir le centre de recherche",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            // 2. CHAMPS LATITUDE / LONGITUDE (Synchronisés)
            /*item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { input ->
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
            }*/

            // 3. CONTRÔLES DE RECHERCHE
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Rayon de recherche : ${searchRadius.toInt()} km",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Slider(
                        value = searchRadius,
                        onValueChange = { searchRadius = it },
                        valueRange = 1f..50f,
                        steps = 49
                    )

                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull()
                            val lon = longitude.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                isLoading = true
                                searchResults = emptyList()
                                testOverpassCall(lat, lon, searchRadius.toInt()) { result ->

                                    val sorted = result
                                        .filter { it.lat != null && it.lon != null }
                                        .sortedBy { summit ->
                                            distanceInKm(
                                                lat,
                                                lon,
                                                summit.lat!!,
                                                summit.lon!!
                                            )
                                        }

                                    searchResults = sorted
                                    isLoading = false
                                }
                            } else {
                                Toast.makeText(context, "Coordonnées invalides", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recherche en cours...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rechercher les sommets alentours")
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            /*// 4. ZONE DE VALIDATION
            if (selectedResults.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Ajouter ${selectedResults.size} sommet(s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            GroupSelector(
                                existingGroups = existingGroups,
                                onGroupSelected = { selectedGroupName = it }
                            )

                            Button(
                                onClick = {
                                    selectedResults.forEach { apiRes ->
                                        val newSummit = SummitEntity(
                                            name = apiRes.name,
                                            altitude = apiRes.altitude,
                                            groupName = selectedGroupName.ifBlank { null },
                                            latitude = apiRes.lat,
                                            longitude = apiRes.lon,
                                            location = "Import Carte",
                                            transportModes = emptyList()
                                        )
                                        viewModel.addSummit(newSummit)
                                    }
                                    Toast.makeText(context, "${selectedResults.size} sommets ajoutés !", Toast.LENGTH_SHORT).show()
                                    onFinished()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirmer l'ajout")
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

             */
            // 5. LISTE DES RÉSULTATS
            if (searchResults.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Lancez une recherche pour voir les résultats.", color = Color.Gray)
                    }
                }
            } else {
                // Gestion erreur API
                if (searchResults.size == 1 && searchResults[0].name.startsWith("Erreur API")) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(searchResults[0].name, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    item {
                        Text(
                            "Résultats (${searchResults.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(searchResults) { result ->
                        val isSelected = selectedResults.contains(result)


                        val centerLat = latitude.toDoubleOrNull()
                        val centerLon = longitude.toDoubleOrNull()
                        val distance = if (
                            centerLat != null &&
                            centerLon != null &&
                            result.lat != null &&
                            result.lon != null
                        ) {
                            distanceInKm(centerLat, centerLon, result.lat, result.lon)
                        } else null

                        ListItem(
                            headlineContent = { Text(result.name) },
                            supportingContent = {
                                Text(
                                    buildString {
                                        append("Alt: ${result.altitude ?: "?"} m")
                                        if (distance != null) {
                                            append(" • ${"%.1f".format(distance)} km")
                                        }
                                    }
                                )
                            },
                            leadingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedResults.add(result)
                                        else selectedResults.remove(result)
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                )
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) selectedResults.remove(result)
                                    else selectedResults.add(result)
                                }
                        )
                    }
                }
            }
        }

        //zone flottante

        if (selectedResults.isNotEmpty()) {

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {

                Column {

                    // BARRE MINI
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPanelExpanded = !isPanelExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "Ajouter ${selectedResults.size} sommet(s)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = if (isPanelExpanded)
                                Icons.Default.KeyboardArrowDown
                            else
                                Icons.Default.KeyboardArrowUp,
                            contentDescription = null
                        )
                    }

                    // CONTENU ÉTENDU
                    AnimatedVisibility(visible = isPanelExpanded) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            GroupSelector(
                                existingGroups = existingGroups,
                                onGroupSelected = { selectedGroupName = it }
                            )

                            Button(
                                onClick = {
                                    selectedResults.forEach { apiRes ->
                                        viewModel.addSummit(
                                            SummitEntity(
                                                name = apiRes.name,
                                                altitude = apiRes.altitude,
                                                groupName = selectedGroupName.ifBlank { null },
                                                latitude = apiRes.lat,
                                                longitude = apiRes.lon,
                                                location = "Import Carte",
                                                transportModes = emptyList()
                                            )
                                        )
                                    }
                                    Toast.makeText(
                                        context,
                                        "${selectedResults.size} sommets ajoutés !",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onFinished()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Confirmer l'ajout")
                            }
                        }
                    }
                }
            }
        }

    }

}