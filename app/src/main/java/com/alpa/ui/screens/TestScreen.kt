package com.alpa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alpa.utils.SummitEntity
import com.alpa.utils.SummitViewModel

// Enum pour le filtre
enum class SummitFilter(val label: String) {
    ALL("Tous"),
    TO_DO("À faire"),
    DONE("Validés")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TestScreen(
    viewModel: SummitViewModel,
    onAddSummitClick: () -> Unit // Callback pour naviguer vers l'ajout
) {
    val summits by viewModel.allSummits.collectAsState(initial = emptyList())

    // États de l'UI
    var currentFilter by remember { mutableStateOf(SummitFilter.ALL) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    // États pour les groupes (quels groupes sont ouverts)
    // Par défaut, on peut décider de tout ouvrir ou tout fermer. Ici, vide = tout fermé.
    // Astuce : pour tout ouvrir par défaut, on pourrait initialiser avec tous les noms de groupes.
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }

    // État pour la modale de changement de groupe
    var showChangeGroupDialog by remember { mutableStateOf(false) }
    var newGroupNameInput by remember { mutableStateOf("") }

    // --- LOGIQUE DE TRI ET REGROUPEMENT ---
    val filteredSummits = remember(summits, currentFilter) {
        when (currentFilter) {
            SummitFilter.ALL -> summits
            SummitFilter.TO_DO -> summits.filter { !it.isValidated }
            SummitFilter.DONE -> summits.filter { it.isValidated }
        }
    }

    // On groupe par nom. Si null, on met "Sans groupe"
    val groupedSummits = remember(filteredSummits) {
        filteredSummits.groupBy { it.groupName ?: "Non classé" }.toSortedMap()
    }

    // Gestion de la sélection
    fun toggleSelection(id: Int) {
        selectedIds = if (selectedIds.contains(id)) {
            selectedIds - id
        } else {
            selectedIds + id
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onClear = { selectedIds = emptySet() },
                    onDelete = {
                        // TODO: Appel ViewModel pour supprimer
                        // viewModel.deleteSelected(selectedIds)
                        selectedIds = emptySet()
                    },
                    onChangeGroup = {
                        newGroupNameInput = "" // Reset
                        showChangeGroupDialog = true
                    }
                )
            } else {
                StandardTopBar(
                    onAddClick = onAddSummitClick,
                    currentFilter = currentFilter,
                    onFilterChange = { currentFilter = it }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onAddSummitClick) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un sommet")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp) // Espace pour le FAB
        ) {
            groupedSummits.forEach { (groupName, summitsInGroup) ->
                val isExpanded = expandedGroups.contains(groupName)

                // En-tête de Groupe
                item(key = "header_$groupName") {
                    GroupHeader2(
                        groupName = groupName,
                        count = summitsInGroup.size,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedGroups = if (isExpanded) expandedGroups - groupName
                            else expandedGroups + groupName
                        }
                    )
                }

                // Liste des sommets (affichée seulement si étendu)
                if (isExpanded) {
                    items(summitsInGroup, key = { it.id }) { summit ->
                        SelectableSummitItem(
                            summit = summit,
                            isSelected = selectedIds.contains(summit.id),
                            isSelectionMode = isSelectionMode,
                            onToggleSelection = { toggleSelection(summit.id) },
                            onClick = {
                                if (isSelectionMode) {
                                    toggleSelection(summit.id)
                                } else {
                                    // TODO: Ouvrir le détail ou l'édition du sommet
                                }
                            }
                        )
                    }
                }
            }

            if (groupedSummits.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun sommet trouvé", color = Color.Gray)
                    }
                }
            }
        }
    }

    // --- DIALOGUE CHANGEMENT DE GROUPE ---
    if (showChangeGroupDialog) {
        AlertDialog(
            onDismissRequest = { showChangeGroupDialog = false },
            title = { Text("Changer le groupe") },
            text = {
                Column {
                    Text("Entrez le nouveau nom du groupe pour les ${selectedIds.size} sommets sélectionnés.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupNameInput,
                        onValueChange = { newGroupNameInput = it },
                        label = { Text("Nom du groupe") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Appel ViewModel
                        // viewModel.updateGroupForSelected(selectedIds, newGroupNameInput)
                        showChangeGroupDialog = false
                        selectedIds = emptySet() // Quitter le mode sélection
                    }
                ) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeGroupDialog = false }) { Text("Annuler") }
            }
        )
    }
}

// --- COMPOSANTS UI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopBar(
    onAddClick: () -> Unit,
    currentFilter: SummitFilter,
    onFilterChange: (SummitFilter) -> Unit
) {
    Column {
        TopAppBar(
            title = { Text("Mes Sommets") },
            actions = {
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter")
                }
            }
        )
        // Barre de filtres (Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummitFilter.values().forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                    leadingIcon = if (currentFilter == filter) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onChangeGroup: () -> Unit
) {
    TopAppBar(
        title = { Text("$count sélectionné(s)") },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Fermer")
            }
        },
        actions = {
            IconButton(onClick = onChangeGroup) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Changer Groupe")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun GroupHeader2(
    groupName: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Badge { Text("$count") }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Réduire" else "Déplier"
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableSummitItem(
    summit: SummitEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggleSelection
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenu Gauche (Info)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${summit.altitude ?: "?"} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contenu Droite (Checkbox ou Statut)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
            } else {
                // Indicateur normal (Fait / Pas fait)
                if (summit.isValidated) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Validé",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "À faire",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}