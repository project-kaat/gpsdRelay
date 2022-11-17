package io.github.project_kaat.gpsdrelay

import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.net.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class tcpSocketServer (private val ipv4AddressSrc : String, private val ipv4PortSrc : String) :
    SocketServerInterface {

    private val TAG = "tcpSocketServer"
    private lateinit var networkThread : NetworkThread
    private var clientConnected = AtomicBoolean()

    override fun start() {
        clientConnected.set(false)
        val src : InetSocketAddress
        try {
            src = InetSocketAddress(ipv4AddressSrc, ipv4PortSrc.toInt())
        }
        catch (e : UnknownHostException) {
            Log.e(TAG, "Invalid ip AND/OR port supplied")
            return
        }
        networkThread = NetworkThread(src, clientConnected)
        networkThread.start()
    }


    private class NetworkThread(private val srcAddress : SocketAddress, val clientConnected : AtomicBoolean) : Thread() {
        private val TAG = "tcpSocketServer.NetworkThread"
        val messageQueue: ArrayBlockingQueue<String> = ArrayBlockingQueue(30)
        private lateinit var tcpSocket: ServerSocket
        private lateinit var clientSocket: Socket
        private lateinit var clientOutputStream: OutputStream

        override fun run() {
            try {
                tcpSocket = ServerSocket()
                tcpSocket.reuseAddress = true
                tcpSocket.bind(srcAddress)
                tcpSocket.soTimeout = 500
                while (!currentThread().isInterrupted) {
                    while (true) {
                        try {
                            clientSocket = tcpSocket.accept()
                            break
                        }
                        catch (e : SocketTimeoutException) {
                            if (currentThread().isInterrupted) {
                                exit()
                                return
                            }
                            continue
                        }
                    }
                    clientConnected.set(true)
                    clientOutputStream = clientSocket.getOutputStream()
                    var msg : String
                    while (clientConnected.get()) {
                        msg = messageQueue.take()
                        try {
                            clientOutputStream.write(msg.toByteArray())
                        } catch (e: IOException) {
                            Log.e(TAG, "client is not reachable")
                            clientConnected.set(false)
                            clientSocket.close()
                        }
                    }
                }
                exit()
                return
            } catch (e: InterruptedException) {
                //Log.i(TAG, "thread was interrupted")
                exit()
                return
            }
        }
        private fun exit() {
            if (this::clientSocket.isInitialized) {
                clientSocket.close()
            }
            tcpSocket.close()
            clientConnected.set(false)
        }
    }


    override fun stop() {
        networkThread.interrupt()
    }

    override fun isReadyToSend(): Boolean {
        return clientConnected.get()
    }

    override fun send(data: String) {
        if (!networkThread.messageQueue.offer(data)) {
            Log.e(TAG, "Can't insert data into MessageQueue")
        }
    }


}