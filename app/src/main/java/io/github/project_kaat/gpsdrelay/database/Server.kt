package io.github.project_kaat.gpsdrelay.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

fun staticFromListOfStringToString(list: List<String>): String =
    list.joinToString(separator = ";")

fun staticFromStringToListOfString(string: String): List<String>  {
    if (string.isEmpty()) {
        return emptyList()
    }
    else {
        return string.split(';')
    }
}
class ServerTypeConverters {

    @TypeConverter
    fun fromListOfStringToString(list: List<String>): String = staticFromListOfStringToString(list)

    @TypeConverter
    fun fromStringToListOfString(string: String): List<String> = staticFromStringToListOfString(string)
}

@Entity(primaryKeys = ["type", "ipv4", "port"])
data class Server(
    val type: Int,
    val ipv4: String,
    val port: Int,
    val relayingEnabled: Boolean,
    val generationEnabled: Boolean,
    val creationTimestamp: Long,
    val enabled: Boolean,
    val relayFilter : List<String>
) {
    companion object {
        fun validateAndCreate(type : GpsdServerType, ipv4 : String, port : String, relayingEnabled: Boolean, generationEnabled: Boolean, creationTimestamp: Long, enabled: Boolean, relayFilter : List<String>) : Server?{
            val ipv4 = ipv4.trim()
            if (!Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!$)|$)){4}$").matches(ipv4)) {
                return null
            }
            if (port.isEmpty() || port.toInt() !in (1..65535)) {
                return null
            }
            if (type == GpsdServerType.TCP && port.toInt() < 1024) {
                return null
            }
            if (creationTimestamp > System.currentTimeMillis()) {
                return null
            }
            return Server(type.code, ipv4, port.toInt(), relayingEnabled, generationEnabled, creationTimestamp, enabled, relayFilter)
        }
    }
}

@Dao
interface ServerDao {

    @Query("SELECT * FROM Server ORDER BY creationTimestamp")
    fun getAllSortByCreationTime(): Flow<List<Server>>

    @Query("SELECT * FROM Server WHERE type=1 AND enabled=1")
    fun getAllTcpEnabled() : List<Server>

    @Query("SELECT * FROM Server WHERE type=2 AND enabled=1")
    fun getAllUdpEnabled() : List<Server>

    @Delete
    suspend fun delete(server : Server)

    @Upsert
    suspend fun upsert(server : Server)

    @Query("UPDATE Server Set enabled=0 WHERE type=1")
    suspend fun deactivateAllTcp()
}
