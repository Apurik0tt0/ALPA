package com.alpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

// On réutilise les modèles définis précédemment (Summit)
// Assurez-vous d'avoir accès à 'initialSummits' ici.

@Composable
fun HomeScreen() {
    // 1. Préparation des données (Logique métier simple)
    val summits = remember { initialSummits } // On reprend la liste fictive

    val validatedSummits = summits.filter { it.isValidated }
    val todoSummits = summits.filter { !it.isValidated }

    // Calcul des Stats
    val totalAltitude = validatedSummits.sumOf { it.altitude }
    val summitsCount = validatedSummits.size
    // Statistique "Fun" : Combien d'Everest (8848m) on a grimpé au total
    val everestRatio = (totalAltitude.toDouble() / 8848.0)
    val formattedEverest = String.format("%.1f", everestRatio)

    // Suggestion : Prendre un sommet "À faire" au hasard ou le premier
    val suggestedSummit = remember(todoSummits) { todoSummits.shuffled().firstOrNull() }

    // Derniers réalisés : Triés par date (du plus récent au plus vieux)
    val recentSummits = validatedSummits
        .sortedByDescending { it.validationDate }
        .take(5)

    // 2. Structure de l'écran
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Tout l'écran scroll verticalement
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- En-tête ---
        HeaderSection()

        // --- Statistiques ---
        StatsSection(totalAltitude, summitsCount, formattedEverest)

        // --- Suggestion (Call to Action) ---
        if (suggestedSummit != null) {
            SuggestionCard(suggestedSummit)
        }

        // --- Dernières ascensions ---
        if (recentSummits.isNotEmpty()) {
            RecentActivitySection(recentSummits)
        }
    }
}

// --- SOUS-COMPOSANTS ---

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Bonjour,",
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
        // Avatar fictif ou icône utilisateur
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profil")
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
fun SuggestionCard(summit: Summit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Prochain défi ?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
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
                            text = "${summit.groupName ?: "Inconnu"} • ${summit.altitude}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Button(
                    onClick = { /* Naviguer vers détail */ },
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
fun RecentActivitySection(summits: List<Summit>) {
    Text("Dernières réussites", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(summits) { summit ->
            RecentSummitItem(summit)
        }
    }
}

@Composable
fun RecentSummitItem(summit: Summit) {
    Card(
        modifier = Modifier.width(140.dp),
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
                    text = summit.name.take(1),
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
            Text(
                text = summit.validationDate?.format(DateTimeFormatter.ofPattern("dd MMM")) ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}