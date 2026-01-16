package com.alpa.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alpa.ui.screens.*

// 1. Définition des écrans
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object SummitList : Screen("summitList", "Liste", Icons.Default.List)
    object Profile : Screen("profile", "Profil", Icons.Default.Person)
}

// 2. Le NavHost qui fait le lien entre routes et écrans
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.SummitList.route) { SummitsListScreen() }
        composable(Screen.Profile.route) { SummitDetailScreen(onBack = {
            // On dit au navController de revenir à l'écran précédent
            navController.popBackStack()
        }) }
    }
}