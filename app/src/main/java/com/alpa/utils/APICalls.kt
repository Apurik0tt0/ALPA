package com.alpa.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


//stockage local des donnés récupérées par appel à l'API
data class ApiSummit(
    val name: String,
    val altitude: Int?,       // certains sommets n'ont pas d'altitude dans OSM
    val lat: Double?,
    val lon: Double?
)

fun testOverpassCall(latitude: Double, longitude: Double, radius: Int, onResult: (List<ApiSummit>) -> Unit) {

    //appelle l'API overpass
    // recherche les pics présents dans un rayon donné
    // 10 000 : rayon du cercle + coordonnées : centre du cercle
    //sauvegarde les résultats nom altitude coord et les affiche sur la page
    val metersRadius: Int = radius * 1000
    val query = """
    [out:json][timeout:25];
    (
      node
        ["natural"="peak"] 
        (around:$metersRadius,$latitude,$longitude); 
    );
    out body;
    """.trimIndent()

    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).get().build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (body != null && body.trimStart().startsWith("{")) {
                // C’est probablement du JSON
                val summits = mutableListOf<ApiSummit>()
                val json = JSONObject(body)
                val elements = json.getJSONArray("elements")
                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)
                    val lat = element.optDouble("lat")
                    val lon = element.optDouble("lon")
                    val tags = element.optJSONObject("tags")
                    val name = tags?.optString("name") ?: continue
                    val ele = tags?.optString("ele")?.toIntOrNull()
                    summits.add(ApiSummit(name = name, altitude = ele, lat = lat, lon = lon))
                }
                onResult(summits)
            } else {
                // Réponse inattendue (XML ou HTML)
                onResult(listOf(ApiSummit("Erreur API : réponse invalide", null, 0.0, 0.0)))
            }
        } catch (e: Exception) {
            onResult(listOf(ApiSummit("Erreur API : ${e.message}", null, 0.0, 0.0)))
        }
    }
}