package com.example.smslogger.data // Replace with your actual package name

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_log")
data class SmsMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,               // 1- ID of event: Incremental
    val smsId: Long?,               // 2- ID of SMS (from Telephony.Sms._ID, can be null if not applicable)
    val smsTimestamp: Long,         // 3- Date/Time of SMS (Unix timestamp)
    val eventTimestamp: Long,       // 4- Date/Time of event (when it was logged, Unix timestamp)
    val phoneNumber: String,        // 5- Sender/Receiver phone number
    val body: String,               // 6- Body of message
    val eventType: String,          // 7- Event Type: "RECEIVED", "SENT", "DELIVERED", etc.
    val threadId: Long?,            // 8- Thread ID from SMS provider (groups related messages)
    val dateSent: Long?,            // 9- Date/Time when SMS was sent (from sender's perspective)
    val person: String?             // 10- Contact name if phone number matches a contact
)