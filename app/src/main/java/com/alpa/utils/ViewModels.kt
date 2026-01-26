package com.alpa.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SummitViewModel(private val dao: SummitDao) : ViewModel() {

    // 1. Observation des données (Flow)
    public val allSummits: Flow<List<SummitEntity>> = dao.getAllSummits()
    public val allGroups: Flow<List<String>> = dao.getAllGroups()

    // --- Outils pour la carte et obtenir la position ---

    private val locationService = LocationService()
    // Pour stocker la position et l'afficher sur la carte
    var userLocation by mutableStateOf<Location?>(null)
    // État pour afficher un message d'erreur à l'utilisateur
    var errorMessage by mutableStateOf<String?>(null)

    @SuppressLint("MissingPermission")
    fun fetchLocation(context: Context) {
        viewModelScope.launch {
            try {
                errorMessage = null // Reset de l'erreur
                val location = locationService.getCurrentLocation(context)
                userLocation = location
            } catch (e: LocationService.LocationServiceException) {
                // Traduction des exceptions en messages clairs

                errorMessage = when(e) {
                    is LocationService.LocationServiceException.MissingPermissionException ->
                        "Permission refusée. Veuillez l'activer dans les paramètres."
                    is LocationService.LocationServiceException.LocationDisabledException ->
                        "Le GPS est désactivé. Veuillez l'activer."
                    is LocationService.LocationServiceException.NoInternetException ->
                        "Connexion internet requise pour la localisation."
                    else -> "Une erreur inconnue est survenue."
                }
                Log.d("test", errorMessage.toString())
            }
        }
    }



    // 2. Action : Ajout d'un sommet
    fun addSummit(summit: SummitEntity) {
        viewModelScope.launch {
            dao.insertOrUpdate(summit)
        }
    }

    fun importFromJSON(jsonString: String) {
        try {
            val summitList = deserializeSummitsFromJson(jsonString)
            viewModelScope.launch {
                dao.insertAll(summitList)
            }
        }
        catch (e: Exception){
            Log.d("test", e.toString())
        }
    }

    fun getSummitById(id: Int): Flow<SummitEntity> = dao.getSummitById(id)

    fun updateSummit(summit: SummitEntity) {
        viewModelScope.launch {
            dao.insertOrUpdate(summit)
        }
    }

    fun toggleTransportMode(summit: SummitEntity, mode: TransportMode) {
        // 1. Calcul de la nouvelle liste des modes
        val currentModes = summit.transportModes
        val newModes = if (currentModes.contains(mode)) {
            currentModes - mode
        } else {
            currentModes + mode
        }

        // 2. Détermination automatique de l'état de validation
        // Si la nouvelle liste n'est pas vide, le sommet est validé
        val isNowValidated = newModes.isNotEmpty()

        // On définit la date :
        // - Si on vient de valider (était vide avant), on met la date du jour.
        // - Si on dévalide (devient vide), on met null.
        // - Sinon, on garde la date existante.
        val newValidationDate = when {
            isNowValidated && summit.validationDate == null -> LocalDate.now()
            !isNowValidated -> null
            else -> summit.validationDate
        }

        // 3. Création de la copie avec les nouveaux états
        val updatedSummit = summit.copy(
            transportModes = newModes,
            isValidated = isNowValidated,
            validationDate = newValidationDate
        )

        // 4. Sauvegarde
        updateSummit(updatedSummit)
    }

    fun updateGroupForList(selectedIds: List<Int>, newGroup: String?) {
        // On lance une coroutine car c'est une opération I/O (base de données)
        viewModelScope.launch {
            // On appelle une fonction spéciale du DAO pour tout mettre à jour d'un coup
            dao.updateRangeForIds(selectedIds, newGroup)
        }
    }

    fun deleteSummits(selectedIds: List<Int>) {
        viewModelScope.launch {
            dao.deleteByIds(selectedIds)
        }
    }

    fun logSummits() {
        viewModelScope.launch {
            // On "collecte" le flux pour voir ce qu'il y a dedans
            allSummits.collect { list ->
                Log.d("D","Nombre de sommets : ${list.size}")
            }
        }
    }

    // 3. LA FACTORY (Notice de fabrication)
    // Elle permet de dire à Android comment créer ce ViewModel avec son paramètre DAO
    companion object {
        fun provideFactory(dao: SummitDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SummitViewModel::class.java)) {
                    return SummitViewModel(dao) as T
                }
                throw IllegalArgumentException("Classe ViewModel inconnue")
            }
        }
    }
}