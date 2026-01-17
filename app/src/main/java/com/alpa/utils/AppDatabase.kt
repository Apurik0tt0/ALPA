package com.alpa.utils

import android.content.Context
import androidx.room.*
import com.alpa.ui.screens.TransportMode
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

// ==========================================
// 0. SERIALIZER CUSTOM (Pour l'export JSON)
// ==========================================
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

// ==========================================
// 1. CONVERTERS (Pour Room / Base de données interne)
// ==========================================
class Converters {
    // Convertit LocalDate <-> String (ISO-8601) pour la DB
    @TypeConverter
    fun fromDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    // Convertit TransportMode <-> String
    @TypeConverter
    fun fromTransportMode(mode: TransportMode): String {
        return mode.name
    }

    @TypeConverter
    fun toTransportMode(value: String): TransportMode {
        return try {
            TransportMode.valueOf(value)
        } catch (e: Exception) {
            TransportMode.HIKING // Valeur par défaut si erreur
        }
    }
}

// ==========================================
// 2. ENTITY (La table SQL)
// ==========================================
@Entity(tableName = "summits")
@Serializable // Prêt pour l'export JSON
data class SummitEntity(
    @PrimaryKey val id: String, // UUID généré à la création
    val name: String,
    val altitude: Int,
    val groupName: String? = null, // Nullable si "Sans groupe"
    val isValidated: Boolean = false,

    // On applique le serializer custom
    @Serializable(with = LocalDateSerializer::class)
    val validationDate: LocalDate? = null,

    val transportMode: TransportMode = TransportMode.HIKING,
    val notes: String = "",

    val location: String = "",       // Ex: "Chamonix, France"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// ==========================================
// 3. DAO (Les fonctions d'accès)
// ==========================================
@Dao
interface SummitDao {

    // Récupère tout (Flow = mise à jour auto de l'UI si la DB change)
    @Query("SELECT * FROM summits ORDER BY validationDate DESC, name ASC")
    fun getAllSummits(): Flow<List<SummitEntity>>

    // Récupère un seul sommet par ID
    @Query("SELECT * FROM summits WHERE id = :summitId LIMIT 1")
    suspend fun getSummitById(summitId: String): SummitEntity?

    // Statistiques : Altitude totale validée
    @Query("SELECT SUM(altitude) FROM summits WHERE isValidated = 1")
    fun getTotalAltitudeFlow(): Flow<Int?>

    // Insère ou Met à jour (Si l'ID existe déjà, il l'écrase)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summit: SummitEntity)

    // Insère une liste (utile pour l'import JSON)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summits: List<SummitEntity>)

    // Supprime un sommet spécifique
    @Delete
    suspend fun delete(summit: SummitEntity)

    // --- ACTIONS DE MASSE (Pour votre mode "Edit" liste) ---

    // Supprimer plusieurs sommets par leurs IDs
    @Query("DELETE FROM summits WHERE id IN (:ids)")
    suspend fun deleteSummitsByIds(ids: List<String>)

    // Changer le groupe de plusieurs sommets
    @Query("UPDATE summits SET groupName = :newGroupName WHERE id IN (:ids)")
    suspend fun updateGroupForIds(ids: List<String>, newGroupName: String?)

    // Valider/Dévalider un sommet rapidement
    @Query("UPDATE summits SET isValidated = :isValidated, validationDate = :date WHERE id = :id")
    suspend fun updateValidationStatus(id: String, isValidated: Boolean, date: LocalDate?)
}

// ==========================================
// 4. DATABASE (Le point d'entrée)
// ==========================================
@Database(entities = [SummitEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun summitDao(): SummitDao

    companion object {
        // Singleton : Une seule instance de la DB pour toute l'app
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alpa_database" // Nom du fichier sur le téléphone
                )
                    // Optionnel : pré-remplir la DB au premier lancement
                    // .addCallback(AppDatabaseCallback(scope))

                    // IMPORTANT : Comme on a changé la version de 1 à 2,
                    // ceci va supprimer les anciennes données pour recréer la table proprement.
                    // Pour une app en prod, il faudrait écrire une Migration manuelle.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}