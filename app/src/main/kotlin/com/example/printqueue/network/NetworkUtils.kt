package com.example.printqueue.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.example.printqueue.config.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

object NetworkUtils {
    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getCurrentWifiSsid(context: Context): String? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo ?: return null
        val ssid = connectionInfo.ssid
        return if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid.substring(1, ssid.length - 1)
        } else {
            ssid
        }
    }

    fun isOnHomeNetwork(context: Context, config: PrinterConfig): Boolean {
        val homeSsid = config.homeWifiSsid ?: return false
        val currentSsid = getCurrentWifiSsid(context) ?: return false
        return currentSsid == homeSsid && isConnectedToWifi(context)
    }

    suspend fun isPrinterOnline(printerIp: String, printerPort: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetAddress.getByName(printerIp) to printerPort, 3000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

private infix fun InetAddress.to(port: Int) = java.net.InetSocketAddress(this, port)
