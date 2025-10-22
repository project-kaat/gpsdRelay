package io.github.project_kaat.gpsdrelay.network

import android.util.Log
import io.github.project_kaat.gpsdrelay.database.Server
import io.github.project_kaat.gpsdrelay.nmeaServerService
import io.github.project_kaat.gpsdrelay.ui.BroadcastNetworkAddressElement
import java.io.IOException
import java.net.*
import java.util.concurrent.ArrayBlockingQueue

class udpSocketServer(private val servers : List<Server>, private val service : nmeaServerService) :
    SocketServerInterface {
    private val TAG = "udpSocketServer"


    private lateinit var networkThread : NetworkThread

    override fun isConnected() = true

    private fun resolveServerAddresses() : List<Server> {
        val resolved = mutableListOf<Server>()
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (server in servers) {
            if (server.ipv4.startsWith("BCAST:")) {
                val interfaceName = server.ipv4.substringAfter("BCAST:")
                for (iface in interfaces) {
                    if (iface.displayName == interfaceName) {
                        for (interfaceAddress in iface.interfaceAddresses) {
                            val broadcast = interfaceAddress.broadcast
                            if (broadcast != null) {
                                resolved += server.copy(ipv4 = broadcast.toString().substring(1)) //remove slash in the beginning
                                Log.d("resolveServerAddress", "${server.ipv4} resolved to ${broadcast}")
                            }
                        }
                    }
                }
            }
            else {
                resolved += server
            }
        }
        return resolved
    }


    override fun start() {
        val runtimeServers = resolveServerAddresses()
        networkThread = NetworkThread(
            runtimeServers.filter {
                it.generationEnabled
            },
            runtimeServers.filter {
                it.relayingEnabled
            })
        networkThread.start()
    }

    private inner class NetworkThread(val generatedMsgSubscribers : List<Server>, val relayedMsgSubscribers : List<Server>) : Thread() {
        private val TAG = "udpSocketServer.NetworkThread"
        val messageQueue : ArrayBlockingQueue<OutgoingMessage> = ArrayBlockingQueue(30)
        private lateinit var udpSocket : DatagramSocket

        override fun run() {
            try {
                udpSocket = DatagramSocket(null)
                udpSocket.reuseAddress = true
                udpSocket.bind(InetSocketAddress(0))
                while (!currentThread().isInterrupted && !udpSocket.isClosed) {
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
                    } catch (_: Exception) {
                        Log.e(TAG, "client is not reachable")
                        //TODO: when client becomes reachable, nothing much changes FIXED. now implement and test with tcp server
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
            service.onServerStop(this@udpSocketServer)
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