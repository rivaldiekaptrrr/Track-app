package com.trackit.app.util

import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import org.json.JSONObject

/**
 * Converts Kotlin data entities to Firestore REST API JSON format and vice versa.
 *
 * Firestore REST API requires all field values to be typed, e.g.:
 *   "amount": { "doubleValue": 100.0 }
 *   "name":   { "stringValue": "test" }
 *   "id":     { "integerValue": "123" }
 *   "flag":   { "booleanValue": true }
 *   "nullField": { "nullValue": null }
 */
object FirestoreMapper {

    // ======================= TRANSACTION =======================

    fun TransactionEntity.toFirestoreJson(): String {
        val fields = JSONObject()

        fun putString(key: String, value: String?) {
            val v = JSONObject()
            if (value == null) v.put("nullValue", JSONObject.NULL) else v.put("stringValue", value)
            fields.put(key, v)
        }
        fun putLong(key: String, value: Long?) {
            val v = JSONObject()
            if (value == null) v.put("nullValue", JSONObject.NULL) else v.put("integerValue", value.toString())
            fields.put(key, v)
        }
        fun putInt(key: String, value: Int?) {
            val v = JSONObject()
            if (value == null) v.put("nullValue", JSONObject.NULL) else v.put("integerValue", value.toString())
            fields.put(key, v)
        }
        fun putDouble(key: String, value: Double) {
            fields.put(key, JSONObject().put("doubleValue", value))
        }
        fun putBool(key: String, value: Boolean) {
            fields.put(key, JSONObject().put("booleanValue", value))
        }

        putLong("id", id)
        putDouble("amount", amount)
        putString("description", description)
        putLong("categoryId", categoryId)
        putLong("date", date)
        putLong("createdAt", createdAt)
        putBool("isRecurring", isRecurring)
        putString("recurringType", recurringType)
        putInt("recurringDayOfMonth", recurringDayOfMonth)
        putString("type", type)
        putLong("profileId", profileId)

        return JSONObject().put("fields", fields).toString()
    }

    /**
     * Parses a raw Firestore REST document JSONObject (from listDocuments response)
     * into a TransactionEntity.
     */
    fun JSONObject.toTransactionEntity(): TransactionEntity? {
        return try {
            val fields = getJSONObject("fields")

            fun strOrNull(key: String): String? {
                if (!fields.has(key)) return null
                val f = fields.getJSONObject(key)
                return if (f.has("stringValue")) f.getString("stringValue") else null
            }
            fun longOrNull(key: String): Long? {
                if (!fields.has(key)) return null
                val f = fields.getJSONObject(key)
                return if (f.has("integerValue")) f.getString("integerValue").toLongOrNull() else null
            }
            fun intOrNull(key: String): Int? {
                if (!fields.has(key)) return null
                val f = fields.getJSONObject(key)
                return if (f.has("integerValue")) f.getString("integerValue").toIntOrNull() else null
            }
            fun doubleOrZero(key: String): Double {
                if (!fields.has(key)) return 0.0
                val f = fields.getJSONObject(key)
                return if (f.has("doubleValue")) f.getDouble("doubleValue") else 0.0
            }
            fun boolOrFalse(key: String): Boolean {
                if (!fields.has(key)) return false
                val f = fields.getJSONObject(key)
                return if (f.has("booleanValue")) f.getBoolean("booleanValue") else false
            }

            TransactionEntity(
                id = 0, // Room will auto-generate local ID
                amount = doubleOrZero("amount"),
                description = strOrNull("description") ?: "",
                categoryId = longOrNull("categoryId"),
                date = longOrNull("date") ?: System.currentTimeMillis(),
                createdAt = longOrNull("createdAt") ?: System.currentTimeMillis(),
                isRecurring = boolOrFalse("isRecurring"),
                recurringType = strOrNull("recurringType"),
                recurringDayOfMonth = intOrNull("recurringDayOfMonth"),
                type = strOrNull("type") ?: "EXPENSE",
                profileId = longOrNull("profileId") ?: 1L
            )
        } catch (e: Exception) {
            null
        }
    }

    // ======================= WEDDING EXPENSE =======================

