package com.alpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpa.utils.SummitEntity
import com.alpa.utils.SummitViewModel
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: SummitViewModel,
    onSummitClick: (Int) -> Unit, // Callback pour naviguer vers les détails
    onInfoClick: () -> Unit
) {
    // 1. Observation de la Base de Données
    val allSummits by viewModel.allSummits.collectAsState(initial = emptyList())

    // 2. Calculs des données (Recalculés uniquement si 'allSummits' change)
    val validatedSummits = remember(allSummits) { allSummits.filter { it.isValidated } }
    val todoSummits = remember(allSummits) { allSummits.filter { !it.isValidated } }

    // --- Statistiques ---
    // Note: altitude est Int? dans l'Entity, on utilise ?: 0 pour éviter les crashs
    val totalAltitude = remember(validatedSummits) { validatedSummits.sumOf { it.altitude ?: 0 } }
    val summitsCount = validatedSummits.size

    // Ratio Everest (8848m)
    val everestRatio = remember(totalAltitude) {
        if (totalAltitude > 0) String.format("%.1f", totalAltitude.toDouble() / 8848.0) else "0.0"
    }

    // --- Suggestion (Aléatoire parmi "À faire") ---
    val suggestedSummit = remember(todoSummits) {
        if (todoSummits.isNotEmpty()) todoSummits.shuffled().first() else null
    }

    // --- Derniers réalisés (Tri par date de validation décroissante) ---
    val recentSummits = remember(validatedSummits) {
        validatedSummits
            .filter { it.validationDate != null }
            .sortedByDescending { it.validationDate }
            .take(5)
    }

    // 3. Structure de l'écran
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- En-tête ---
        HeaderSection(onInfoClick)

        // --- Statistiques ---
        StatsSection(totalAltitude, summitsCount, everestRatio)

        // --- Suggestion (Call to Action) ---
        // S'affiche uniquement s'il y a des sommets à faire
        if (suggestedSummit != null) {
            SuggestionCard(summit = suggestedSummit, onClick = { onSummitClick(suggestedSummit.id) })
        } else if (allSummits.isEmpty()) {
            // Message si la base est vide
            EmptyStateCard()
        }

        // --- Dernières ascensions ---
        if (recentSummits.isNotEmpty()) {
            RecentActivitySection(summits = recentSummits, onSummitClick = onSummitClick)
        }

        // Bouton de debug (Optionnel)
        /*Button(onClick = { viewModel.logSummits() }) {
            Text(text = "Log Debug BDD")
        }*/
    }
}

// --- SOUS-COMPOSANTS ---

@Composable
fun HeaderSection(onInfoClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bienvenue sur",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Text(
                text = "ALPA",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column{
            IconButton(onClick =  onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Informations",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatsSection(altitude: Int, count: Int, everestRatio: String) {
    Text("Vos exploits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Carte 1 : Altitude totale
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Terrain,
            value = "${altitude}m",
            label = "Dénivelé cumulé",
            color = MaterialTheme.colorScheme.primary
        )
        // Carte 2 : Sommets
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Flag,
            value = "$count",
            label = "Sommets vaincus",
            color = MaterialTheme.colorScheme.secondary
        )
    }

    // Carte Large "Fun Fact"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Niveau Sherpa", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Vous avez grimpé l'équivalent de $everestRatio x l'Everest !",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: ImageVector, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SuggestionCard(summit: SummitEntity, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Prochain défi ?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick // La carte entière est cliquable
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF64B5F6), Color(0xFF1976D2))
                        )
                    )
            ) {
                // Contenu de la carte
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = summit.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${summit.groupName ?: "Inconnu"} • ${summit.altitude ?: "?"}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Button(
                    onClick = onClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Voir")
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Aucun sommet enregistré", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Commencez par ajouter votre premier sommet via l'onglet Liste !")
        }
    }
}

@Composable
fun RecentActivitySection(summits: List<SummitEntity>, onSummitClick: (Int) -> Unit) {
    Text("Dernières réussites", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(summits, key = { it.id }) { summit ->
            RecentSummitItem(summit = summit, onClick = { onSummitClick(summit.id) })
        }
    }
}

@Composable
fun RecentSummitItem(summit: SummitEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = summit.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = summit.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            // Affichage de la date (si présente)
            summit.validationDate?.let { date ->
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}