package io.github.project_kaat.gpsdrelay

interface SocketServerInterface {
    fun send(data : String)
    fun start()
    fun stop()
    fun isReadyToSend() : Boolean
}