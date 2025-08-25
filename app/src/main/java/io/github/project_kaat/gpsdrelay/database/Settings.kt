package io.github.project_kaat.gpsdrelay.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val autostartEnabled : Boolean,
    val autostartNetworkTimeoutS : Int,
    val nmeaGenerationIntervalMs : Long,
    val nmeaIncludeFilter : String
)

@Dao
interface SettingsDao {

    @Upsert
    suspend fun upsert(settings : Settings)

    @Query("SELECT EXISTS(SELECT * FROM Settings WHERE ID=1)")
    fun areSettingsPresent() : Boolean

    @Query("SELECT * FROM Settings WHERE ID=1")
    fun getSettings() : Flow<List<Settings>>

}