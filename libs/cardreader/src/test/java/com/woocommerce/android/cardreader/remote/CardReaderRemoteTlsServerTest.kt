package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import kotlinx.coroutines.test.runTest
import okhttp3.tls.HeldCertificate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import java.security.cert.CertificateParsingException

class CardReaderRemoteTlsServerTest {
    private val logWrapper: LogWrapper = mock()

    @Test
    fun `given ecdsa certificate builds, when start, then ecdsa key type is used`() = runTest {
        // GIVEN
        val server = CardReaderRemoteTlsServer(
            logWrapper = logWrapper,
            heldCertificateFactory = { keyType -> buildCertificate(keyType) },
        )

        // WHEN
        server.start()

        // THEN
        assertThat(server.certificateKeyType).isEqualTo(CardReaderRemoteCertificateKeyType.ECDSA_256)
        server.close()
    }

    @Test
    fun `given ecdsa certificate is rejected, when start, then falls back to rsa and starts`() = runTest {
        // GIVEN
        val server = CardReaderRemoteTlsServer(
            logWrapper = logWrapper,
            heldCertificateFactory = { keyType ->
                when (keyType) {
                    CardReaderRemoteCertificateKeyType.ECDSA_256 ->
                        throw IllegalArgumentException("failed to decode certificate")
                    CardReaderRemoteCertificateKeyType.RSA_2048 -> buildCertificate(keyType)
                }
            },
        )

        // WHEN
        server.start()

        // THEN
        assertThat(server.certificateKeyType).isEqualTo(CardReaderRemoteCertificateKeyType.RSA_2048)
        assertThat(server.port).isGreaterThan(0)
        assertThat(server.fingerprint).isNotEmpty()
        server.close()
    }

    @Test
    fun `given both key types are rejected, when start, then thrown message carries the root cause`() = runTest {
        // GIVEN
        val server = CardReaderRemoteTlsServer(
            logWrapper = logWrapper,
            heldCertificateFactory = {
                throw IllegalArgumentException(
                    "failed to decode certificate",
                    CertificateParsingException("Invalid encoding: unexpected tag"),
                )
            },
        )

        // WHEN
        val failure = runCatching { server.start() }.exceptionOrNull()

        // THEN
        assertThat(failure)
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("failed to decode certificate")
            .hasMessageContaining("Invalid encoding: unexpected tag")
    }

    private fun buildCertificate(keyType: CardReaderRemoteCertificateKeyType): HeldCertificate =
        HeldCertificate.Builder()
            .commonName("test")
            .apply {
                when (keyType) {
                    CardReaderRemoteCertificateKeyType.ECDSA_256 -> ecdsa256()
                    CardReaderRemoteCertificateKeyType.RSA_2048 -> rsa2048()
                }
            }
            .build()
}
