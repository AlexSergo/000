package com.example.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import kotlin.experimental.and

class FirewallService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var whiteList: Set<String> = setOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        whiteList = intent?.getStringArrayExtra("whiteList")?.toSet() ?: setOf()
        startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun startVpn() {
        val builder = Builder()
        builder.setSession("MyFirewall")
            .addAddress("192.168.0.25", 24) // Example address
            .addRoute("0.0.0.0", 0) // Route all traffic through VPN
        vpnInterface = builder.establish()

        Thread {
            try {
                vpnInterface?.fileDescriptor?.let { fd ->
                    val inputStream = FileInputStream(fd)
                    val outputStream = FileOutputStream(fd)
                    while (true) {
                        val packet = ByteArray(32767)
                        val length = inputStream.read(packet)
                        if (length > 0) {
                            // TODO: Add your packet filtering logic here
                            val destination = getDestinationAddress(packet)
                            if (destination == "0.0.160.2") {
                                outputStream.write(packet, 0, length)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("FirewallService", "Error in VPN service", e)
            }
        }.start()
    }

    private fun getDestinationAddress(packet: ByteArray): String {
        try {
            // IP-заголовок начинается с 14 байт (Ethernet заголовок)
            // Обычно IP-заголовок начинается с 14 байта
            val ipHeaderStart = 14
            // Определяем длину IP-заголовка
            val versionAndIHL = packet[ipHeaderStart]
            val ihl = (versionAndIHL and 0x0F).toInt() * 4

            // Адрес назначения начинается с байта после IP-заголовка
            val destIpStart = ipHeaderStart + ihl + 16
            val destIpBytes = packet.copyOfRange(destIpStart, destIpStart + 4)
            // Преобразуем байты в строку IP-адреса
            val destIp = destIpBytes.joinToString(".") { it.toUByte().toString() }

            return destIp
        } catch (e: Exception) {
            Log.e("FirewallService", "Error extracting destination address", e)
            return "unknown"
        }
    }

    private fun isAllowed(address: String): Boolean {
        return whiteList.any { it.equals(address, ignoreCase = true) }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            Log.e("FirewallService", "Error stopping VPN", e)
        }
        vpnInterface = null
    }
}
