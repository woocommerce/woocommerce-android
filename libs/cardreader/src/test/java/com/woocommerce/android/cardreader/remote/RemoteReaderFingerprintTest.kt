package com.woocommerce.android.cardreader.remote

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.security.cert.X509Certificate

class RemoteReaderFingerprintTest {
    @Test
    fun `given a known DER-encoded cert, when sha256Base64 computed, then matches expected value`() {
        // GIVEN
        val expectedDerBytes = byteArrayOf(1, 2, 3, 4, 5)
        // Precomputed SHA-256 of bytes [1,2,3,4,5] in URL-safe base64 no padding:
        // hex: 74f81fe167d99b4cb41d6d0ccda82278caee9f3e2f25d5e5a3936ff3dcec60d0
        val expectedBase64 = "dPgf4WfZm0y0HW0MzagieMrunz4vJdXlo5Nv89zsYNA"
        val expectedSuffix = "74F8"
        val cert = StubCert(expectedDerBytes)

        // WHEN
        val base64 = RemoteReaderFingerprint.sha256Base64(cert)
        val suffix = RemoteReaderFingerprint.suffix4(cert)

        // THEN
        assertThat(base64).isEqualTo(expectedBase64)
        assertThat(suffix).isEqualTo(expectedSuffix)
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
