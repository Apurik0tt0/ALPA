package com.alpa.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SummitViewModel(private val dao: SummitDao) : ViewModel() {

    // 1. Observation des données (Flow)
    val allSummits: Flow<List<SummitEntity>> = dao.getAllSummits()

    // 2. Action : Ajout d'un sommet
    fun addSummit(summit: SummitEntity) {
        viewModelScope.launch {
            dao.insertOrUpdate(summit)
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