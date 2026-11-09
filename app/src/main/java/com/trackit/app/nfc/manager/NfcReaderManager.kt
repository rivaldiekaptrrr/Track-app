package com.trackit.app.nfc.manager

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.parser.EmoneyCardParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcReaderManager @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _cardEvents = MutableSharedFlow<Result<EmoneyCardResult>>(extraBufferCapacity = 1)
    val cardEvents: SharedFlow<Result<EmoneyCardResult>> = _cardEvents.asSharedFlow()

    fun isNfcSupported(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context) != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return adapter.isEnabled
    }

    fun enableReaderMode(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val options = Bundle().apply {
            // Menunda polling presence check internal Android agar transceive APDU tidak terinterupsi tiba-tiba
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        adapter.enableReaderMode(
            activity,
            { tag: Tag ->
                scope.launch {
                    val result = EmoneyCardParser.readCard(tag)
                    _cardEvents.emit(result)
                }
            },
            flags,
            options
        )
    }

    fun disableReaderMode(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (_: Exception) {}
    }
}

