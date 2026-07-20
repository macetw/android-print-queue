package com.example.printqueue.config

import android.content.Context
import androidx.preference.PreferenceManager

class PrinterConfig(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var homeWifiSsid: String?
        get() = prefs.getString("home_wifi_ssid", null)
        set(value) = prefs.edit().putString("home_wifi_ssid", value).apply()

    var printerIpAddress: String?
        get() = prefs.getString("printer_ip", null)
        set(value) = prefs.edit().putString("printer_ip", value).apply()

    var printerPort: Int
        get() = prefs.getInt("printer_port", 631)
        set(value) = prefs.edit().putInt("printer_port", value).apply()

    var printerName: String?
        get() = prefs.getString("printer_name", null)
        set(value) = prefs.edit().putString("printer_name", value).apply()

    fun isConfigured(): Boolean {
        return !homeWifiSsid.isNullOrEmpty() && !printerIpAddress.isNullOrEmpty()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
