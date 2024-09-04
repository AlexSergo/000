package com.example.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
            return when (packet[0] and 0xF0.toByte()) {
                0x40.toByte() -> {
                    // определить длину заголовка
                    val headerLength = (packet[0] and 0x0F).toInt() * 4

                    // Адрес назначения - последние 4 байта заголовка
                    val destIpBytes = packet.copyOfRange(headerLength - 4, headerLength)

                    // Преобразуем байты в строку IP-адреса
                    destIpBytes.joinToString(".") { it.toUByte().toString() }
                }

                0x60.toByte() -> {
                    // определить длину заголовка
                    val headerLength = packet[6].toInt()

                    // Извлекаем адрес назначения из пакета
                    val destinationAddress = packet.copyOfRange(8 + headerLength, 24 + headerLength)

                    // Преобразуем адрес в строковый формат
                    destinationAddress.joinToString(":") { String.format("%02X", it) }
                }

                else -> "unknown"
            }
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