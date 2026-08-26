package com.trackit.app.util

import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingCommitteeEntity
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingGuestEntity
import com.trackit.app.data.local.entity.WeddingPaymentTermEntity
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.local.entity.WeddingVendorEntity
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.local.entity.BudgetSettingEntity
import com.trackit.app.data.local.entity.CategoryBudgetEntity
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

    // ── Shared helpers ──────────────────────────────────────────────────────────

    private fun JSONObject.putStr(key: String, value: String?) {
        val v = JSONObject()
        if (value == null) v.put("nullValue", JSONObject.NULL) else v.put("stringValue", value)
        put(key, v)
    }
    private fun JSONObject.putLng(key: String, value: Long?) {
        val v = JSONObject()
        if (value == null) v.put("nullValue", JSONObject.NULL) else v.put("integerValue", value.toString())
        put(key, v)
    }
    private fun JSONObject.putInt(key: String, value: Int) {
        put(key, JSONObject().put("integerValue", value.toString()))
    }
    private fun JSONObject.putDbl(key: String, value: Double) {
        put(key, JSONObject().put("doubleValue", value))
    }
    private fun JSONObject.putBool(key: String, value: Boolean) {
        put(key, JSONObject().put("booleanValue", value))
    }
    private fun JSONObject.strOrNull(key: String): String? {
        if (!has(key)) return null
        val f = getJSONObject(key)
        return if (f.has("stringValue")) f.getString("stringValue") else null
    }
    private fun JSONObject.longOrNull(key: String): Long? {
        if (!has(key)) return null
        val f = getJSONObject(key)
        return if (f.has("integerValue")) f.getString("integerValue").toLongOrNull() else null
    }
    private fun JSONObject.intOrZero(key: String): Int {
        if (!has(key)) return 0
        val f = getJSONObject(key)
        return if (f.has("integerValue")) f.getString("integerValue").toIntOrNull() ?: 0 else 0
    }
    private fun JSONObject.dblOrZero(key: String): Double {
        if (!has(key)) return 0.0
        val f = getJSONObject(key)
        return if (f.has("doubleValue")) f.getDouble("doubleValue") else 0.0
    }
    private fun JSONObject.boolOrFalse(key: String): Boolean {
        if (!has(key)) return false
        val f = getJSONObject(key)
        return if (f.has("booleanValue")) f.getBoolean("booleanValue") else false
    }
    private fun fields(block: JSONObject.() -> Unit): String =
        JSONObject().put("fields", JSONObject().apply(block)).toString()

    // ======================= TRANSACTION =======================

    fun TransactionEntity.toFirestoreJson(): String = fields {
        putStr("id", id)
        putDbl("amount", amount)
        putStr("description", description)
        putStr("categoryId", categoryId)
        putLng("date", date)
        putLng("createdAt", createdAt)
        putBool("isRecurring", isRecurring)
        putStr("recurringType", recurringType)
        putInt("recurringDayOfMonth", recurringDayOfMonth ?: 0)
        putStr("type", type)
        putLng("profileId", profileId)
    }

    fun JSONObject.toTransactionEntity(): TransactionEntity? {
        return try {
            val f = getJSONObject("fields")
            TransactionEntity(
                id = f.strOrNull("id") ?: java.util.UUID.randomUUID().toString(),
                amount = f.dblOrZero("amount"),
                description = f.strOrNull("description") ?: "",
                categoryId = f.strOrNull("categoryId"),
                date = f.longOrNull("date") ?: System.currentTimeMillis(),
                createdAt = f.longOrNull("createdAt") ?: System.currentTimeMillis(),
                isRecurring = f.boolOrFalse("isRecurring"),
                recurringType = f.strOrNull("recurringType"),
                recurringDayOfMonth = f.intOrZero("recurringDayOfMonth").takeIf { it != 0 },
                type = f.strOrNull("type") ?: "EXPENSE",
                profileId = f.longOrNull("profileId") ?: 1L
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING PROFILE =======================

    fun WeddingProfileEntity.toFirestoreJson(): String = fields {
        putStr("id", id)
        putStr("groomName", groomName)
        putStr("brideName", brideName)
        putLng("weddingDate", weddingDate)
        putDbl("totalBudgetCap", totalBudgetCap)
        putStr("religionType", religionType)
        putStr("religionDetail", religionDetail)
        putStr("culturalPresetGroom", culturalPresetGroom)
        putStr("culturalPresetBride", culturalPresetBride)
        putStr("quote", quote)
        putBool("quoteEnabled", quoteEnabled)
        putStr("quoteFontSize", quoteFontSize)
        putStr("quoteFontStyle", quoteFontStyle)
        putLng("createdAt", createdAt)
    }

    fun JSONObject.toWeddingProfileEntity(): WeddingProfileEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingProfileEntity(
                id = f.strOrNull("id") ?: return null,
                groomName = f.strOrNull("groomName") ?: "",
                brideName = f.strOrNull("brideName") ?: "",
                weddingDate = f.longOrNull("weddingDate") ?: System.currentTimeMillis(),
                totalBudgetCap = f.dblOrZero("totalBudgetCap"),
                religionType = f.strOrNull("religionType") ?: "ISLAM",
                religionDetail = f.strOrNull("religionDetail"),
                culturalPresetGroom = f.strOrNull("culturalPresetGroom"),
                culturalPresetBride = f.strOrNull("culturalPresetBride"),
                quote = f.strOrNull("quote"),
                quoteEnabled = f.boolOrFalse("quoteEnabled"),
                quoteFontSize = f.strOrNull("quoteFontSize") ?: "SEDANG",
                quoteFontStyle = f.strOrNull("quoteFontStyle") ?: "ITALIC",
                createdAt = f.longOrNull("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING EXPENSE =======================

    fun WeddingExpenseEntity.toFirestoreJson(): String = fields {
        putStr("expenseId", expenseId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("category", category)
        putStr("title", title)
        putDbl("totalEstimated", totalEstimated)
        putDbl("totalPaid", totalPaid)
        putStr("paidBySource", paidBySource)
        putStr("paymentStatus", paymentStatus)
        putLng("createdAt", createdAt)
        putStr("notes", notes)
    }

    fun JSONObject.toWeddingExpenseEntity(): WeddingExpenseEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingExpenseEntity(
                expenseId = f.strOrNull("expenseId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                category = f.strOrNull("category") ?: "",
                title = f.strOrNull("title") ?: "",
                totalEstimated = f.dblOrZero("totalEstimated"),
                totalPaid = f.dblOrZero("totalPaid"),
                paidBySource = f.strOrNull("paidBySource") ?: "BERSAMA",
                paymentStatus = f.strOrNull("paymentStatus") ?: "UNPAID",
                notes = f.strOrNull("notes"),
                createdAt = f.longOrNull("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING TASK =======================

    fun WeddingTaskEntity.toFirestoreJson(): String = fields {
        putStr("taskId", taskId)
        putStr("weddingProfileId", weddingProfileId)
        putInt("phaseMonth", phaseMonth)
        putStr("title", title)
        putStr("description", description)
        putStr("pic", pic)
        putBool("isCompleted", isCompleted)
        putLng("dueDate", dueDate)
        putInt("sortOrder", sortOrder)
    }

    fun JSONObject.toWeddingTaskEntity(): WeddingTaskEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingTaskEntity(
                taskId = f.strOrNull("taskId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                phaseMonth = f.intOrZero("phaseMonth"),
                title = f.strOrNull("title") ?: "",
                description = f.strOrNull("description"),
                pic = f.strOrNull("pic") ?: "BOTH",
                isCompleted = f.boolOrFalse("isCompleted"),
                dueDate = f.longOrNull("dueDate"),
                sortOrder = f.intOrZero("sortOrder")
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING VENDOR =======================

    fun WeddingVendorEntity.toFirestoreJson(): String = fields {
        putStr("vendorId", vendorId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("category", category)
        putStr("name", name)
        putStr("picName", picName)
        putStr("phoneNumber", phoneNumber)
        putStr("instagramHandle", instagramHandle)
        putDbl("contractValue", contractValue)
        putStr("notes", notes)
        putStr("status", status)
        putLng("createdAt", createdAt)
    }

    fun JSONObject.toWeddingVendorEntity(): WeddingVendorEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingVendorEntity(
                vendorId = f.strOrNull("vendorId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                category = f.strOrNull("category") ?: "",
                name = f.strOrNull("name") ?: "",
                picName = f.strOrNull("picName"),
                phoneNumber = f.strOrNull("phoneNumber"),
                instagramHandle = f.strOrNull("instagramHandle"),
                contractValue = f.dblOrZero("contractValue"),
                notes = f.strOrNull("notes"),
                status = f.strOrNull("status") ?: "PROSPEK",
                createdAt = f.longOrNull("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING GUEST =======================

    fun WeddingGuestEntity.toFirestoreJson(): String = fields {
        putStr("guestId", guestId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("guestName", guestName)
        putStr("phoneNumber", phoneNumber)
        putStr("groupAllocation", groupAllocation)
        putStr("sessionTarget", sessionTarget)
        putInt("estimatedPax", estimatedPax)
        putStr("rsvpStatus", rsvpStatus)
    }

    fun JSONObject.toWeddingGuestEntity(): WeddingGuestEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingGuestEntity(
                guestId = f.strOrNull("guestId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                guestName = f.strOrNull("guestName") ?: "",
                phoneNumber = f.strOrNull("phoneNumber"),
                groupAllocation = f.strOrNull("groupAllocation") ?: "TEMAN_CPP",
                sessionTarget = f.strOrNull("sessionTarget") ?: "KEDUANYA",
                estimatedPax = f.intOrZero("estimatedPax").coerceAtLeast(1),
                rsvpStatus = f.strOrNull("rsvpStatus") ?: "PENDING"
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING COMMITTEE =======================

    fun WeddingCommitteeEntity.toFirestoreJson(): String = fields {
        putStr("memberId", memberId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("memberName", memberName)
        putStr("role", role)
        putStr("side", side)
        putStr("phoneNumber", phoneNumber)
        putStr("uniformDescription", uniformDescription)
        putDbl("fabricMeters", fabricMeters)
        putStr("uniformStatus", uniformStatus)
        putInt("sortOrder", sortOrder)
    }

    fun JSONObject.toWeddingCommitteeEntity(): WeddingCommitteeEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingCommitteeEntity(
                memberId = f.strOrNull("memberId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                memberName = f.strOrNull("memberName") ?: "",
                role = f.strOrNull("role") ?: "",
                side = f.strOrNull("side") ?: "KELUARGA_CPP",
                phoneNumber = f.strOrNull("phoneNumber"),
                uniformDescription = f.strOrNull("uniformDescription"),
                fabricMeters = f.dblOrZero("fabricMeters"),
                uniformStatus = f.strOrNull("uniformStatus") ?: "BELUM_DIBAGI",
                sortOrder = f.intOrZero("sortOrder")
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING PAYMENT TERM =======================

    fun WeddingPaymentTermEntity.toFirestoreJson(): String = fields {
        putStr("termId", termId)
        putStr("expenseId", expenseId)
        putStr("termName", termName)
        putDbl("amount", amount)
        putLng("dueDate", dueDate)
        putBool("isPaid", isPaid)
        putLng("paidDate", paidDate)
    }

    fun JSONObject.toWeddingPaymentTermEntity(): WeddingPaymentTermEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingPaymentTermEntity(
                termId = f.strOrNull("termId") ?: return null,
                expenseId = f.strOrNull("expenseId") ?: return null,
                termName = f.strOrNull("termName") ?: "",
                amount = f.dblOrZero("amount"),
                dueDate = f.longOrNull("dueDate") ?: System.currentTimeMillis(),
                isPaid = f.boolOrFalse("isPaid"),
                paidDate = f.longOrNull("paidDate")
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING SESERAHAN =======================

    fun WeddingSeserahanEntity.toFirestoreJson(): String = fields {
        putStr("itemId", itemId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("direction", direction)
        putStr("itemName", itemName)
        putInt("quantity", quantity)
        putDbl("estimatedPrice", estimatedPrice)
        putStr("status", status)
        putStr("notes", notes)
        putInt("sortOrder", sortOrder)
    }

    fun JSONObject.toWeddingSeserahanEntity(): WeddingSeserahanEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingSeserahanEntity(
                itemId = f.strOrNull("itemId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                direction = f.strOrNull("direction") ?: "SESERAHAN_CPP",
                itemName = f.strOrNull("itemName") ?: "",
                quantity = f.intOrZero("quantity").coerceAtLeast(1),
                estimatedPrice = f.dblOrZero("estimatedPrice"),
                status = f.strOrNull("status") ?: "BELUM_BELI",
                notes = f.strOrNull("notes"),
                sortOrder = f.intOrZero("sortOrder")
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING DOCUMENT =======================

    fun WeddingDocumentEntity.toFirestoreJson(): String = fields {
        putStr("docId", docId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("docName", docName)
        putStr("ownerType", ownerType)
        putBool("isCompleted", isCompleted)
        // localFilePath is intentionally NOT synced (it's a device-local SAF Uri)
        putDbl("adminCost", adminCost)
        putInt("sortOrder", sortOrder)
    }

    fun JSONObject.toWeddingDocumentEntity(): WeddingDocumentEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingDocumentEntity(
                docId = f.strOrNull("docId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                docName = f.strOrNull("docName") ?: "",
                ownerType = f.strOrNull("ownerType") ?: "BOTH",
                isCompleted = f.boolOrFalse("isCompleted"),
                localFilePath = null, // Device-specific — never restore from remote
                adminCost = f.dblOrZero("adminCost"),
                sortOrder = f.intOrZero("sortOrder")
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING EVENT (RUNDOWN TAB) =======================

    fun WeddingEventEntity.toFirestoreJson(): String = fields {
        putStr("eventId", eventId)
        putStr("weddingProfileId", weddingProfileId)
        putStr("eventName", eventName)
        putLng("eventDate", eventDate)
        putStr("eventLocation", eventLocation)
        putInt("sortOrder", sortOrder)
    }

    fun JSONObject.toWeddingEventEntity(): WeddingEventEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingEventEntity(
                eventId = f.strOrNull("eventId") ?: return null,
                weddingProfileId = f.strOrNull("weddingProfileId") ?: return null,
                eventName = f.strOrNull("eventName") ?: "",
                eventDate = f.longOrNull("eventDate") ?: System.currentTimeMillis(),
                eventLocation = f.strOrNull("eventLocation"),
                sortOrder = f.intOrZero("sortOrder")
            )
        } catch (e: Exception) { null }
    }

    // ======================= WEDDING RUNDOWN ITEM =======================

    fun WeddingRundownItemEntity.toFirestoreJson(): String = fields {
        putStr("itemId", itemId)
        putStr("eventId", eventId)
        putStr("timeStart", timeStart)
        putInt("durationMinutes", durationMinutes)
        putStr("sessionTitle", sessionTitle)
        putStr("pic", pic)
        putStr("mcScript", mcScript)
        putInt("sortOrder", sortOrder)
    }

    fun JSONObject.toWeddingRundownItemEntity(): WeddingRundownItemEntity? {
        return try {
            val f = getJSONObject("fields")
            WeddingRundownItemEntity(
                itemId = f.strOrNull("itemId") ?: return null,
                eventId = f.strOrNull("eventId") ?: return null,
                timeStart = f.strOrNull("timeStart") ?: "08:00",
                durationMinutes = f.intOrZero("durationMinutes").coerceAtLeast(1),
                sessionTitle = f.strOrNull("sessionTitle") ?: "",
                pic = f.strOrNull("pic"),
                mcScript = f.strOrNull("mcScript"),
                sortOrder = f.intOrZero("sortOrder")
            )
        } catch (e: Exception) { null }
    }

    // ======================= CATEGORY =======================

    fun CategoryEntity.toFirestoreJson(): String = fields {
        putStr("id", id)
        putStr("name", name)
        putStr("iconName", iconName)
        putStr("colorHex", colorHex)
        putStr("customKeywords", customKeywords)
        putStr("type", type)
        putBool("isHidden", isHidden)
        putLng("profileId", profileId)
    }

    fun JSONObject.toCategoryEntity(): CategoryEntity? {
        return try {
            val f = getJSONObject("fields")
            CategoryEntity(
                id = f.strOrNull("id") ?: java.util.UUID.randomUUID().toString(),
                name = f.strOrNull("name") ?: return null,
                iconName = f.strOrNull("iconName") ?: "category",
                colorHex = f.strOrNull("colorHex") ?: "#1565C0",
                customKeywords = f.strOrNull("customKeywords") ?: "",
                type = f.strOrNull("type") ?: "EXPENSE",
                isHidden = f.boolOrFalse("isHidden"),
                profileId = f.longOrNull("profileId") ?: 1L
            )
        } catch (e: Exception) { null }
    }

    // ======================= PROFILE =======================

    fun ProfileEntity.toFirestoreJson(): String = fields {
        putLng("id", id)
        putStr("name", name)
        putStr("iconName", iconName)
        putStr("colorHex", colorHex)
        putLng("createdAt", createdAt)
        putStr("mode", mode)
        putStr("weddingProfileId", weddingProfileId)
    }

    fun JSONObject.toProfileEntity(): ProfileEntity? {
        return try {
            val f = getJSONObject("fields")
            ProfileEntity(
                id = f.longOrNull("id") ?: 0L,
                name = f.strOrNull("name") ?: return null,
                iconName = f.strOrNull("iconName") ?: "person",
                colorHex = f.strOrNull("colorHex") ?: "#1565C0",
                createdAt = f.longOrNull("createdAt") ?: System.currentTimeMillis(),
                mode = f.strOrNull("mode") ?: "EXPENSE",
                weddingProfileId = f.strOrNull("weddingProfileId")
            )
        } catch (e: Exception) { null }
    }

    // ======================= BUDGET SETTING =======================

    fun BudgetSettingEntity.toFirestoreJson(): String = fields {
        putLng("profileId", profileId)
        putDbl("monthlyBudget", monthlyBudget)
    }

    fun JSONObject.toBudgetSettingEntity(): BudgetSettingEntity? {
        return try {
            val f = getJSONObject("fields")
            BudgetSettingEntity(
                profileId = f.longOrNull("profileId") ?: return null,
                monthlyBudget = f.dblOrZero("monthlyBudget")
            )
        } catch (e: Exception) { null }
    }

    // ======================= CATEGORY BUDGET =======================

    fun CategoryBudgetEntity.toFirestoreJson(): String = fields {
        putStr("categoryId", categoryId)
        putDbl("amount", amount)
        putDbl("alertPercentage", alertPercentage.toDouble())
        putStr("lastWarningMonth", lastWarningMonth)
        putLng("profileId", profileId)
    }

    fun JSONObject.toCategoryBudgetEntity(): CategoryBudgetEntity? {
        return try {
            val f = getJSONObject("fields")
            CategoryBudgetEntity(
                categoryId = f.strOrNull("categoryId") ?: return null,
                amount = f.dblOrZero("amount"),
                alertPercentage = f.dblOrZero("alertPercentage").toFloat().coerceIn(0f, 1f),
                lastWarningMonth = f.strOrNull("lastWarningMonth") ?: "",
                profileId = f.longOrNull("profileId") ?: 1L
            )
        } catch (e: Exception) { null }
    }
}

