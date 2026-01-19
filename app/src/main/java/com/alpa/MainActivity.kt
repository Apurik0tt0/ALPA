package com.alpa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.alpa.ui.theme.ALPATheme
import com.alpa.ui.MainScreen
import com.alpa.utils.SummitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Récupérer l'instance de la DB via la classe Application
        val app = application as AlpaApplication
        val dao = app.database.summitDao()

        // 2. Instancier le ViewModel via le ViewModelProvider et notre Factory
        // C'est ce qui permet au ViewModel de survivre à la rotation de l'écran !
        val summitViewModel = ViewModelProvider(
            this,
            SummitViewModel.provideFactory(dao)
        )[SummitViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            ALPATheme {
                MainScreen(viewModel = summitViewModel)
            }
        }
    }
}