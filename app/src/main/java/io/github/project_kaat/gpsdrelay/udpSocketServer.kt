package io.github.project_kaat.gpsdrelay

import android.util.Log
import java.io.IOException
import java.net.*
import java.util.concurrent.ArrayBlockingQueue

class udpSocketServer(private val ipv4AddressSRC : String, private val ipv4PortSRC : String, private val ipv4AddressDST : String, private val ipv4PortDST : String) :
    SocketServerInterface {
    private val TAG = "udpSocketServer"

    private lateinit var networkThread : NetworkThread

    override fun isReadyToSend(): Boolean {
        if (this::networkThread.isInitialized) {
            return networkThread.isAlive
        }
        else {
            return false
        }
    }

    override fun start() {
        val src : InetSocketAddress
        val dst : InetSocketAddress
        try {
            src = InetSocketAddress(ipv4AddressSRC, ipv4PortSRC.toInt())
            dst = InetSocketAddress(ipv4AddressDST, ipv4PortDST.toInt())
        }
        catch (e : UnknownHostException) {
            Log.e(TAG, "Invalid ip AND/OR port supplied")
            return
        }
        networkThread = NetworkThread(src, dst)
        networkThread.start()
    }

    private class NetworkThread(private val srcAddress : SocketAddress, private val dstAddress : InetSocketAddress) : Thread() {
        private val TAG = "udpSocketServer.NetworkThread"
        val messageQueue : ArrayBlockingQueue<String> = ArrayBlockingQueue(30)
        private lateinit var udpSocket : DatagramSocket

        override fun run() {
            try {
                udpSocket = DatagramSocket(null)
                udpSocket.reuseAddress = true
                udpSocket.bind(srcAddress)
                udpSocket.connect(dstAddress)
                var msg: String
                while (!currentThread().isInterrupted) {
                    msg = messageQueue.take()
                    try {
                        udpSocket.send(DatagramPacket(msg.toByteArray(), msg.length))
                    } catch (e: IOException) {
                        Log.e(TAG, "client is not reachable")
                        exit()
                    }
                }
                exit()
            }
            catch (e : InterruptedException) {
                Log.e(TAG, "thread was interrupted")
                exit()
            }
        }

        private fun exit() {
            //Log.i(TAG, "closing the socket")
            udpSocket.disconnect()
            udpSocket.close()
        }

    }

    override fun send(data : String){
        if (!networkThread.messageQueue.offer(data)) {
            Log.e(TAG, "Can't insert data into MessageQueue")
        }
    }

    override fun stop() {
        networkThread.interrupt()
    }
}