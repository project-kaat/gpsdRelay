package com.example.gpsdrelay

interface SocketServerInterface {
    fun send(data : String)
    fun start()
    fun stop()
    fun isReadyToSend() : Boolean
}