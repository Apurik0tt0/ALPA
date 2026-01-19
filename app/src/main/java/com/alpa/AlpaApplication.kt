package com.alpa

import android.app.Application
import com.alpa.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class AlpaApplication : Application() {

    // 1. On crée un scope qui vit tant que l'app est ouverte
    // SupervisorJob permet au scope de continuer même si une tâche échoue
    val applicationScope = CoroutineScope(SupervisorJob())

    // 2. On initialise la base de données de manière "paresseuse" (lazy)
    // Elle ne sera créée que lorsqu'on l'utilisera pour la première fois
    val database by lazy {
        AppDatabase.getDatabase(this, applicationScope)
    }

    // 3. On prépare aussi le Repository (optionnel mais recommandé plus tard)
    // val repository by lazy { SummitRepository(database.summitDao()) }
}