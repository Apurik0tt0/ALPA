package com.alpa.utils

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase // Nécessaire pour le Callback
import com.alpa.ui.screens.TransportMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.util.UUID

// ==========================================
// 0. SERIALIZER CUSTOM
// ==========================================
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) { encoder.encodeString(value.toString()) }
    override fun deserialize(decoder: Decoder): LocalDate { return LocalDate.parse(decoder.decodeString()) }
}

// ==========================================
// 1. CONVERTERS
// ==========================================
class Converters {
    // --- Dates ---
    @TypeConverter
    fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }

    // --- TransportMode LISTE ---
    // On transforme la liste en String: "HIKING,MTB,CLIMBING"
    @TypeConverter
    fun fromTransportModes(modes: List<TransportMode>): String {
        return modes.joinToString(",") { it.name }
    }

    // On transforme la String "HIKING,MTB" en List<TransportMode>
    @TypeConverter
    fun toTransportModes(data: String): List<TransportMode> {
        if (data.isBlank()) return emptyList()

        return data.split(",").mapNotNull { name ->
            try {
                TransportMode.valueOf(name)
            } catch (e: Exception) {
                null // Ignore les valeurs inconnues/corrompues
            }
        }
    }
}

// ==========================================
// 2. ENTITY
// ==========================================
@Entity(tableName = "summits")
@Serializable
data class SummitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val altitude: Int,
    val groupName: String? = null,
    val isValidated: Boolean = false,

    @Serializable(with = LocalDateSerializer::class)
    val validationDate: LocalDate? = null,

    // LISTE, par défaut vide.
    val transportModes: List<TransportMode> = emptyList(),

    val notes: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// ==========================================
// 3. DAO
// ==========================================
@Dao
interface SummitDao {
    @Query("SELECT * FROM summits ORDER BY validationDate DESC, name ASC")
    fun getAllSummits(): Flow<List<SummitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summit: SummitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summits: List<SummitEntity>) // Utilisé pour le pré-remplissage

    @Delete
    suspend fun delete(summit: SummitEntity)

    // ... tes autres méthodes (updateGroupForIds, etc.) restent valides
}

// ==========================================
// 4. DATABASE
// ==========================================
@Database(entities = [SummitEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun summitDao(): SummitDao

    // --- CALLBACK POUR REMPLIR LA DB ---
    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        // Fonction appelée UNE SEULE FOIS lors de la création du fichier DB
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.summitDao())
                }
            }
        }

        // Les données à insérer
        suspend fun populateDatabase(dao: SummitDao) {
            val initialSummits = listOf(
                SummitEntity(
                    name = "Mont Blanc",
                    altitude = 4807,
                    location = "Chamonix, France",
                    transportModes = listOf(TransportMode.HIKING, TransportMode.CLIMBING), // Exemple liste multiple
                    isValidated = false
                ),
                SummitEntity(
                    name = "Puy de Sancy",
                    altitude = 1885,
                    location = "Mont-Dore, France",
                    transportModes = listOf(TransportMode.HIKING), // Exemple liste simple
                    isValidated = true,
                    validationDate = LocalDate.now().minusDays(10)
                ),
                SummitEntity(
                    name = "Grand Colon",
                    altitude = 2394,
                    location = "Belledonne, France",
                    transportModes = emptyList(), // Exemple liste vide
                    notes = "À faire en ski cet hiver"
                )
            )
            dao.insertAll(initialSummits)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // On ajoute le paramètre 'scope' pour pouvoir lancer des coroutines
        // Design pattern singleton
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alpa_database"
                )
                    // ICI : On attache le callback défini plus haut
                    .addCallback(AppDatabaseCallback(scope))

                    // Gestion de la migration destructrice (efface tout si la version change)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}