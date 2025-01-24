package io.github.project_kaat.gpsdrelay.network

interface SocketServerInterface {
    fun send(message : OutgoingMessage)
    fun start()
    fun stop()
    fun isConnected() : Boolean
}