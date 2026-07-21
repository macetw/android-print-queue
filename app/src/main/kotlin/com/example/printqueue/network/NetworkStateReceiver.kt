package com.example.printqueue.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.printqueue.config.PrinterConfig
import com.example.printqueue.db.PrintQueueDatabase
import com.example.printqueue.print.IppPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkStateReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d("NetworkStateReceiver", "Network state changed: ${intent.action}")

    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
      val config = PrinterConfig(context)
      val db = PrintQueueDatabase.getInstance(context)

      // Check if we're on home network now
      if (NetworkUtils.isOnHomeNetwork(context, config)) {
        Log.d("NetworkStateReceiver", "Connected to home network, checking printer")

        val printerIp = config.printerIpAddress ?: return@launch
        val printerPort = config.printerPort

        // Check if printer is online
        if (NetworkUtils.isPrinterOnline(printerIp, printerPort)) {
          Log.d("NetworkStateReceiver", "Printer is online, processing queue")

          // Process pending jobs
          val pendingJobs = db.printJobDao().getPendingJobs()
          if (pendingJobs.isNotEmpty()) {
            val printer = IppPrinter(printerIp, printerPort)

            for (job in pendingJobs) {
              try {
                val success = printer.printFile(job.filePath, job.fileName)
                val updated = job.copy(
                  status = if (success) "COMPLETED" else "FAILED",
                  errorMessage = if (success) null else "Print failed"
                )
                db.printJobDao().update(updated)
                Log.d("NetworkStateReceiver", "Processed job ${job.id}: ${updated.status}")
              } catch (e: Exception) {
                Log.e("NetworkStateReceiver", "Error processing job", e)
                val updated = job.copy(status = "FAILED", errorMessage = e.message)
                db.printJobDao().update(updated)
              }
            }
          }
        }
      }
    }
  }
}
