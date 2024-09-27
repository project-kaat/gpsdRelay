package io.github.project_kaat.gpsdrelay

import android.app.Application

class gpsdrelay : Application() {

    val serverManager = NmeaServerManager(this)

}