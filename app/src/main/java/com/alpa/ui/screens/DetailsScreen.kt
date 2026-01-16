package com.alpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter


// Enum pour le mode de transport
enum class TransportMode(val label: String, val icon: ImageVector) {
    HIKING("Randonnée", Icons.AutoMirrored.Filled.DirectionsWalk),
    SKI_TOURING("Ski de Rando", Icons.Default.DownhillSkiing), // Icône proche
    MTB("VTT", Icons.Default.DirectionsBike),
    CLIMBING("Alpinisme", Icons.Default.Terrain),
    OTHER("Autre", Icons.Default.MoreHoriz)
}

// Mise à jour de la classe Summit (ajoutez les nouveaux champs)
data class Summit(
    val id: String,
    val name: String,
    val altitude: Int,
    val groupName: String? = null,
    val isValidated: Boolean = false,
    val validationDate: LocalDate? = null,
    // Nouveaux champs :
    val transportMode: TransportMode = TransportMode.HIKING,
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummitDetailScreen(
    onBack: () -> Unit
) {
    // --- 1. SIMULATION DU SOMMET (Données locales à l'écran) ---
    // Dans la vraie app, on récupérerait le sommet via son ID passé en argument
    var summit by remember {
        mutableStateOf(
            Summit(
                id = "99",
                name = "Grand Paradis",
                altitude = 4061,
                groupName = "Alpes Grées",
                isValidated = true,
                validationDate = LocalDate.now().minusDays(10),
                transportMode = TransportMode.SKI_TOURING,
                notes = "Une montée incroyable mais le glacier était très crevassé sur la fin. Vue imprenable sur le Mont Blanc."
            )
        )
    }

    // État pour savoir si on est en mode édition
    var isEditing by remember { mutableStateOf(false) }

    // États temporaires pour le formulaire d'édition
    var editName by remember { mutableStateOf("") }
    var editAltitude by remember { mutableStateOf("") }
    var editGroup by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(TransportMode.HIKING) }

    // Fonction pour initialiser le formulaire quand on clique sur "Modifier"
    fun startEditing() {
        editName = summit.name
        editAltitude = summit.altitude.toString()
        editGroup = summit.groupName ?: ""
        editNotes = summit.notes
        editMode = summit.transportMode
        isEditing = true
    }

    // Fonction pour sauvegarder
    fun saveChanges() {
        summit = summit.copy(
            name = editName,
            altitude = editAltitude.toIntOrNull() ?: summit.altitude,
            groupName = editGroup.ifBlank { null },
            notes = editNotes,
            transportMode = editMode
        )
        isEditing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifier le sommet" else "Détails") },
                navigationIcon = {
                    IconButton(onClick = { if (isEditing) isEditing = false else onBack() }) {
                        Icon(if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = { saveChanges() }) {
                            Text("Enregistrer", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = { startEditing() }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                // --- MODE ÉDITION (Formulaire) ---
                EditForm(
                    name = editName, onNameChange = { editName = it },
                    altitude = editAltitude, onAltitudeChange = { editAltitude = it },
                    group = editGroup, onGroupChange = { editGroup = it },
                    notes = editNotes, onNotesChange = { editNotes = it },
                    selectedMode = editMode, onModeChange = { editMode = it }
                )
            } else {
                // --- MODE VUE (Affichage propre) ---
                ViewContent(summit = summit)
            }
        }
    }
}

// --- SOUS-COMPOSANTS POUR L'AFFICHAGE ---

@Composable
fun ViewContent(summit: Summit) {
    // En-tête avec Icône géante et Nom
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (summit.isValidated) Icons.Default.EmojiEvents else Icons.Default.Terrain,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = summit.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = "${summit.altitude} m", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Informations détaillées
    DetailRow(icon = Icons.Default.Place, label = "Massif / Groupe", value = summit.groupName ?: "Non classé")

    if (summit.isValidated) {
        DetailRow(
            icon = Icons.Default.Event,
            label = "Date de réalisation",
            value = summit.validationDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ?: "Date inconnue"
        )
        DetailRow(
            icon = summit.transportMode.icon,
            label = "Moyen de réalisation",
            value = summit.transportMode.label
        )
    } else {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ce sommet est encore dans votre liste 'À faire'.")
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Section Notes
    Text("Mes Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Text(
            text = summit.notes.ifBlank { "Aucune note pour le moment..." },
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// --- SOUS-COMPOSANTS POUR L'ÉDITION ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditForm(
    name: String, onNameChange: (String) -> Unit,
    altitude: String, onAltitudeChange: (String) -> Unit,
    group: String, onGroupChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit,
    selectedMode: TransportMode, onModeChange: (TransportMode) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Nom du sommet") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = altitude,
        onValueChange = { if (it.all { char -> char.isDigit() }) onAltitudeChange(it) },
        label = { Text("Altitude (m)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = group,
        onValueChange = onGroupChange,
        label = { Text("Groupe / Massif") },
        modifier = Modifier.fillMaxWidth()
    )

    Text("Moyen de réalisation", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))

    // Chips pour sélectionner le mode de transport
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TransportMode.values().forEach { mode ->
            FilterChip(
                selected = mode == selectedMode,
                onClick = { onModeChange(mode) },
                label = { Text(mode.label) },
                leadingIcon = {
                    if (mode == selectedMode) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(mode.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
    }

    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notes personnelles") },
        modifier = Modifier.fillMaxWidth().height(150.dp),
        maxLines = 10
    )
}