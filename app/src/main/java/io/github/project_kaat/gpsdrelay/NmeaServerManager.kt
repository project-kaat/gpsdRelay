package io.github.project_kaat.gpsdrelay

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

internal class NetCallback(private val serverManager : NmeaServerManager) : NetworkCallback() {
    override fun onAvailable(network: Network) {
        serverManager.networkAvailable.set(true)
    }
}



class NmeaServerManager(private val context : Context) : NetworkCallback() {

    private var autostartingService = false
    private var connman : ConnectivityManager? = null
    private lateinit var netcallback : NetCallback
    internal val networkAvailable = AtomicBoolean(false)

    fun awaitConnectionAndStartService(timeout : Int) {
        autostartingService = true
        connman = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        netcallback = NetCallback(this)
        connman?.registerDefaultNetworkCallback(netcallback)

        val timeEnd =  System.currentTimeMillis() + (timeout * 1000)

        while (autostartingService && System.currentTimeMillis() < timeEnd) { //busy wait until timeout to keep the app alive
            if (networkAvailable.get()) {
                startService()
            }
            Thread.sleep(100)
        }
        connman?.unregisterNetworkCallback(netcallback)
        autostartingService = false
    }

    var isServiceRunning : Boolean = false
        private set


    fun startService() : Boolean {
        if (isServiceRunning) {
            Toast.makeText(context, "Can't start gpsdRelay service as it seems to be already running", Toast.LENGTH_LONG).show()
            return false
        }

        if (autostartingService) { //if request came from autostart - good. if it didn't - cancel anyway
            autostartingService = false
            connman?.unregisterNetworkCallback(netcallback)

        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //check if GPS is enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "Enable GPS first", Toast.LENGTH_LONG).show()
            return false
        }

        val startIntent = Intent(context, nmeaServerService::class.java)
        startIntent.action = context.getString(R.string.INTENT_ACTION_START_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        } else {
            context.startService(startIntent)
        }
        isServiceRunning = true
        return true
    }

    fun stopService() {
        if (!isServiceRunning) {
            Toast.makeText(context, "Can't start gpsdRelay service as it seems to be already stopped", Toast.LENGTH_LONG).show()
            return
        }
        val stopIntent = Intent(context, nmeaServerService::class.java)
        stopIntent.action = context.getString(R.string.INTENT_ACTION_STOP_SERVICE)
        context.startService(stopIntent)
        isServiceRunning = false
    }
}