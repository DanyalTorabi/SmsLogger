package com.example.smslogger.data // Replace with your actual package name

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SmsMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_logger_database" // Name of your SQLite file
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // .fallbackToDestructiveMigration() // Use with caution for production
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}