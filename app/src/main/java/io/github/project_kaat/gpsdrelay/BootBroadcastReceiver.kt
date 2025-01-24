package io.github.project_kaat.gpsdrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BootBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "BootBroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, "onReceive START")

        var autostartEnabled = false
        var autostartTimeout = 0

        runBlocking() {
            GlobalScope.launch() {
                Log.d(TAG, "launched coroutine")
                val settings =
                    (context.applicationContext as gpsdRelay).gpsdRelayDatabase.settingsDao.getSettings().first()[0]

                autostartEnabled = settings.autostartEnabled
                autostartTimeout = settings.autostartNetworkTimeoutS

            }.join()
        }

        if (autostartEnabled) {

            Log.d(TAG, "autostart is enabled")
            (context.applicationContext as gpsdRelay).serverManager.awaitConnectionAndStartService(autostartTimeout)
            Log.d(TAG, "service autostarted")
        }


    }
}