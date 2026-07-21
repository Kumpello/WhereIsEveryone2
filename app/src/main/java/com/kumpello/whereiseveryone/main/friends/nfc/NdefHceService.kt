package com.kumpello.whereiseveryone.main.friends.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import timber.log.Timber

class NdefHceService : HostApduService() {

    private val preferencesManager: PreferencesManager by inject()

    private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val STATUS_FAILED = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    private val CAPABILITY_CONTAINER = byteArrayOf(
        0x00, 0x0F, // CCLEN
        0x20,       // Mapping Version
        0x00, 0xFF.toByte(), // MLe (Increased to 255)
        0x00, 0xFF.toByte(), // MLc (Increased to 255)
        0x04, 0x06, // T (NDEF Control TLV), L (Length)
        0xE1.toByte(), 0x04.toByte(), // File ID (E104)
        0x01, 0xFF.toByte(), // Max NDEF size (Increased to 511)
        0x00,       // Read access
        0xFF.toByte() // Write access
    )

    private enum class SelectedFile {
        NONE, CC, NDEF
    }

    private var selectedFile = SelectedFile.NONE
    private var ndefFile: ByteArray? = null

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("Service Created")
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val hexCommand = commandApdu.joinToString("") { "%02X".format(it) }
        Timber.tag(TAG).d("Command received: $hexCommand")
        Timber.tag(TAG).d("Current state: $selectedFile")

        val response = when {
            // SELECT AID (D2760000850101)
            hexCommand.contains("00A4040007D2760000850101") -> {
                Timber.tag(TAG).d("AID Selected")
                selectedFile = SelectedFile.NONE
                prepareNdefFile() // Pre-calculate to avoid latency
                STATUS_SUCCESS
            }

            // SELECT CC FILE (E103)
            hexCommand.contains("00A4000C02E103") -> {
                Timber.tag(TAG).d("CC File Selected")
                selectedFile = SelectedFile.CC
                STATUS_SUCCESS
            }

            // SELECT NDEF FILE (E104)
            hexCommand.contains("00A4000C02E104") -> {
                Timber.tag(TAG).d("NDEF File Selected")
                selectedFile = SelectedFile.NDEF
                STATUS_SUCCESS
            }

            // READ BINARY
            hexCommand.startsWith("00B0") -> {
                val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
                val length = if (commandApdu.size > 4) commandApdu[4].toInt() and 0xFF else 0

                val fullFile = when (selectedFile) {
                    SelectedFile.CC -> CAPABILITY_CONTAINER
                    SelectedFile.NDEF -> ndefFile
                    else -> null
                }

                if (fullFile != null) {
                    val end = if (length == 0) fullFile.size else minOf(offset + length, fullFile.size)
                    if (offset < fullFile.size) {
                        // Successfully served NDEF content
                        if (selectedFile == SelectedFile.NDEF && offset + length >= fullFile.size) {
                            Timber.tag(TAG).i("NDEF successfully served. Sending success broadcast.")
                            val intent = android.content.Intent("com.kumpello.whereiseveryone.NFC_SUCCESS")
                            intent.setPackage(packageName)
                            sendBroadcast(intent)
                        }
                        fullFile.sliceArray(offset until end) + STATUS_SUCCESS
                    } else {
                        STATUS_SUCCESS // End of file
                    }
                } else {
                    STATUS_FAILED
                }
            }

            else -> {
                Timber.tag(TAG).w("Unknown Command: $hexCommand")
                STATUS_FAILED
            }
        }
        
        Timber.tag(TAG).d("Response sent: ${response.joinToString("") { "%02X".format(it) }}")
        return response
    }

    private fun prepareNdefFile() {
        val username = runBlocking {
            preferencesManager.get(PreferencesKey.UserName)
        }?.trim() ?: "unknown"

        val uri = "whereiseveryone://addfriend/$username"
        Timber.tag(TAG).d("Username: '$username' (len: ${username.length}), URI: '$uri' (len: ${uri.length})")

        // Manually construct the URI record to be 100% sure of the length and prefix
        val uriPayload = byteArrayOf(0x00.toByte()) + uri.toByteArray(Charsets.UTF_8)
        val uriRecord = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, null, uriPayload)
        
        // Remove AAR for now to simplify parsing and see if it resolves the 0-record issue
        val message = NdefMessage(arrayOf(uriRecord))
        val payload = message.toByteArray()
        val size = payload.size
        
        Timber.tag(TAG).d("NDEF Message Payload (size: $size): ${payload.joinToString("") { "%02X".format(it) }}")
        
        ndefFile = byteArrayOf(
            (size shr 8).toByte(), (size and 0xFF).toByte()
        ) + payload
    }

    override fun onDeactivated(reason: Int) {
        Timber.tag(TAG).d("Deactivated. Reason: ${when(reason) {
            DEACTIVATION_LINK_LOSS -> "Link Loss"
            DEACTIVATION_DESELECTED -> "Deselected"
            else -> "Unknown ($reason)"
        }}")
        selectedFile = SelectedFile.NONE
        ndefFile = null
    }

    companion object {
        private const val TAG = "NDEF_HCE_SERVICE"
    }
}