    fun WeddingExpenseEntity.toFirestoreJson(): String {
        val fields = JSONObject()

        fields.put("expenseId", JSONObject().put("stringValue", expenseId))
        fields.put("weddingProfileId", JSONObject().put("stringValue", weddingProfileId))
        fields.put("category", JSONObject().put("stringValue", category))
        fields.put("title", JSONObject().put("stringValue", title))
        fields.put("totalEstimated", JSONObject().put("doubleValue", totalEstimated))
        fields.put("totalPaid", JSONObject().put("doubleValue", totalPaid))
        fields.put("paidBySource", JSONObject().put("stringValue", paidBySource))
        fields.put("paymentStatus", JSONObject().put("stringValue", paymentStatus))
        fields.put("createdAt", JSONObject().put("integerValue", createdAt.toString()))
        if (notes != null) {
            fields.put("notes", JSONObject().put("stringValue", notes))
        } else {
            fields.put("notes", JSONObject().put("nullValue", JSONObject.NULL))
        }

        return JSONObject().put("fields", fields).toString()
    }

    fun JSONObject.toWeddingExpenseEntity(): WeddingExpenseEntity? {
        return try {
            val fields = getJSONObject("fields")
            WeddingExpenseEntity(
                expenseId = fields.getJSONObject("expenseId").getString("stringValue"),
                weddingProfileId = fields.getJSONObject("weddingProfileId").getString("stringValue"),
                category = fields.getJSONObject("category").getString("stringValue"),
                title = fields.getJSONObject("title").getString("stringValue"),
                totalEstimated = fields.getJSONObject("totalEstimated").getDouble("doubleValue"),
                totalPaid = if (fields.has("totalPaid")) fields.getJSONObject("totalPaid").getDouble("doubleValue") else 0.0,
                paidBySource = if (fields.has("paidBySource")) fields.getJSONObject("paidBySource").getString("stringValue") else "BERSAMA",
                paymentStatus = if (fields.has("paymentStatus")) fields.getJSONObject("paymentStatus").getString("stringValue") else "UNPAID",
                notes = if (fields.has("notes") && fields.getJSONObject("notes").has("stringValue")) fields.getJSONObject("notes").getString("stringValue") else null,
                createdAt = if (fields.has("createdAt")) fields.getJSONObject("createdAt").getString("integerValue").toLong() else System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    // ======================= WEDDING TASK =======================

    fun WeddingTaskEntity.toFirestoreJson(): String {
        val fields = JSONObject()

        fields.put("taskId", JSONObject().put("stringValue", taskId))
        fields.put("weddingProfileId", JSONObject().put("stringValue", weddingProfileId))
        fields.put("phaseMonth", JSONObject().put("integerValue", phaseMonth.toString()))
        fields.put("title", JSONObject().put("stringValue", title))
        fields.put("pic", JSONObject().put("stringValue", pic))
        fields.put("isCompleted", JSONObject().put("booleanValue", isCompleted))
        fields.put("sortOrder", JSONObject().put("integerValue", sortOrder.toString()))
        if (description != null) {
            fields.put("description", JSONObject().put("stringValue", description))
        } else {
            fields.put("description", JSONObject().put("nullValue", JSONObject.NULL))
        }
        if (dueDate != null) {
            fields.put("dueDate", JSONObject().put("integerValue", dueDate.toString()))
        } else {
            fields.put("dueDate", JSONObject().put("nullValue", JSONObject.NULL))
        }

        return JSONObject().put("fields", fields).toString()
    }

    fun JSONObject.toWeddingTaskEntity(): WeddingTaskEntity? {
        return try {
            val fields = getJSONObject("fields")
            WeddingTaskEntity(
                taskId = fields.getJSONObject("taskId").getString("stringValue"),
                weddingProfileId = fields.getJSONObject("weddingProfileId").getString("stringValue"),
                phaseMonth = fields.getJSONObject("phaseMonth").getString("integerValue").toInt(),
                title = fields.getJSONObject("title").getString("stringValue"),
                description = if (fields.has("description") && fields.getJSONObject("description").has("stringValue")) fields.getJSONObject("description").getString("stringValue") else null,
                pic = if (fields.has("pic")) fields.getJSONObject("pic").getString("stringValue") else "BOTH",
                isCompleted = if (fields.has("isCompleted")) fields.getJSONObject("isCompleted").getBoolean("booleanValue") else false,
                dueDate = if (fields.has("dueDate") && fields.getJSONObject("dueDate").has("integerValue")) fields.getJSONObject("dueDate").getString("integerValue").toLongOrNull() else null,
                sortOrder = if (fields.has("sortOrder")) fields.getJSONObject("sortOrder").getString("integerValue").toInt() else 0
            )
        } catch (e: Exception) {
            null
        }
    }
}
