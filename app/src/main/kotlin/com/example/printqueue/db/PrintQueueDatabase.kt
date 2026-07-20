package com.example.printqueue.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PrintJob::class],
    version = 1,
    exportSchema = false
)
abstract class PrintQueueDatabase : RoomDatabase() {
    abstract fun printJobDao(): PrintJobDao

    companion object {
        @Volatile
        private var instance: PrintQueueDatabase? = null

        fun getInstance(context: Context): PrintQueueDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrintQueueDatabase::class.java,
                    "print_queue.db"
                ).build().also { instance = it }
            }
        }
    }
}
