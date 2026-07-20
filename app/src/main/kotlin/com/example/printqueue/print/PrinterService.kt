package com.example.printqueue.print

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrinterDiscoverySession
import android.print.PrinterInfo
import android.print.PrinterId
import android.print.PrinterCapabilitiesInfo
import android.printservice.PrintService
import android.printservice.PrintJob
import android.util.Log
import com.example.printqueue.config.PrinterConfig
import com.example.printqueue.db.PrintQueueDatabase
import com.example.printqueue.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

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

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        return QueuePrinterDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        scope.launch {
            try {
                val printerInfo = printJob.info
                val printAttributes = printerInfo.attributes

                // Save print job to database
                val jobData = com.example.printqueue.db.PrintJob(
                    fileName = printerInfo.name,
                    filePath = savePrintJobFile(printJob),
                    mimeType = "application/pdf"
                )

                db.printJobDao().insert(jobData)
                Log.d(tag, "Print job saved: ${jobData.fileName}")

                // Try to process queue immediately
                processQueueIfReady()
            } catch (e: Exception) {
                Log.e(tag, "Error queuing print job", e)
                printJob.fail("Failed to queue print job")
            }
        }
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        Log.d(tag, "Print job cancelled: ${printJob.info.name}")
        printJob.cancel()
    }

    private suspend fun processQueueIfReady() {
        val printerIp = config.printerIpAddress ?: return
        val printerPort = config.printerPort

        // Check conditions
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
                    job.status = if (success) "COMPLETED" else "FAILED"
                    job.errorMessage = if (success) null else "Print failed"
                    db.printJobDao().update(job)
                    Log.d(tag, "Job ${job.id} status: ${job.status}")
                }
            }
        }
    }

    private fun savePrintJobFile(printJob: PrintJob): String {
        val jobDir = File(filesDir, "print_jobs")
        jobDir.mkdirs()

        val jobFile = File(jobDir, "job_${System.currentTimeMillis()}.pdf")

        val input = contentResolver.openInputStream(printJob.info.uri)
        jobFile.outputStream().use { output ->
            input?.copyTo(output)
        }

        return jobFile.absolutePath
    }

    private inner class QueuePrinterDiscoverySession(private val service: PrinterService) :
        PrinterDiscoverySession() {

        override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) {
            Log.d(tag, "Printer discovery started")

            val printerId = PrinterId.Builder(service, "print_queue", false).build()
            val printerInfo = PrinterInfo.Builder(printerId, "Print Queue", PrinterInfo.PRINTER_STATUS_IDLE)
                .setDescription("Local print queue")
                .setCapabilities(getCapabilities())
                .build()

            addPrinters(listOf(printerInfo))
        }

        override fun onStopPrinterDiscovery() {
            Log.d(tag, "Printer discovery stopped")
        }

        override fun onValidatePrinters(printerIds: MutableList<PrinterId>) {
            // Validate printers
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

        private fun getCapabilities(): PrinterCapabilitiesInfo {
            return PrinterCapabilitiesInfo.Builder(PrinterId.Builder(service, "print_queue", false).build())
                .addMediaSize(PrintAttributes.MediaSize.ISO_A4, true)
                .addResolution(PrintAttributes.Resolution("300x300", "300x300", 300, 300), true)
                .setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME or PrintAttributes.COLOR_MODE_COLOR, PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        }
    }
}
