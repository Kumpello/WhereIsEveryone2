package com.kumpello.whereiseveryone.main.friends.nfc

import android.app.KeyguardManager
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.kumpello.whereiseveryone.common.extension.createAddFriendDeepLink
import org.koin.android.ext.android.inject

class NdefHceService : HostApduService() {
    private val sharingSession: NfcSharingSession by inject()
    private val processor by lazy {
        NdefApduProcessor(sharingSession, ::createNdefFile) {
            sendBroadcast(Intent(ACTION_PROFILE_SHARED).setPackage(packageName))
        }
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val keyguard = getSystemService(KeyguardManager::class.java)
        val unlocked = keyguard != null && !keyguard.isDeviceLocked && !keyguard.isKeyguardLocked
        return processor.process(commandApdu, deviceUnlocked = unlocked)
    }

    private fun createNdefFile(username: String): ByteArray {
        val uriPayload = byteArrayOf(0x00) + createAddFriendDeepLink(username).toByteArray(Charsets.UTF_8)
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, null, uriPayload)
        val payload = NdefMessage(arrayOf(record)).toByteArray()
        return byteArrayOf((payload.size shr 8).toByte(), (payload.size and 0xFF).toByte()) + payload
    }

    override fun onDeactivated(reason: Int) {
        processor.reset()
    }

    companion object {
        const val ACTION_PROFILE_SHARED = "com.kumpello.whereiseveryone.NFC_SUCCESS"
    }
}
