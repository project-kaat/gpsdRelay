package io.github.project_kaat.gpsdrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceControlBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val app = context.applicationContext as gpsdRelay

        when (intent.action) {
            context.getString(R.string.INTENT_ACTION_START_SERVICE) -> {
                app.serverManager.startService()
            }
            context.getString(R.string.INTENT_ACTION_STOP_SERVICE) -> {
                app.serverManager.stopService()
            }
        }
    }
}