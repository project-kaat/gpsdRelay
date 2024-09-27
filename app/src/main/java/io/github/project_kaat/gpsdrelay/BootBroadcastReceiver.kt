package io.github.project_kaat.gpsdrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("bootBroadcastReceiver", "boot intent received")
        val app = context.applicationContext as gpsdrelay
        val prefs = PreferenceManager.getDefaultSharedPreferences(app)
        if (prefs.getBoolean(app.getString(R.string.settings_key_autostart_service), false)) {
            Log.d("bootBroadcastReceiver", "autostart is enabled")

            val timeout = try {
                prefs.getString(
                    app.getString(R.string.settings_key_autostart_timeout),
                    app.getString(R.string.settings_autostart_timeout_default)
                )!!.toInt()
            } catch (ignore : Exception) {
                app.getString(R.string.settings_autostart_timeout_default).toInt()
            }

            app.serverManager.awaitConnectionAndStartService(timeout)
        }

    }
}