package io.github.project_kaat.gpsdrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ServiceControlBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("bastreceiver", "received intent. action = ${intent.action}")
        val app = context.applicationContext as gpsdrelay

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