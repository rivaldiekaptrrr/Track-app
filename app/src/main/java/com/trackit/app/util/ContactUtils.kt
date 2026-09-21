package com.trackit.app.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

object ContactUtils {
    /**
     * Extracts contact name and phone number from the Uri returned by ActivityResultContracts.PickContact().
     * DOES NOT REQUIRE READ_CONTACTS permission (uses Android System Contact Picker with temporary URI permission).
     */
    suspend fun getContactFromUri(context: Context, contactUri: Uri): DeviceContact? = withContext(Dispatchers.IO) {
        try {
            var name: String? = null
            var contactId: String? = null

            // 1. Get Contact Name and ID
            context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    if (idIndex != -1) {
                        contactId = cursor.getString(idIndex)
                    }
                }
            }

            if (name == null) return@withContext null

            // 2. Get Phone Number using Contact ID
            var phoneNumber = ""
            if (contactId != null) {
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIndex != -1) {
                            val rawNumber = cursor.getString(numberIndex) ?: ""
                            phoneNumber = rawNumber.replace(Regex("[\\s\\-\\(\\)]"), "")
                        }
                    }
                }
            }

            DeviceContact(name = name ?: "Tamu", phoneNumber = phoneNumber)
        } catch (e: Exception) {
            null
        }
    }
}
