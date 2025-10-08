package io.github.project_kaat.gpsdrelay

import android.app.Application
import androidx.room.Room
import io.github.project_kaat.gpsdrelay.database.GpsdRelayDatabase
import io.github.project_kaat.gpsdrelay.database.MIGRATION_1_2
import io.github.project_kaat.gpsdrelay.database.MIGRATION_2_3
import io.github.project_kaat.gpsdrelay.database.Settings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class gpsdRelay : Application() {

    val serverManager = NmeaServerManager(this)

    lateinit var gpsdRelayDatabase: GpsdRelayDatabase

    override fun onCreate() {
        super.onCreate()
        gpsdRelayDatabase = Room.databaseBuilder(
            applicationContext, GpsdRelayDatabase::class.java,
            getString(R.string.db_name)
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        GlobalScope.launch() {
            if (!gpsdRelayDatabase.settingsDao.areSettingsPresent()) {
                initializeSettings()
            }
        }
    }

    private suspend fun initializeSettings() {

        gpsdRelayDatabase.settingsDao.upsert(
            Settings(
                1,
                false,
                60,
                1000,
                false
            )
        )

    }
}