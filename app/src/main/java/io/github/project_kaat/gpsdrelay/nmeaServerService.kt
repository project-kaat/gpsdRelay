package io.github.project_kaat.gpsdrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class nmeaServerService : Service(), OnNmeaMessageListener, LocationListener {
    private val TAG = "nmeaServerService"

    private lateinit var locationManager : LocationManager
    private var sendInterval : Long = 0
    private lateinit var socketServer : SocketServerInterface
    private lateinit var handler : Handler
    private var generateOwnNMEA : Boolean = false
    private var relayNMEA : Boolean = false

    private val isServerReady = object : Runnable {
        override fun run() {
            if (socketServer.isReadyToSend()) {
                enableLocationUpdates()
            }
            else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun timestampToUTC(timeMS : Long) : Array<String> {

        val datetimeFormat = SimpleDateFormat("ddMMyy-HHmmss.00", Locale.US)
        val datetimeString = datetimeFormat.format(Date(timeMS))
        return arrayOf(datetimeString.substringBefore('-'), datetimeString.substringAfter('-'))
    }

    private fun ddToDDMM(decimalDegrees : Double, isLatitude : Boolean) : String {
        val degrees = decimalDegrees.toInt()
        val minutes = abs( ((decimalDegrees - degrees) * 60)  )
        val orientation : String // Store the easting/westing northing/southing indicators
        val coordinate : String

        // Since the orientation is returned, the degrees should never be negative
        if (isLatitude) {
            if (decimalDegrees > 0) {
                orientation = ",N"
            }
            else {
                orientation = ",S"
            }
            coordinate = abs(degrees).toString().padStart(2, '0') +
                    minutes.toString().padStart(2, '0').substring(0, 7) +
                    orientation
        }
        else {
            if (decimalDegrees > 0) {
                orientation = ",E"
            }
            else {
                orientation = ",W"
            }
            coordinate = abs(degrees).toString().padStart(3, '0') +
                    minutes.toString().padStart(2, '0').substring(0, 7) +
                    orientation
        }

        return coordinate
    }

    override fun onLocationChanged(p0: Location) {

        if (generateOwnNMEA) {

            val utcDateTime = timestampToUTC(p0.time)
            val latString = ddToDDMM(p0.latitude, true)
            val longString = ddToDDMM(p0.longitude, false)

            var msg = "\$GPRMC,${utcDateTime[1]},A,${latString},${longString},,,${utcDateTime[0]},,,,V*"
            var checksum = 0
            for (char in msg) {
                when (char) {
                    '$' -> continue
                    '*' -> break
                    else -> {
                        checksum = checksum.xor(char.code)
                    }
                }
            }

            msg += checksum.toString(16) + "\r\n"
            
            socketServer.send(msg)
        }
    }

    override fun onNmeaMessage(p0: String?, p1: Long) {
        //Log.i("SERVICE", "nmea message received: ${p0}")
        if (p0 != null) {
            socketServer.send(p0)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.action == getString(R.string.INTENT_ACTION_START_SERVICE)) {

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)

            sendInterval = prefs.getString(getString(R.string.settings_key_sync_interval), getString(
                R.string.settings_sync_interval_default
            ))!!.toLong()
            generateOwnNMEA = prefs.getBoolean(getString(R.string.settings_key_generate_nmea), false)
            relayNMEA = prefs.getBoolean(getString(R.string.settings_key_relay_nmea), false)
            startSelfForeground()
            when (prefs.getString(getString(R.string.settings_key_server_type), getString(R.string.settings_server_type_default))) {
                "TCP" ->
                    socketServer = tcpSocketServer(
                        prefs.getString(getString(R.string.settings_key_ipa_src), getString(R.string.settings_ipa_src_default))!!,
                        prefs.getString(getString(R.string.settings_key_ipp_src), getString(R.string.settings_ipp_src_default))!!,
                    )
                "UDP" ->
                    socketServer = udpSocketServer(
                        prefs.getString(getString(R.string.settings_key_ipa_src), getString(R.string.settings_ipa_src_default))!!,
                        prefs.getString(getString(R.string.settings_key_ipp_src), getString(R.string.settings_ipp_src_default))!!,
                        prefs.getString(getString(R.string.settings_key_ipa_dst), getString(R.string.settings_ipa_dst_default))!!,
                        prefs.getString(getString(R.string.settings_key_ipp_dst), getString(R.string.settings_ipp_dst_default))!!
                    )
            }
            
            socketServer.start()
            locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            handler = Handler(mainLooper)

            handler.post(isServerReady)

            return START_STICKY

            }
        else {
            //Log.i(TAG, "Service is stopping")
            stopSelf()
            stopForeground(true)
            return START_NOT_STICKY
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        socketServer.stop()
        disableLocationUpdates()
        super.onDestroy()

    }

    private fun startSelfForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = packageName
            val channelName = getString(R.string.notification_channel_name)
            val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            val notification = notificationBuilder.setOngoing(true)
                .setContentTitle(getString(R.string.notification_service_running))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(2, notification)

        }
        else {
            startForeground(1, Notification())
        }

    }

    private fun enableLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                sendInterval,
                0f,
                this
            )
            if (relayNMEA) {
                locationManager.addNmeaListener(this, null)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "GPS access security exception")
            Toast.makeText(this, "Not authorized to use GPS", Toast.LENGTH_LONG).show()
        }
    }

    private fun disableLocationUpdates() {
        locationManager.removeUpdates(this)
        if (relayNMEA) {
            locationManager.removeNmeaListener(this)
        }

    }
}
