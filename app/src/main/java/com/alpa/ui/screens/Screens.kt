package com.alpa.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector


sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object SummitList : Screen("summitList", "Liste", Icons.Default.List)
    object Profile : Screen("profile", "Profil", Icons.Default.Person)
}

//@Composable
//fun HomeScreen() {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text(text = "🏠 Écran d'accueil")
//    }
//}

@Composable
fun SearchScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "🔍 Écran de recherche")
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "👤 Écran de profil")
    }
}