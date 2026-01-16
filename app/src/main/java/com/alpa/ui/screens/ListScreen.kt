package com.alpa.ui.screens

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

// --- 1. MODÈLES DE DONNÉES ---

data class Summit(
    val id: String,
    val name: String,
    val altitude: Int,
    val groupName: String? = null, // Null si "Sans groupe"
    val isValidated: Boolean = false,
    val validationDate: LocalDate? = null
)

enum class FilterType { ALL, VALIDATED, TODO }

// --- 2. DONNÉES DE TEST (MOCK) ---

val initialSummits = listOf(
    Summit("1", "Mont Blanc", 4807, "Alpes", true, LocalDate.of(2022, 7, 15)),
    Summit("2", "Dôme du Goûter", 4304, "Alpes", true, LocalDate.of(2022, 7, 14)),
    Summit("3", "Puy de Dôme", 1465, "Massif Central", true, LocalDate.of(2021, 5, 20)),
    Summit("4", "Puy de Sancy", 1885, "Massif Central", false),
    Summit("5", "Vignemale", 3298, "Pyrénées", false),
    Summit("6", "Pic du Midi", 2877, "Pyrénées", false),
    Summit("7", "Everest", 8848, "Himalaya", false),
    Summit("8", "Kilimanjaro", 5895, null, false), // Sans groupe
    Summit("9", "Fuji", 3776, null, true, LocalDate.of(2019, 8, 1))
)

