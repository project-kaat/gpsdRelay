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

    override fun start() {
        val src : InetSocketAddress
        try {
            src = InetSocketAddress(ipv4AddressSrc, ipv4PortSrc.toInt())
        }
        catch (e : UnknownHostException) {
            Log.e(TAG, "Invalid ip AND/OR port supplied")
            return
        }
        networkThread = NetworkThread(src)
        networkThread.start()
    }


    private class NetworkThread(private val srcAddress : SocketAddress) : Thread() {
        private val TAG = "tcpSocketServer.NetworkThread"
        val messageQueue: ArrayBlockingQueue<String> = ArrayBlockingQueue(30)
        private lateinit var tcpSocketIPv4: ServerSocket

        override fun run() {
            try {
                val inetAddressIPv4 = InetAddress.getByName((srcAddress as InetSocketAddress).hostString)
                val ipv4SocketAddress = InetSocketAddress(inetAddressIPv4, (srcAddress).port)
                tcpSocketIPv4 = ServerSocket()
                tcpSocketIPv4.reuseAddress = true
                tcpSocketIPv4.bind(ipv4SocketAddress)
                tcpSocketIPv4.soTimeout = 500

                acceptConnections(tcpSocketIPv4)

                exit()
                return
            } catch (e: InterruptedException) {
                    exit()
                    return
            } finally {
                exit()
            }
        }

        private fun acceptConnections(tcpSocket: ServerSocket) {
            while (!currentThread().isInterrupted) {
                try {
                    val clientSocket = tcpSocket.accept()
                    val clientThread = Thread {
                        handleClientConnection(clientSocket)
                    }
                    clientThread.start()
                } catch (e: SocketTimeoutException) {
                    if (currentThread().isInterrupted) {
                        return
                    }
                }
            }
        }

        private fun handleClientConnection(clientSocket: Socket) {
            val clientConnected = AtomicBoolean(true)
            var clientOutputStream: OutputStream? = null
            try {
                clientOutputStream = clientSocket.getOutputStream()
                var msg : String
                while (clientConnected.get()) {
                    msg = messageQueue.take()
                    try {
                        clientOutputStream?.write(msg.toByteArray())
                        } catch (e: IOException) {
                            Log.e(TAG, "client is not reachable")
                            clientConnected.set(false)
                            clientOutputStream?.close()
                            clientSocket.close()
                            return
                        }
                    }
                } catch (e: InterruptedException) {
                //Log.i(TAG, "thread was interrupted")
                    try {
                        clientOutputStream?.close()
                        clientSocket.close()
                        clientConnected.set(false)
                        return
                    } catch (e: IOException) {
                        Log.e(TAG, "Error closing client socket")
                    } finally {
                        clientOutputStream?.close()
                        clientSocket.close()
                        clientConnected.set(false)
                    }
            }
        }
        private fun exit() {
            if (this::tcpSocketIPv4.isInitialized) {
                tcpSocketIPv4.close()
            }
        }
    }

    override fun stop() {
        networkThread.interrupt()
    }

    override fun isReadyToSend(): Boolean {
        return true
    }

    override fun send(data: String) {
        if (!networkThread.messageQueue.offer(data)) {
            Log.e(TAG, "Can't insert data into MessageQueue")
        }
    }


}