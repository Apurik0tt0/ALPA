package com.alpa.utils

import android.util.Log
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

    // 2. Action : Ajout d'un sommet
    fun addSummit(summit: SummitEntity) {
        viewModelScope.launch {
            dao.insertOrUpdate(summit)
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