package com.woocommerce.android.cardreader.remote

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class CardReaderRemoteTlsClientTest {
    private val certBytes = byteArrayOf(1, 2, 3, 4, 5)
    private val matchingFingerprint = "dPgf4WfZm0y0HW0MzagieMrunz4vJdXlo5Nv89zsYNA"
    private val mismatchingFingerprint = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU"

    @Test
    fun `given matching pinned fingerprint, when checkServerTrusted, then does not throw`() {
        // GIVEN
        val trustManager = CardReaderRemoteTlsClient.PinnedFingerprintTrustManager(matchingFingerprint)

        // WHEN / THEN
        trustManager.checkServerTrusted(arrayOf(StubCert(certBytes)), "EC")
    }

    @Test
    fun `given mismatched pinned fingerprint, when checkServerTrusted, then throws CertificateException`() {
        // GIVEN
        val trustManager = CardReaderRemoteTlsClient.PinnedFingerprintTrustManager(mismatchingFingerprint)

        // WHEN / THEN
        assertThatThrownBy { trustManager.checkServerTrusted(arrayOf(StubCert(certBytes)), "EC") }
            .isInstanceOf(CertificateException::class.java)
    }

    @Test
    fun `given empty chain, when checkServerTrusted, then throws CertificateException`() {
        // GIVEN
        val trustManager = CardReaderRemoteTlsClient.PinnedFingerprintTrustManager(matchingFingerprint)

        // WHEN / THEN
        assertThatThrownBy { trustManager.checkServerTrusted(emptyArray(), "EC") }
            .isInstanceOf(CertificateException::class.java)
    }

    @Test
    fun `when checkClientTrusted, then always throws CertificateException`() {
        // GIVEN
        val trustManager = CardReaderRemoteTlsClient.PinnedFingerprintTrustManager(matchingFingerprint)

        // WHEN / THEN
        assertThatThrownBy { trustManager.checkClientTrusted(arrayOf(StubCert(certBytes)), "EC") }
            .isInstanceOf(CertificateException::class.java)
    }

    @Test
    fun `when getAcceptedIssuers, then returns empty array`() {
        // GIVEN
        val trustManager = CardReaderRemoteTlsClient.PinnedFingerprintTrustManager(matchingFingerprint)

        // WHEN
        val issuers = trustManager.acceptedIssuers

        // THEN
        assertThat(issuers).isEmpty()
    }

    private class StubCert(private val der: ByteArray) : X509Certificate() {
        override fun getEncoded(): ByteArray = der
        override fun getCriticalExtensionOIDs() = error("not used")
        override fun getExtensionValue(oid: String?) = error("not used")
        override fun getNonCriticalExtensionOIDs() = error("not used")
        override fun hasUnsupportedCriticalExtension() = error("not used")
        override fun checkValidity() = error("not used")
        override fun checkValidity(date: java.util.Date?) = error("not used")
        override fun getVersion() = error("not used")
        override fun getSerialNumber() = error("not used")
        override fun getIssuerDN() = error("not used")
        override fun getSubjectDN() = error("not used")
        override fun getNotBefore() = error("not used")
        override fun getNotAfter() = error("not used")
        override fun getTBSCertificate() = error("not used")
        override fun getSignature() = error("not used")
        override fun getSigAlgName() = error("not used")
        override fun getSigAlgOID() = error("not used")
        override fun getSigAlgParams() = error("not used")
        override fun getIssuerUniqueID() = error("not used")
        override fun getSubjectUniqueID() = error("not used")
        override fun getKeyUsage() = error("not used")
        override fun getBasicConstraints() = error("not used")
        override fun verify(key: java.security.PublicKey?) = error("not used")
        override fun verify(key: java.security.PublicKey?, sigProvider: String?) = error("not used")
        override fun toString() = "StubCert"
        override fun getPublicKey() = error("not used")
    }
}