// --- 3. ÉCRAN PRINCIPAL ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SummitsListScreen() {
    // -- État des données --
    // On utilise `toMutableStateList` pour pouvoir modifier la liste facilement
    val summits = remember { initialSummits.toMutableStateList() }

    // -- État de l'interface --
    var currentFilter by remember { mutableStateOf(FilterType.ALL) }

    // Liste des IDs des sommets sélectionnés (Mode Edit)
    val selectedIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    // Liste des groupes repliés (cachés)
    val collapsedGroups = remember { mutableStateListOf<String?>() }

    // État pour la modale de déplacement de groupe
    var showMoveGroupDialog by remember { mutableStateOf(false) }

    // -- Logique de filtrage et de regroupement --
    val filteredSummits = remember(summits, currentFilter) {
        when (currentFilter) {
            FilterType.ALL -> summits
            FilterType.VALIDATED -> summits.filter { it.isValidated }
            FilterType.TODO -> summits.filter { !it.isValidated }
        }
    }

    // On transforme la liste plate en Map : Groupe -> Liste de sommets
    val groupedSummits = remember(filteredSummits) {
        filteredSummits.groupBy { it.groupName ?: "Sans groupe" }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // TopBar MODE SÉLECTION
                TopAppBar(
                    title = { Text("${selectedIds.size} sélectionné(s)") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    navigationIcon = {
                        IconButton(onClick = { selectedIds.clear() }) {
                            Icon(Icons.Default.Close, "Annuler")
                        }
                    },
                    actions = {
                        // Action: Dégrouper
                        IconButton(onClick = {
                            summits.replaceAll { if (it.id in selectedIds) it.copy(groupName = null) else it }
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.LinkOff, "Dégrouper")
                        }
                        // Action: Déplacer (Ouvre dialog)
                        IconButton(onClick = { showMoveGroupDialog = true }) {
                            Icon(Icons.Default.DriveFileMove, "Déplacer")
                        }
                        // Action: Supprimer
                        IconButton(onClick = {
                            summits.removeIf { it.id in selectedIds }
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Delete, "Supprimer")
                        }
                    }
                )
            } else {
                // TopBar MODE NORMAL
                Column {
                    TopAppBar(
                        title = { Text("Mes Sommets") },
                        actions = {
                            IconButton(onClick = { /* Ajouter nouveau sommet */ }) {
                                Icon(Icons.Default.Add, "Ajouter")
                            }
                        }
                    )
                    // Barre de filtre simple
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentFilter == FilterType.ALL,
                            onClick = { currentFilter = FilterType.ALL },
                            label = { Text("Tous") }
                        )
                        FilterChip(
                            selected = currentFilter == FilterType.TODO,
                            onClick = { currentFilter = FilterType.TODO },
                            label = { Text("À faire") }
                        )
                        FilterChip(
                            selected = currentFilter == FilterType.VALIDATED,
                            onClick = { currentFilter = FilterType.VALIDATED },
                            label = { Text("Validés") }
                        )
                    }
                }
            }
        }
    ) { padding ->

        // -- LISTE SCROLLABLE --
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            groupedSummits.forEach { (groupName, itemsInGroup) ->
                val isCollapsed = collapsedGroups.contains(groupName)

                // En-tête de Groupe
                stickyHeader {
                    GroupHeader(
                        title = groupName,
                        count = itemsInGroup.size,
                        isCollapsed = isCollapsed,
                        onToggle = {
                            if (isCollapsed) collapsedGroups.remove(groupName)
                            else collapsedGroups.add(groupName)
                        }
                    )
                }

                // Items du groupe (si déplié)
                if (!isCollapsed) {
                    items(itemsInGroup, key = { it.id }) { summit ->
                        val isSelected = selectedIds.contains(summit.id)

                        SummitItem(
                            summit = summit,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onToggleValidation = {
                                // Mise à jour de l'état validé/non validé
                                val index = summits.indexOf(summit)
                                if (index != -1) {
                                    summits[index] = summit.copy(
                                        isValidated = !summit.isValidated,
                                        validationDate = if (!summit.isValidated) LocalDate.now() else null
                                    )
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedIds.remove(summit.id) else selectedIds.add(summit.id)
                                    if (selectedIds.isEmpty()) { /* Sortir auto du mode selection ? ou pas */ }
                                } else {
                                    // Action click normal (ex: voir détails)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedIds.add(summit.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Dialog pour déplacer vers un groupe
        if (showMoveGroupDialog) {
            MoveToGroupDialog(
                existingGroups = summits.mapNotNull { it.groupName }.distinct(),
                onDismiss = { showMoveGroupDialog = false },
                onGroupSelected = { newGroup ->
                    summits.replaceAll { if (it.id in selectedIds) it.copy(groupName = newGroup) else it }
                    selectedIds.clear()
                    showMoveGroupDialog = false
                }
            )
        }
    }
}

// --- 4. COMPOSANTS UI DÉTAILLÉS ---

@Composable
fun GroupHeader(
    title: String,
    count: Int,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Badge { Text(count.toString()) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummitItem(
    summit: Summit,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleValidation: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.surface

    ListItem(
        modifier = Modifier
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        headlineContent = {
            Text(
                summit.name,
                style = if (summit.isValidated)
                    MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                else MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column {
                Text("${summit.altitude} m")
                if (summit.isValidated && summit.validationDate != null) {
                    Text(
                        "Validé le ${summit.validationDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        leadingContent = {
            if (isSelectionMode) {
                // En mode sélection, on affiche une Checkbox à gauche
                Checkbox(checked = isSelected, onCheckedChange = null) // Le click est géré par le parent
            } else {
                // En mode normal, on affiche l'icône de montagne
                Icon(Icons.Default.Terrain, null, tint = Color.Gray)
            }
        },
        trailingContent = {
            // Bouton de validation rapide (seulement en mode normal)
            if (!isSelectionMode) {
                IconButton(onClick = onToggleValidation) {
                    Icon(
                        if (summit.isValidated) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Valider",
                        tint = if (summit.isValidated) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
fun MoveToGroupDialog(
    existingGroups: List<String>,
    onDismiss: () -> Unit,
    onGroupSelected: (String) -> Unit
) {
    var newGroupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Déplacer vers un groupe") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Liste des groupes existants
                Text("Groupes existants :", style = MaterialTheme.typography.labelLarge)
                existingGroups.forEach { group ->
                    TextButton(
                        onClick = { onGroupSelected(group) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 0.dp)
                    ) {
                        Text(group, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Créer un nouveau groupe
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Ou créer nouveau groupe") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newGroupName.isNotBlank()) onGroupSelected(newGroupName) },
                enabled = newGroupName.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}