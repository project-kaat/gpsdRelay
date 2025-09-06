package io.github.project_kaat.gpsdrelay.network

import android.util.Log
import io.github.project_kaat.gpsdrelay.database.Server
import java.io.IOException
import java.net.*
import java.util.concurrent.ArrayBlockingQueue

class udpSocketServer(servers : List<Server>) :
    SocketServerInterface {
    private val TAG = "udpSocketServer"

    private val generatedMsgSubscribers = servers.filter{
        it.generationEnabled
    }
    private val relayedMsgSubscribers = servers.filter{
        it.relayingEnabled
    }

    private lateinit var networkThread : NetworkThread

    override fun isConnected() = true

    override fun start() {
        networkThread = NetworkThread()
        networkThread.start()
    }

    inner class NetworkThread() : Thread() {
        private val TAG = "udpSocketServer.NetworkThread"
        val messageQueue : ArrayBlockingQueue<OutgoingMessage> = ArrayBlockingQueue(30)
        private lateinit var udpSocket : DatagramSocket

        override fun run() {
            try {
                udpSocket = DatagramSocket(null)
                udpSocket.reuseAddress = true
                udpSocket.bind(InetSocketAddress(0))
                while (!currentThread().isInterrupted) {
                    val msg = messageQueue.take()
                    try {
                        if (msg.isGenerated) {
                            generatedMsgSubscribers.forEach {
                                udpSocket.send(
                                    DatagramPacket(
                                        msg.data.toByteArray(), msg.data.length,
                                        InetSocketAddress(it.ipv4, it.port)
                                    )
                                )
                            }
                        }
                        else {
                            relayedMsgSubscribers.forEach {
                                if (isMessageAllowedByFilter(msg, it.relayFilter)) {
                                    udpSocket.send(
                                        DatagramPacket(
                                            msg.data.toByteArray(), msg.data.length,
                                            InetSocketAddress(it.ipv4, it.port)
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: IOException) {
                        Log.e(TAG, "client is not reachable")
                        exit()
                    }
                }
                exit()
            }
            catch (_ : InterruptedException) {
                //Log.i(TAG, "thread was interrupted")
                exit()
            }
        }

        private fun exit() {
            //Log.i(TAG, "closing the socket")
            udpSocket.disconnect()
            udpSocket.close()
        }

    }

    override fun send(message : OutgoingMessage){
        if (!networkThread.messageQueue.offer(message)) {
            Log.e(TAG, "Can't insert data into MessageQueue")
        }
    }

    override fun stop() {
        networkThread.interrupt()
    }
}