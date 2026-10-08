package com.kumpello.whereiseveryone.feature.main.sharing.nfc

/** Processes APDUs on the HCE service's main thread. Authorization is checked on every command. */
internal class NdefApduProcessor(
    private val sharingSession: NfcSharingSession,
    private val createNdefFile: (String) -> ByteArray,
    private val onShared: () -> Unit
) {
    private enum class SelectedFile { NONE, CC, NDEF }

    private var selectedSession: NfcSharingSession.Session? = null
    private var selectedFile = SelectedFile.NONE
    private var ndefFile: ByteArray? = null

    fun process(command: ByteArray, deviceUnlocked: Boolean): ByteArray {
        if (!deviceUnlocked) sharingSession.stop()
        val session = sharingSession.current()
        if (session == null) {
            reset()
            return STATUS_FAILED
        }
        if (selectedSession !== session) reset()

        val hex = command.joinToString("") { "%02X".format(it) }
        if (hex.startsWith("00A4040007D2760000850101")) {
            reset()
            selectedSession = session
            ndefFile = createNdefFile(session.username)
            return STATUS_SUCCESS
        }
        if (selectedSession == null) return STATUS_FAILED

        return when {
            hex.startsWith("00A4000C02E103") -> {
                selectedFile = SelectedFile.CC
                STATUS_SUCCESS
            }
            hex.startsWith("00A4000C02E104") -> {
                selectedFile = SelectedFile.NDEF
                STATUS_SUCCESS
            }
            hex.startsWith("00B0") && command.size == 5 -> {
                val offset = ((command[2].toInt() and 0xFF) shl 8) or (command[3].toInt() and 0xFF)
                val length = command[4].toInt() and 0xFF
                val file = when (selectedFile) {
                    SelectedFile.CC -> CAPABILITY_CONTAINER
                    SelectedFile.NDEF -> ndefFile
                    SelectedFile.NONE -> null
                } ?: return STATUS_FAILED
                if (offset >= file.size) return STATUS_SUCCESS
                val end = if (length == 0) file.size else minOf(offset + length, file.size)
                val response = file.copyOfRange(offset, end) + STATUS_SUCCESS
                if (selectedFile == SelectedFile.NDEF && end == file.size) {
                    sharingSession.stop()
                    reset()
                    onShared()
                }
                response
            }
            else -> STATUS_FAILED
        }
    }

    fun reset() {
        selectedSession = null
        selectedFile = SelectedFile.NONE
        ndefFile = null
    }

    private companion object {
        val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        val STATUS_FAILED = byteArrayOf(0x6A, 0x82.toByte())
        val CAPABILITY_CONTAINER = byteArrayOf(
            0x00, 0x0F, // CCLEN
            0x20, // Mapping version
            0x00, 0xFF.toByte(), // MLe
            0x00, 0xFF.toByte(), // MLc
            0x04, 0x06, // NDEF control TLV
            0xE1.toByte(), 0x04, // NDEF file ID
            0x01, 0xFF.toByte(), // Maximum NDEF size
            0x00, 0xFF.toByte() // Read-only access
        )
    }
}
