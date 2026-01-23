package com.alpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpa.utils.SummitEntity
import com.alpa.utils.SummitViewModel
import com.alpa.utils.TransportMode
import java.time.format.DateTimeFormatter



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummitDetailScreen(
    summitId: Int,
    viewModel: SummitViewModel,
    onBack: () -> Unit
) {
    // Récupération "Live" du sommet
    val summitState by viewModel.getSummitById(summitId).collectAsState(initial = null)
    val summit = summitState

    // État : Mode édition actif ou non
    var isEditing by remember { mutableStateOf(false) }

    // --- ÉTATS DU FORMULAIRE TEMPORAIRE ---
    var editName by remember { mutableStateOf("") }
    var editAltitude by remember { mutableStateOf("") }
    var editGroup by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }

    // IMPORTANT : C'est maintenant une LISTE
    var editModes by remember { mutableStateOf(emptyList<TransportMode>()) }

    // Initialisation des champs quand on clique sur "Modifier"
    fun startEditing(s: SummitEntity) {
        editName = s.name
        editAltitude = s.altitude?.toString() ?: ""
        editGroup = s.groupName ?: ""
        editNotes = s.notes
        editModes = s.transportModes // On récupère la liste existante
        isEditing = true
    }

    // Sauvegarde
    fun saveChanges(original: SummitEntity) {
        val updatedSummit = original.copy(
            name = editName,
            altitude = editAltitude.toIntOrNull(),
            groupName = editGroup.ifBlank { null },
            notes = editNotes,
            transportModes = editModes // On sauvegarde la liste
        )
        viewModel.updateSummit(updatedSummit)
        isEditing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifier" else "Détails") },
                navigationIcon = {
                    IconButton(onClick = { if (isEditing) isEditing = false else onBack() }) {
                        Icon(if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (summit != null) {
                        if (isEditing) {
                            TextButton(onClick = { saveChanges(summit) }) {
                                Text("Enregistrer", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            IconButton(onClick = { startEditing(summit) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Modifier")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (summit == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    // --- MODE ÉDITION ---
                    EditForm(
                        name = editName, onNameChange = { editName = it },
                        altitude = editAltitude, onAltitudeChange = { editAltitude = it },
                        group = editGroup, onGroupChange = { editGroup = it },
                        notes = editNotes, onNotesChange = { editNotes = it }
                    )
                } else {
                    // --- MODE VUE ---
                    ViewContent(viewModel = viewModel, summit = summit)
                }
            }
        }
    }
}

// --- COMPOSANTS D'AFFICHAGE ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewContent(viewModel: SummitViewModel, summit: SummitEntity) {


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
        Text(text = "${summit.altitude ?: "?"} m", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    DetailRow(icon = Icons.Default.Place, label = "Massif / Groupe", value = summit.groupName ?: "Non classé")


        DetailRow(
            icon = Icons.Default.Event,
            label = "Date de réalisation",
            value = summit.validationDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ?: "Date inconnue"
        )

        // Affichage spécial pour la liste des modes de transport
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Moyen(s) de réalisation", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Affichage de TOUS les modes possibles
        FlowRow(
            modifier = Modifier.padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Supposons que TransportMode.values() donne tous les types (Ski, Pied, Vélo, etc.)
            TransportMode.values().forEach { mode ->
                val isSelected = summit.transportModes.contains(mode)

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.toggleTransportMode(summit, mode)
                    },
                    label = { Text(mode.label) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        { Icon(mode.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                )
            }
        }
    }

    // Le reste de ton code (Date de réalisation / Card "À faire")
    if (summit.isValidated) {
        DetailRow(
            icon = Icons.Default.Event,
            label = "Date de réalisation",
            value = summit.validationDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ?: "Date inconnue"
        )
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ce sommet est encore à faire.")
            }
        }
    }


    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Text("Mes Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// --- FORMULAIRE D'ÉDITION ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditForm(
    name: String, onNameChange: (String) -> Unit,
    altitude: String, onAltitudeChange: (String) -> Unit,
    group: String, onGroupChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        Text("Moyens de réalisation (Multiple)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes personnelles") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 10,
            singleLine = false
        )
    }
}