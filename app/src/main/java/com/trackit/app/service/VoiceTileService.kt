package com.trackit.app.service

import android.content.Intent
import android.service.quicksettings.TileService
import com.trackit.app.MainActivity

class VoiceTileService : TileService() {
    override fun onClick() {
        super.onClick()
        
        // Launch MainActivity with START_VOICE_IMMEDIATELY flag
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("START_VOICE_IMMEDIATELY", true)
        }
        
        startActivityAndCollapse(intent)
    }
}
