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
import com.alpa.utils.SummitEntity
import com.alpa.utils.SummitViewModel
import java.time.LocalDate

// --- ENUMS ---
enum class FilterType { ALL, VALIDATED, TODO }

// --- ÉCRAN PRINCIPAL ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SummitsListScreen(
    viewModel: SummitViewModel,
    onNavigateToAddSummit: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    // -- 1. Données : Observation de la BDD via le ViewModel --
    val allSummits by viewModel.allSummits.collectAsState(initial = emptyList())

    // -- 2. État de l'interface --
    var currentFilter by remember { mutableStateOf(FilterType.ALL) }

    // Liste des IDs sélectionnés (Mode Edit). On utilise Int pour l'ID (comme défini dans SummitEntity)
    val selectedIds = remember { mutableStateListOf<Int>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    // Liste des groupes repliés
    val collapsedGroups = remember { mutableStateListOf<String?>() }

    // État pour la modale de déplacement de groupe
    var showMoveGroupDialog by remember { mutableStateOf(false) }

    // -- 3. Logique de filtrage et de regroupement --
    val filteredSummits = remember(allSummits, currentFilter) {
        when (currentFilter) {
            FilterType.ALL -> allSummits
            FilterType.VALIDATED -> allSummits.filter { it.isValidated }
            FilterType.TODO -> allSummits.filter { !it.isValidated }
        }
    }

    // Transformation en Map : Groupe -> Liste de sommets
    val groupedSummits = remember(filteredSummits) {
        filteredSummits.groupBy { it.groupName ?: "Sans groupe" }.toSortedMap()
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // --- TopBar MODE SÉLECTION ---
                TopAppBar(
                    title = { Text("${selectedIds.size} sélectionné(s)") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    navigationIcon = {
                        IconButton(onClick = { selectedIds.clear() }) {
                            Icon(Icons.Default.Close, "Annuler")
                        }
                    },
                    actions = {
                        // Action: Dégrouper (Mettre le groupe à null)
                        IconButton(onClick = {
                            //viewModel.updateGroupForList(selectedIds.toList(), null)
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
                            //viewModel.deleteSummits(selectedIds.toList())
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Delete, "Supprimer")
                        }
                    }
                )
            } else {
                // --- TopBar MODE NORMAL ---
                Column {
                    TopAppBar(
                        title = { Text("Mes Sommets") },
                        actions = {
                            IconButton(onClick = { onNavigateToAddSummit() }) {
                                Icon(Icons.Default.Add, "Ajouter")
                            }
                        }
                    )
                    // Barre de filtre
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            groupedSummits.forEach { (groupName, itemsInGroup) ->
                val isCollapsed = collapsedGroups.contains(groupName)

                // En-tête de Groupe (Sticky)
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
                                //viewModel.toggleValidation(summit)
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedIds.remove(summit.id)
                                    else selectedIds.add(summit.id)
                                } else {
                                    // Action click normal
                                    onNavigateToDetail(summit.id)
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

            if (filteredSummits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun sommet trouvé", color = Color.Gray)
                    }
                }
            }
        }

        // Dialog pour déplacer vers un groupe
        if (showMoveGroupDialog) {
            MoveToGroupDialog(
                existingGroups = allSummits.mapNotNull { it.groupName }.distinct(),
                onDismiss = { showMoveGroupDialog = false },
                onGroupSelected = { newGroup ->
                    //viewModel.updateGroupForList(selectedIds.toList(), newGroup)
                    selectedIds.clear()
                    showMoveGroupDialog = false
                }
            )
        }
    }
}

// --- COMPOSANTS UI DÉTAILLÉS ---

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
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                Text(count.toString(), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummitItem(
    summit: SummitEntity,
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
                else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Column {
                val altitudeText = if (summit.altitude != null && summit.altitude > 0) "${summit.altitude} m" else "Alt. inconnue"
                Text(altitudeText, style = MaterialTheme.typography.bodyMedium)

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
                // En mode sélection : Checkbox (click géré par le parent ListItem)
                Checkbox(checked = isSelected, onCheckedChange = null)
            } else {
                // En mode normal : Icône
                Icon(
                    Icons.Default.Terrain,
                    contentDescription = null,
                    tint = if(summit.isValidated) MaterialTheme.colorScheme.primary.copy(alpha=0.6f) else Color.Gray
                )
            }
        },
        trailingContent = {
            // Bouton de validation rapide (masqué en mode sélection pour éviter les conflits)
            if (!isSelectionMode) {
                IconButton(onClick = onToggleValidation) {
                    Icon(
                        imageVector = if (summit.isValidated) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (summit.isValidated) "Invalider" else "Valider",
                        tint = if (summit.isValidated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    )
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                Text(
                    "Choisir un groupe existant :",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Liste des groupes existants (scrollable si besoin)
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(existingGroups) { group ->
                        TextButton(
                            onClick = { onGroupSelected(group) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                group,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Créer un nouveau groupe
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Ou créer nouveau") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newGroupName.isNotBlank()) onGroupSelected(newGroupName) },
                enabled = newGroupName.isNotBlank()
            ) {
                Text("Créer et déplacer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}