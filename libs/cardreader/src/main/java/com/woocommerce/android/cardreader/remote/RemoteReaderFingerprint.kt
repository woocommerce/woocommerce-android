package com.woocommerce.android.cardreader.remote

import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

object RemoteReaderFingerprint {
    fun sha256(cert: X509Certificate): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(cert.encoded)
    }

    fun sha256Base64(cert: X509Certificate): String = encodeBase64(sha256(cert))

    fun suffix4(cert: X509Certificate): String = suffix4FromBytes(sha256(cert))

    fun suffix4FromBase64(fingerprintBase64: String): String = suffix4FromBytes(decodeBase64(fingerprintBase64))

    private fun suffix4FromBytes(fingerprint: ByteArray): String {
        require(fingerprint.size >= 2) { "Fingerprint too short" }
        return fingerprint.take(2).joinToString("") { "%02X".format(it) }
    }

    private fun encodeBase64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun decodeBase64(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)
}
