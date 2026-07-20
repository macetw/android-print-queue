package com.example.printqueue.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

data class DiscoveredPrinter(
    val name: String,
    val ipAddress: String,
    val port: Int = 631
)

object PrinterDiscovery {
    suspend fun discoverPrintersOnNetwork(baseIp: String = "192.168.1"): List<DiscoveredPrinter> {
        return withContext(Dispatchers.Default) {
            val printers = mutableListOf<DiscoveredPrinter>()

            // Scan common ports and IP range in parallel
            val jobs = (1..254).map { i ->
                async(Dispatchers.IO) {
                    val ip = "$baseIp.$i"
                    checkIpForPrinter(ip)
                }
            }

            val results = jobs.awaitAll()
            printers.addAll(results.filterNotNull())
            printers
        }
    }

    private suspend fun checkIpForPrinter(ip: String): DiscoveredPrinter? {
        return withContext(Dispatchers.IO) {
            // Check port 631 (CUPS/IPP standard)
            if (isPortOpen(ip, 631)) {
                return@withContext DiscoveredPrinter(
                    name = "Printer at $ip",
                    ipAddress = ip,
                    port = 631
                )
            }

            // Check port 9100 (JetDirect)
            if (isPortOpen(ip, 9100)) {
                return@withContext DiscoveredPrinter(
                    name = "Printer at $ip",
                    ipAddress = ip,
                    port = 9100
                )
            }

            null
        }
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetAddress.getByName(ip) to port, 500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

private infix fun InetAddress.to(port: Int) = java.net.InetSocketAddress(this, port)
