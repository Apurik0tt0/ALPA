package com.alpa.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alpa.ui.screens.*
import com.alpa.utils.SummitViewModel

// 1. Définition des écrans
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object SummitList : Screen("summitList", "Liste", Icons.Default.List)
    object Map : Screen("map", "Carte", Icons.Default.Map)

    object  AddSummit : Screen("addSummit", "Ajouter", Icons.Default.Search)

    object Info : Screen("info", "Info", Icons.Default.Person)

}

// 2. Le NavHost qui fait le lien entre routes et écrans
@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: SummitViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Route vers l'écran d'accueil
        composable(Screen.Home.route) { HomeScreen(viewModel = viewModel,
            onSummitClick = { summitId ->
                navController.navigate("detail/$summitId")
            },
            onInfoClick = {
                navController.navigate("info")
            }
        ) }

        // Route vers l'écran d'info
        composable(Screen.Info.route) { InfoScreen(viewModel = viewModel)}

        // Route vers la liste des sommets
        composable(Screen.SummitList.route) { SummitsListScreen( viewModel = viewModel,onNavigateToAddSummit = {
            navController.navigate(Screen.AddSummit.route)
        },
            onNavigateToDetail = { summitId ->
                navController.navigate("detail/$summitId")
            }
        ) }

        // Route vers la carte de tous les sommets
        composable(Screen.Map.route) { MapScreen(viewModel = viewModel) }

        // Route vers l'ajout d'un sommet
        composable(Screen.AddSummit.route) { AddSummitScreen(
            onBack = { navController.popBackStack() },
            onSummitAdded = { navController.popBackStack() },
            viewModel = viewModel
        ) }

        // Route vers les détails d'un sommet
        composable("detail/{summitId}") { backStackEntry ->
            val summitId = backStackEntry.arguments?.getString("summitId")?.toIntOrNull() ?: 0
            SummitDetailScreen(
                summitId = summitId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}