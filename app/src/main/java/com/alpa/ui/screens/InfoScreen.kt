package com.alpa.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpa.utils.SummitEntity
import com.alpa.utils.SummitViewModel
import com.alpa.utils.serializeSummitsToJson
import java.lang.System.console
import java.time.LocalDate

@Composable
fun InfoScreen(
    viewModel: SummitViewModel
) {
    val allSummits by viewModel.allSummits.collectAsState(initial = emptyList())
    val context = LocalContext.current

// Ce lanceur ouvre le sélecteur de dossier d'Android
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        // Une fois que l'utilisateur a choisi l'emplacement, on écrit dedans
        uri?.let {
            val jsonString = serializeSummitsToJson(allSummits) // Ta fonction de sérialisation
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                // Optionnel : Afficher un Toast de succès
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val jsonContent = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                reader.readText()
            }
            if (jsonContent != null) {
                //viewModel.importData(jsonContent)
                println(jsonContent)
                viewModel.importFromJSON(jsonContent)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "À propos d'ALPA",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ALPA est votre assistant personnel pour la gestion de données de sommets. " +
                            "Cette application permet de centraliser vos expéditions, de suivre vos progrès " +
                            "et de sauvegarder vos performances de manière sécurisée.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Divider()

        // Section Import/Export
        Text(
            text = "Gestion des données",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bouton Exporter
            Button(
                onClick = {
                    val fileName = "alpa_export_${LocalDate.now()}.json"
                    createDocumentLauncher.launch(fileName)},
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exporter")
            }

            // Bouton Importer
            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importer")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Version en bas de page
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}