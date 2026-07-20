package com.example.printqueue.print

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class IppPrinter(
    val ipAddress: String,
    val port: Int = 631
) {
    private val client = OkHttpClient()
    private val baseUrl = "http://$ipAddress:$port"

    suspend fun printFile(filePath: String, fileName: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val fileContent = file.readBytes()

            // Simple IPP print request
            // This is a basic implementation - full IPP protocol is more complex
            val request = Request.Builder()
                .url("$baseUrl/ipp/print")
                .post(fileContent.toRequestBody("application/pdf".toMediaType()))
                .header("Content-Type", "application/pdf")
                .header("User-Agent", "PrintQueue/1.0")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPrinterStatus(): PrinterStatus {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                PrinterStatus.ONLINE
            } else {
                PrinterStatus.OFFLINE
            }
        } catch (e: Exception) {
            PrinterStatus.OFFLINE
        }
    }
}

enum class PrinterStatus {
    ONLINE, OFFLINE, ERROR
}
