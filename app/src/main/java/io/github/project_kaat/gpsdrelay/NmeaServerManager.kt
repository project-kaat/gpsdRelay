package io.github.project_kaat.gpsdrelay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        netcallback = NetCallback(this)
        connman = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    /*var isServiceRunning : Boolean = false
        private set*/

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning : StateFlow<Boolean> = _isServiceRunning


    fun startService() : Boolean {
        if (!checkRequiredPermissions()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Toast.makeText(context, context.getString(R.string.manager_permissions_required_pre_10), Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(context, context.getString(R.string.manager_permissions_required), Toast.LENGTH_LONG).show()
            }
            return false
        }

        if (_isServiceRunning.value) {
            Toast.makeText(context, context.getString(R.string.manager_service_already_running), Toast.LENGTH_LONG).show()
            return false
        }

        if (autostartingService) { //if request came from autostart - good. if it didn't - cancel anyway
            autostartingService = false
            connman?.unregisterNetworkCallback(netcallback)

        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //check if GPS is enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, context.getString(R.string.manager_gps_not_enabled), Toast.LENGTH_LONG).show()
            return false
        }

        val startIntent = Intent(context, nmeaServerService::class.java)
        startIntent.action = context.getString(R.string.INTENT_ACTION_START_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        } else {
            context.startService(startIntent)
        }
        _isServiceRunning.value=true
        return true
    }

    fun stopService() {
        if (!isServiceRunning.value) {
            Toast.makeText(context, context.getString(R.string.manager_service_already_stopped), Toast.LENGTH_LONG).show()
            return
        }
        val stopIntent = Intent(context, nmeaServerService::class.java)
        stopIntent.action = context.getString(R.string.INTENT_ACTION_STOP_SERVICE)
        context.startService(stopIntent)
        _isServiceRunning.value = false
    }

    private fun checkRequiredPermissions() : Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return checkLocationPermission()
        }
        else {
            return checkLocationPermission() && checkBackgroundLocationPermission()
        }
    }


    fun checkLocationPermission() : Boolean {
        val locationGranted = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return locationGranted
    }

    fun checkBackgroundLocationPermission() : Boolean {
        val backgroundLocationGranted = context.checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return backgroundLocationGranted
    }
}