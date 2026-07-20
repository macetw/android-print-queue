package com.example.printqueue.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "print_jobs")
data class PrintJob(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, PRINTING, COMPLETED, FAILED
    val errorMessage: String? = null
)
