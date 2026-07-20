package com.example.printqueue.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintJobDao {
    @Insert
    suspend fun insert(job: PrintJob): Long

    @Update
    suspend fun update(job: PrintJob)

    @Delete
    suspend fun delete(job: PrintJob)

    @Query("SELECT * FROM print_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<PrintJob>>

    @Query("SELECT * FROM print_jobs WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingJobs(): List<PrintJob>

    @Query("SELECT * FROM print_jobs WHERE id = :id")
    suspend fun getJobById(id: Long): PrintJob?

    @Query("DELETE FROM print_jobs WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedJobs()
}
