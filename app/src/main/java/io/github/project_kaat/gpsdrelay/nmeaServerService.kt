package io.github.project_kaat.gpsdrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.hardware.GeomagneticField
import android.location.OnNmeaMessageListener
import android.net.ConnectivityManager
import android.net.Network
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.github.project_kaat.gpsdrelay.database.GpsdRelayDatabase
import io.github.project_kaat.gpsdrelay.network.OutgoingMessage
import io.github.project_kaat.gpsdrelay.network.SocketServerInterface
import io.github.project_kaat.gpsdrelay.network.tcpSocketServer
import io.github.project_kaat.gpsdrelay.network.udpSocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class nmeaServerService : Service(), OnNmeaMessageListener, LocationListener {

    inner class networkCallback : ConnectivityManager.NetworkCallback() {
        private var netAvailable = false
        override fun onAvailable(network: Network) {
            Log.d(TAG, "net is available")
            netAvailable = true
            if (runningServerList.isEmpty()) {
                startServers()
            }

        }

        override fun onUnavailable() {
            Log.d(TAG, "net is unavailable")
            netAvailable = false
            stopServersAfterDelay(2.seconds)
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "net is lost. (it's so over '_')")
            netAvailable = false
            stopServersAfterDelay(2.seconds)
        }

        private fun stopServersAfterDelay(delay : Duration) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                if (!netAvailable && runningServerList.isNotEmpty()) {
                    stopServers()
                }
            }, delay.inWholeMilliseconds)
        }
    }

    private val TAG = "nmeaServerService"

    private val netCallback = networkCallback()

    private lateinit var database : GpsdRelayDatabase

    private lateinit var connectivityManager : ConnectivityManager
    private var monitorDefaultNetwork = false
    private lateinit var locationManager : LocationManager

    private var sendInterval : Long = 0
    private var isMockLocation : Boolean = false
    private val serverList : MutableList<SocketServerInterface> = mutableListOf()
    private val runningServerList : MutableList<SocketServerInterface> = mutableListOf()

    fun onServerStop(server : SocketServerInterface) {
        runningServerList -= server
    }


    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        database = (application as gpsdRelay).gpsdRelayDatabase
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        connectivityManager = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private fun isMockLocationEnabled(location : Location) : Boolean {
        // API level 24-30 doesn't support Location.isMock() for this functionality
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            return location.isFromMockProvider
        } else {
            return location.isMock
        }
    }

    private fun timestampToUTC(timeMS : Long) : Array<String> {
        val datetimeFormat = SimpleDateFormat("ddMMyy-HHmmss.00", Locale.US).also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }
        val datetimeString = datetimeFormat.format(Date(timeMS))
        return arrayOf(datetimeString.substringBefore('-'), datetimeString.substringAfter('-'))
    }

    private fun ddToDDMM(decimalDegrees : Double, isLatitude : Boolean) : String {
        val degrees = decimalDegrees.toInt()
        val minutes = abs( ((decimalDegrees - degrees) * 60)  )
        val minuteParts = minutes.toString().split(".")
        val integerPartOfMinutes = minuteParts[0].padStart(2, '0')
        val fractionalPartOfMinutes = minuteParts.getOrNull(1)?.padEnd(6, '0')?.substring(0, 6) ?: "000000"
        val coordinate : String

        val orientation = if (isLatitude) {
            if (decimalDegrees > 0) ",N" else ",S"
        } else {
            if (decimalDegrees > 0) ",E" else ",W"
        }

        // Since the orientation is returned, the degrees should never be negative
        val paddedDegrees = abs(degrees).toString().padStart(if (isLatitude) 2 else 3, '0')
        coordinate = "$paddedDegrees$integerPartOfMinutes.$fractionalPartOfMinutes$orientation"

        return coordinate
    }

    private fun getChecksumString(message : String) : String {
        var checksum = 0
        for (char in message) {
            when (char) {
                '$' -> continue
                '*' -> break
                else -> {
                    checksum = checksum.xor(char.code)
                }
            }
        }

        return String.format("%02X", checksum)
    }

    private fun getGNGGA(p0 : Location) : String {
        /*
        NMEA GGA standard referenced from https://gpsd.io/NMEA.html#_gga_global_positioning_system_fix_data
        Example:
        $GNGGA,001043.00,4404.14036,N,12118.85961,W,1,12,0.98,1113.0,M,-21.3,M*47

        $--GGA,hhmmss.ss,ddmm.mm,a,ddmm.mm,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh<CR><LF>

        Field Number:
            0.  Talker ID + GGA
            1.  UTC of this position report, hh is hours, mm is minutes, ss.ss is seconds.
            2.  Latitude, dd is degrees, mm.mm is minutes
            3.  N or S (North or South)
            4.  Longitude, dd is degrees, mm.mm is minutes
            5.  E or W (East or West)
            6.  GPS Quality Indicator (non null)
                 0 - fix not available,
                 1 - GPS fix,
                 2 - Differential GPS fix (values above 2 are 2.3 features)
                 3 = PPS fix
                 4 = Real Time Kinematic
                 5 = Float RTK
                 6 = estimated (dead reckoning)
                 7 = Manual input mode
                 8 = Simulation mode
            7.  Number of satellites in use, 00 - 12
            8.  Horizontal Dilution of precision (meters)
            9.  Antenna Altitude above/below mean-sea-level (geoid) (in meters)
            10. Units of antenna altitude, meters
            11. Geoidal separation, the difference between the WGS-84 earth ellipsoid and mean-sea-level (geoid), "-" means mean-sea-level below ellipsoid
            12. Units of geoidal separation, meters
            13. Age of differential GPS data, time in seconds since last SC104 type 1 or 9 update, null field when DGPS is not used
            14. Differential reference station ID, 0000-1023
            15. Checksum

            The number of digits past the decimal point for Time, Latitude and Longitude is model dependent.
        */
        val utcDateTime = timestampToUTC(p0.time)
        val latString = ddToDDMM(p0.latitude, true)
        val longString = ddToDDMM(p0.longitude, false)
        val altString = String.format("%.1f", p0.altitude)
        val accuracyString = String.format("%.1f", p0.accuracy)

        // Fields 6 and 7 will always represent a fix with 12 satellites
        // Lat and long are fields 2,3 and 4,5 because they include the orientation
        // Geoidal separation will always report as 0 M
        var msg = "\$GNGGA,${utcDateTime[1]},${latString},${longString},1,12,${accuracyString},${altString},M,0,M,,*"
        msg += getChecksumString(msg) + "\r\n"

        return msg
    }

    private fun getGNVTG(p0 : Location) : String {
    /*
        NMEA GGA standard referenced from: https://gpsd.io/NMEA.html#_vtg_track_made_good_and_ground_speed
        Example: $GPVTG,220.86,T,,M,2.550,N,4.724,K,A*34

        $--VTG,x.x,T,x.x,M,x.x,N,x.x,K*hh<CR><LF>
        NMEA 2.3:
        $--VTG,x.x,T,x.x,M,x.x,N,x.x,K,m*hh<CR><LF>

        Field Number:
            0.  Talker ID + VTG
            1.  Course over ground, degrees True
            2.  T = True
            3.  Course over ground, degrees Magnetic
            4.  M = Magnetic
            5.  Speed over ground, knots
            6.  N = Knots
            7.  Speed over ground, km/hr
            8.  K = Kilometers Per Hour
            9.  FAA mode indicator (NMEA 2.3 and later)
            10. Checksum
     */
        val courseTrueString = String.format("%.2f", p0.bearing)

        // Finding the declination angle is necessary to calculate the magnetic course
        val geomagneticField = GeomagneticField(
            p0.latitude.toFloat(),
            p0.longitude.toFloat(),
            p0.altitude.toFloat(),
            p0.time
        )
        val declination = geomagneticField.declination
        val courseMagString = String.format("%.2f", ( (p0.bearing + declination) % 360) )

        /*
            Speed is returned in M/s so the following conversion formulas are needed:
            M/s to knots = (meters_per_second * 3600) / 1852
            M/s to km/hr = meters_per_second * 3.6
         */
        val knotsString = String.format("%.1f", (p0.speed * 3600 / 1852 ) )
        val kmPerHrString = String.format("%.1f", (p0.speed * 3.6) )

        // Always returns autonomous FAA mode
        var msg = "\$GNVTG,${courseTrueString},T,${courseMagString},M,${knotsString},N,${kmPerHrString},K,A*"
        msg += getChecksumString(msg) + "\r\n"

        return msg
    }

    private fun getGPRMC(p0 : Location) : String {
        /*
            NMEA RMC standard referenced from: https://gpsd.io/NMEA.html#_rmc_recommended_minimum_navigation_information
            Example: $GNRMC,001031.00,A,4404.13993,N,12118.86023,W,0.146,,100117,,,A*7B

            $--RMC,hhmmss.ss,A,ddmm.mm,a,dddmm.mm,a,x.x,x.x,xxxx,x.x,a*hh<CR><LF>
            NMEA 2.3:
            $--RMC,hhmmss.ss,A,ddmm.mm,a,dddmm.mm,a,x.x,x.x,xxxx,x.x,a,m*hh<CR><LF>
            NMEA 4.1:
            $--RMC,hhmmss.ss,A,ddmm.mm,a,dddmm.mm,a,x.x,x.x,xxxx,x.x,a,m,s*hh<CR><LF>

            Field Number:
                0.  Talker ID + RMC
                1.  UTC of position fix, hh is hours, mm is minutes, ss.ss is seconds.
                2.  Status, A = Valid, V = Warning
                3.  Latitude, dd is degrees. mm.mm is minutes.
                4.  N or S
                5.  Longitude, ddd is degrees. mm.mm is minutes.
                6.  E or W
                7.  Speed over ground, knots
                8.  Track made good, degrees true
                9.  Date, ddmmyy
                10. Magnetic Variation, degrees
                11. E or W
                12. FAA mode indicator (NMEA 2.3 and later)
                13. Nav Status (NMEA 4.1 and later) A=autonomous, D=differential, E=Estimated, M=Manual input mode N=not valid, S=Simulator, V = Valid
                14. Checksum
         */

        val utcDateTime = timestampToUTC(p0.time)
        val latString = ddToDDMM(p0.latitude, true)
        val longString = ddToDDMM(p0.longitude, false)
        val courseTrueString = p0.bearing

        val knotsString = ( (p0.speed) * 3600 / 1852).toString()

        var msg =
            "\$GPRMC,${utcDateTime[1]},A,${latString},${longString},${knotsString},${courseTrueString},${utcDateTime[0]},,,A,V*"

        msg += getChecksumString(msg) + "\r\n"
        return msg
    }

    override fun onLocationChanged(location: Location) {

        isMockLocation = isMockLocationEnabled(location)

        // If we are receiving mock location data, then create GPGGA and GPVTG strings to pass
        val msg = if (isMockLocation)
            getGNGGA(location) + getGNVTG(location)
        else
            getGPRMC(location)

        runningServerList.forEach() {
            if (it.isConnected()) {
                it.send(OutgoingMessage(isGenerated = true, msg))
            }
        }
    }

    override fun onNmeaMessage(p0: String?, p1: Long) {
        //Log.i("SERVICE", "nmea message received: ${p0}")
        if (p0 != null) {
            runningServerList.forEach() {
                if (it.isConnected()) {
                    it.send(OutgoingMessage(isGenerated = false, p0))
                }
            }
        }
    }

    private fun startServers() {
        Log.d(TAG, "starting servers")
        serverList.forEach() {
            it.start()
            runningServerList += it
        }
    }

    private fun stopServers() {
        Log.d(TAG, "stopping servers")
        serverList.forEach() {
            it.stop()
            runningServerList -= it
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    if (intent == null || intent.action == getString(R.string.INTENT_ACTION_START_SERVICE)) {

        Log.d(TAG, "Service is starting")

        runBlocking() {
            GlobalScope.launch {
                sendInterval =
                    database.settingsDao.getSettings().first()[0].nmeaGenerationIntervalMs

                monitorDefaultNetwork =
                    database.settingsDao.getSettings().first()[0].monitorDefaultNetworkEnabled

                //init enabled tcp servers (just 1 atm)

                val tcpEnabled = database.serverDao.getAllTcpEnabled()
                if (tcpEnabled.isNotEmpty()) {
                    for (tcpServer in tcpEnabled) {
                        serverList += tcpSocketServer(tcpServer, this@nmeaServerService)
                    }
                }

                //init enabled udp clients in 1 server thread

                val udpEnabled = database.serverDao.getAllUdpEnabled()
                if (udpEnabled.isNotEmpty()) {
                    serverList += udpSocketServer(udpEnabled, this@nmeaServerService)
                }
            }.join()
        }

        if (monitorDefaultNetwork) {
            connectivityManager.registerDefaultNetworkCallback(netCallback)
        }


        startServers()

        startSelfForeground()

        enableLocationUpdates()

        return START_STICKY

        }
    else {
        Log.d(TAG, "Service is stopping")
        stopSelf()
        stopForeground(STOP_FOREGROUND_REMOVE)
        return START_NOT_STICKY
    }
}

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        disableLocationUpdates()
        stopServers()
        if (monitorDefaultNetwork) {
            connectivityManager.unregisterNetworkCallback(netCallback)
        }
        super.onDestroy()

    }

    private fun startSelfForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = packageName
            val channelName = getString(R.string.notification_channel_name)
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_NONE
            )
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            val notification = notificationBuilder.setOngoing(true)
                .setContentTitle(getString(R.string.notification_service_running))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(3, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
            }
            else {
                startForeground(2, notification)
            }

        } else {
            startForeground(1, Notification())
        }

    }

    private fun enableLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, sendInterval, 0f, this
            )
            locationManager.addNmeaListener(this, null)
        } catch (e: SecurityException) {
            Log.e(TAG, "GPS access security exception")
            Toast.makeText(this, getString(R.string.service_gps_denied), Toast.LENGTH_LONG).show()
        }
    }

    private fun disableLocationUpdates() {
        locationManager.removeUpdates(this)
        locationManager.removeNmeaListener(this)

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //this is needed to prevent crashes on older versions of android
    }
}
