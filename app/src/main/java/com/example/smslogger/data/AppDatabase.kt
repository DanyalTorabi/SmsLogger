package com.example.smslogger.data // Replace with your actual package name

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SmsMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new columns to the existing table
                database.execSQL("ALTER TABLE sms_log ADD COLUMN threadId INTEGER")
                database.execSQL("ALTER TABLE sms_log ADD COLUMN dateSent INTEGER")
                database.execSQL("ALTER TABLE sms_log ADD COLUMN person TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_logger_database" // Name of your SQLite file
                )
                    .addMigrations(MIGRATION_1_2)
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // .fallbackToDestructiveMigration() // Use with caution for production
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}