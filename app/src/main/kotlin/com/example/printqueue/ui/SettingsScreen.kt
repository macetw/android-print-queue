package com.example.printqueue.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printqueue.config.PrinterConfig
import com.example.printqueue.network.NetworkUtils
import com.example.printqueue.network.PrinterDiscovery
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
  val context = LocalContext.current
  val config = PrinterConfig(context)
  val scope = rememberCoroutineScope()

  val homeWifiSsid = remember { mutableStateOf(config.homeWifiSsid ?: "") }
  val printerIp = remember { mutableStateOf(config.printerIpAddress ?: "") }
  val printerPort = remember { mutableStateOf(config.printerPort.toString()) }
  val printerName = remember { mutableStateOf(config.printerName ?: "") }
  val discovering = remember { mutableStateOf(false) }
  val discoveredPrinters = remember { mutableStateOf(emptyList<Pair<String, String>>()) }
  val currentWifi = remember { mutableStateOf(NetworkUtils.getCurrentWifiSsid(context) ?: "Not connected") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      "Settings",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    // Current WiFi
    Text("Current WiFi: ${currentWifi.value}", fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))

    // Home WiFi SSID
    Text("Home WiFi SSID", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    OutlinedTextField(
      value = homeWifiSsid.value,
      onValueChange = { homeWifiSsid.value = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("WiFi SSID") }
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Printer IP
    Text("Printer IP Address", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    OutlinedTextField(
      value = printerIp.value,
      onValueChange = { printerIp.value = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("e.g., 192.168.1.100") }
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Printer Port
    Text("Printer Port", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    OutlinedTextField(
      value = printerPort.value,
      onValueChange = { printerPort.value = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Default: 631") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Printer Name
    Text("Printer Name", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    OutlinedTextField(
      value = printerName.value,
      onValueChange = { printerName.value = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("e.g., Brother Printer") }
    )
    Spacer(modifier = Modifier.height(24.dp))

    // Discover Button
    Button(
      onClick = {
        discovering.value = true
        scope.launch {
          try {
            val printers = PrinterDiscovery.discoverPrintersOnNetwork()
            discoveredPrinters.value = printers.map { it.name to it.ipAddress }
          } finally {
            discovering.value = false
          }
        }
      },
      modifier = Modifier.fillMaxWidth(),
      enabled = !discovering.value
    ) {
      Text(if (discovering.value) "Discovering..." else "Discover Printers")
    }

    if (discoveredPrinters.value.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      Text("Discovered Printers:", fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))
      discoveredPrinters.value.forEach { (name, ip) ->
        Button(
          onClick = {
            printerIp.value = ip
            printerName.value = name
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("$name ($ip)")
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Save Button
    Button(
      onClick = {
        config.homeWifiSsid = homeWifiSsid.value
        config.printerIpAddress = printerIp.value
        config.printerPort = printerPort.value.toIntOrNull() ?: 631
        config.printerName = printerName.value
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Save Configuration")
    }
  }
}
