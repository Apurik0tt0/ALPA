package com.alpa.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonConfig = Json {
    prettyPrint = true          // Rend le JSON lisible pour l'humain
    ignoreUnknownKeys = true    // Évite de planter si on ajoute des champs plus tard
    encodeDefaults = true       // Inclut les valeurs par défaut dans le JSON
}

/**
 * Transforme une liste de sommets en chaîne de caractères JSON
 */
fun serializeSummitsToJson(summits: List<SummitEntity>): String {
    return jsonConfig.encodeToString(summits)
}

/**
 * Transforme une chaîne JSON en liste d'objets SummitEntity
 * Retourne une liste vide en cas d'erreur de format
 */
fun deserializeSummitsFromJson(jsonString: String): List<SummitEntity> {
    return try {
        jsonConfig.decodeFromString<List<SummitEntity>>(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}