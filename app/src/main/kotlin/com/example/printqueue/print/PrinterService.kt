package com.example.printqueue.print

import android.print.PrintAttributes
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrintJob
import android.printservice.PrintService
import android.util.Log
import com.example.printqueue.config.PrinterConfig
import com.example.printqueue.db.PrintQueueDatabase
import com.example.printqueue.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PrinterService : PrintService() {
  private val tag = "PrinterService"
  private val scope = CoroutineScope(Dispatchers.Main)
  private lateinit var config: PrinterConfig
  private lateinit var db: PrintQueueDatabase

  override fun onCreate() {
    super.onCreate()
    config = PrinterConfig(this)
    db = PrintQueueDatabase.getInstance(this)
    Log.d(tag, "PrinterService created")
  }

  override fun onCreatePrinterDiscoverySession(): android.printservice.PrinterDiscoverySession {
    return object : android.printservice.PrinterDiscoverySession() {
      override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) {
        Log.d(tag, "Printer discovery started")
        val service = this@PrinterService
        val printerId = android.print.PrinterId.Builder(service, "print_queue", false).build()
        val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
          .addMediaSize(PrintAttributes.MediaSize.ISO_A4, true)
          .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
          .build()

        val printerInfo = PrinterInfo.Builder(printerId, "Print Queue", 0)
          .setDescription("Local print queue")
          .setCapabilities(capabilities)
          .build()

        addPrinters(listOf(printerInfo))
      }

      override fun onStopPrinterDiscovery() {
        Log.d(tag, "Printer discovery stopped")
      }

      override fun onValidatePrinters(printerIds: MutableList<PrinterId>) {
        Log.d(tag, "Validating printers")
      }

      override fun onStartPrinterStateTracking(printerId: PrinterId) {
        Log.d(tag, "Printer state tracking started")
      }

      override fun onStopPrinterStateTracking(printerId: PrinterId) {
        Log.d(tag, "Printer state tracking stopped")
      }

      override fun onDestroy() {
        Log.d(tag, "Discovery session destroyed")
      }
    }
  }

  override fun onPrintJobQueued(printJob: PrintJob) {
    scope.launch {
      try {
        val jobName = printJob.info.label ?: "Print Job"

        // Save print job to database
        val jobData = com.example.printqueue.db.PrintJob(
          fileName = jobName,
          filePath = savePrintJobFile(printJob),
          mimeType = "application/pdf"
        )

        db.printJobDao().insert(jobData)
        Log.d(tag, "Print job saved: $jobName")
        printJob.complete()

        // Try to process queue immediately
        processQueueIfReady()
      } catch (e: Exception) {
        Log.e(tag, "Error queuing print job", e)
        printJob.fail("Failed to queue print job")
      }
    }
  }

  override fun onRequestCancelPrintJob(printJob: PrintJob) {
    Log.d(tag, "Print job cancelled")
    printJob.cancel()
  }

  private suspend fun processQueueIfReady() {
    val printerIp = config.printerIpAddress ?: return
    val printerPort = config.printerPort

    val onHomeNetwork = NetworkUtils.isOnHomeNetwork(this, config)
    val printerOnline = NetworkUtils.isPrinterOnline(printerIp, printerPort)

    Log.d(tag, "Queue check: onHomeNetwork=$onHomeNetwork, printerOnline=$printerOnline")

    if (onHomeNetwork && printerOnline) {
      val pendingJobs = db.printJobDao().getPendingJobs()
      if (pendingJobs.isNotEmpty()) {
        Log.d(tag, "Processing ${pendingJobs.size} pending jobs")
        val printer = IppPrinter(printerIp, printerPort)

        for (job in pendingJobs) {
          val success = printer.printFile(job.filePath, job.fileName)
          val updated = job.copy(
            status = if (success) "COMPLETED" else "FAILED",
            errorMessage = if (success) null else "Print failed"
          )
          db.printJobDao().update(updated)
          Log.d(tag, "Job ${job.id} status: ${updated.status}")
        }
      }
    }
  }

  private fun savePrintJobFile(printJob: PrintJob): String {
    val jobDir = File(filesDir, "print_jobs")
    jobDir.mkdirs()

    val jobFile = File(jobDir, "job_${System.currentTimeMillis()}.pdf")

    try {
      val document = printJob.document
      if (document != null) {
        val input = contentResolver.openInputStream(document.uri)
        if (input != null) {
          jobFile.outputStream().use { output ->
            input.copyTo(output)
          }
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error saving print job file", e)
      // Create empty placeholder file if document access fails
      jobFile.createNewFile()
    }

    return jobFile.absolutePath
  }
}
