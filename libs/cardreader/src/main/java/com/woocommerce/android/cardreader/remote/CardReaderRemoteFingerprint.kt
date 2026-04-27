package com.woocommerce.android.cardreader.remote

import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

object CardReaderRemoteFingerprint {
    internal fun sha256(cert: X509Certificate): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(cert.encoded)
    }

    internal fun sha256Base64(cert: X509Certificate): String = encodeBase64(sha256(cert))

    internal fun pairingCode(cert: X509Certificate): String = pairingCodeFromBytes(sha256(cert))

    fun pairingCodeFromBase64(fingerprintBase64: String): String = pairingCodeFromBytes(decodeBase64(fingerprintBase64))

    private fun pairingCodeFromBytes(fingerprint: ByteArray): String {
        require(fingerprint.size >= 2) { "Fingerprint too short" }
        return fingerprint.take(2).joinToString("") { "%02X".format(it) }
    }

    private fun encodeBase64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun decodeBase64(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)
}
