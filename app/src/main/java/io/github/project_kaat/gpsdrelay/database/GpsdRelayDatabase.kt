package io.github.project_kaat.gpsdrelay.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Server ADD relayFilter TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Settings ADD monitorDefaultNetworkEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Server::class, Settings::class], version = 3)
@TypeConverters(ServerTypeConverters::class)
abstract class GpsdRelayDatabase : RoomDatabase() {
    abstract val serverDao : ServerDao
    abstract val settingsDao : SettingsDao
}