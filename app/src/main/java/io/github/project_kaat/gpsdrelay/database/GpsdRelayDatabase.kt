package io.github.project_kaat.gpsdrelay.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Server::class, Settings::class], version = 1)
abstract class GpsdRelayDatabase : RoomDatabase() {
    abstract val serverDao : ServerDao
    abstract val settingsDao : SettingsDao
}