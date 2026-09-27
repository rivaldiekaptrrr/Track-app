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
            var phoneNumber = ""

            // 1. Query the picked contact URI directly for Display Name and direct phone if available
            context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }

                    val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (phoneIndex != -1) {
                        val rawNumber = cursor.getString(phoneIndex) ?: ""
                        if (rawNumber.isNotBlank()) {
                            phoneNumber = rawNumber.replace(Regex("[\\s\\-\\(\\)]"), "")
                        }
                    }
                }
            }

            // 2. If phone number wasn't directly in the contact cursor, query the Data directory
            // using the appended contact Uri (which inherits the temporary permission grant from PickContact)
            if (phoneNumber.isBlank()) {
                val dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
                val phoneProjection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.Data.MIMETYPE
                )
                val phoneSelection = "${ContactsContract.Data.MIMETYPE} = ?"
                val phoneSelectionArgs = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)

                context.contentResolver.query(
                    dataUri,
                    phoneProjection,
                    phoneSelection,
                    phoneSelectionArgs,
                    null
                )?.use { dataCursor ->
                    if (dataCursor.moveToFirst()) {
                        val numberIndex = dataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIndex != -1) {
                            val rawNumber = dataCursor.getString(numberIndex) ?: ""
                            phoneNumber = rawNumber.replace(Regex("[\\s\\-\\(\\)]"), "")
                        }
                    }
                }
            }

            if (name == null && phoneNumber.isBlank()) {
                return@withContext null
            }

            DeviceContact(
                name = name?.takeIf { it.isNotBlank() } ?: "Tamu Kontak",
                phoneNumber = phoneNumber
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads all device contacts with phone numbers for in-app bulk importing.
     * Requires READ_CONTACTS runtime permission.
     */
    suspend fun getAllContacts(context: Context): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<DeviceContact>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else ""
                    val rawNumber = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""
                    val cleanNumber = rawNumber.replace(Regex("[\\s\\-\\(\\)]"), "")

                    if (name.isNotBlank() && cleanNumber.isNotBlank() && cleanNumber !in seenNumbers) {
                        seenNumbers.add(cleanNumber)
                        contactsList.add(
                            DeviceContact(
                                name = name.trim(),
                                phoneNumber = cleanNumber
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Handle SecurityException or other provider errors
        }

        contactsList
    }
}
