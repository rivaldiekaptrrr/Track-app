package com.trackit.app.util

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

object ContactUtils {
    /**
     * Fetches contacts from the device, filtering out duplicates based on phone numbers.
     * Requires READ_CONTACTS permission.
     */
    suspend fun getDeviceContacts(context: Context): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<DeviceContact>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: continue
                val number = it.getString(numberIndex) ?: continue
                
                // Basic cleanup of phone number (remove spaces, dashes)
                val cleanNumber = number.replace(Regex("[\\s\\-\\(\\)]"), "")
                
                if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                    contacts.add(DeviceContact(name = name, phoneNumber = cleanNumber))
                }
            }
        }
        
        contacts
    }
}
