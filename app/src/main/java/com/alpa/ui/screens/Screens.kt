package com.alpa.ui.screens


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector


sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object SummitList : Screen("summitList", "Liste", Icons.Default.List)

    object Map : Screen("map", "Carte", Icons.Default.Map)
}
