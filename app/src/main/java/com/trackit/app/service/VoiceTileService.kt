package com.trackit.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.trackit.app.MainActivity

class VoiceTileService : TileService() {
    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("START_VOICE_IMMEDIATELY", true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ : startActivityAndCollapse(Intent) dihapus, wajib pakai PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            // Android < 14 : gunakan versi lama yang masih didukung
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
