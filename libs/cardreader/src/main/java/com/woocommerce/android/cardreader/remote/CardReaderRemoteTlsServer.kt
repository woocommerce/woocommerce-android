package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.remote.CardReaderRemoteCertificateKeyType.ECDSA_256
import com.woocommerce.android.cardreader.remote.CardReaderRemoteCertificateKeyType.RSA_2048
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.tls.HeldCertificate
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

internal class CardReaderRemoteTlsServer(
    private val logWrapper: LogWrapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val heldCertificateFactory: HeldCertificateFactory = DEFAULT_CERTIFICATE_FACTORY,
) : AutoCloseable {
    private var serverSocket: SSLServerSocket? = null
    private var certificate: X509Certificate? = null
    private var keyType: CardReaderRemoteCertificateKeyType? = null

    val port: Int
        get() = requireNotNull(serverSocket) { "Server not started" }.localPort

    val fingerprint: String
        get() = CardReaderRemoteFingerprint.sha256Base64(
            requireNotNull(certificate) { "Server not started" }
        )

    val certificateKeyType: CardReaderRemoteCertificateKeyType
        get() = requireNotNull(keyType) { "Server not started" }

    suspend fun start(): Unit = withContext(ioDispatcher) {
        check(serverSocket == null) { "Server already started" }

        val identity = buildIdentity()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setKeyEntry(
                KEY_ALIAS,
                identity.held.keyPair.private,
                KEYSTORE_PASSWORD,
                arrayOf<java.security.cert.Certificate>(identity.held.certificate),
            )
        }

        val keyManagerFactory = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore, KEYSTORE_PASSWORD) }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, null, SecureRandom())
        }

        val socket = (sslContext.serverSocketFactory.createServerSocket(0) as SSLServerSocket).apply {
            soTimeout = ACCEPT_TIMEOUT_MILLIS
        }
        serverSocket = socket
        certificate = identity.held.certificate
        keyType = identity.keyType
    }

    // Some devices' Conscrypt rejects the EC certificate okhttp-tls generates, which killed the
    // session before it opened a socket. RSA-2048 resolves a different provider path.
    private fun buildIdentity(): Identity {
        val ecdsaFailure = try {
            return Identity(heldCertificateFactory.create(ECDSA_256), ECDSA_256)
        } catch (e: IllegalArgumentException) {
            e
        }

        logWrapper.w(LOG_TAG, "ECDSA certificate rejected, retrying with RSA: ${ecdsaFailure.describeCauseChain()}")

        try {
            return Identity(heldCertificateFactory.create(RSA_2048), RSA_2048)
        } catch (rsaFailure: IllegalArgumentException) {
            throw IllegalStateException(
                "Certificate generation failed for ECDSA and RSA: ${rsaFailure.describeCauseChain()}",
                rsaFailure,
            )
        }
    }

    suspend fun acceptOne(): CardReaderRemoteConnection = withContext(ioDispatcher) {
        val socket = requireNotNull(serverSocket) { "Server not started" }
        val accepted = socket.accept().apply {
            soTimeout = SESSION_READ_TIMEOUT_MILLIS
        }
        CardReaderRemoteConnection(socket = accepted, logWrapper = logWrapper, ioDispatcher = ioDispatcher)
    }

    override fun close() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        certificate = null
        keyType = null
    }

    private data class Identity(
        val held: HeldCertificate,
        val keyType: CardReaderRemoteCertificateKeyType,
    )

    internal fun interface HeldCertificateFactory {
        fun create(keyType: CardReaderRemoteCertificateKeyType): HeldCertificate
    }

    companion object {
        private const val LOG_TAG = "CardReaderRemoteTlsServer"
        private const val KEY_ALIAS = "woopos-remote"
        private val KEYSTORE_PASSWORD = CharArray(0)
        private const val CERT_COMMON_NAME = "woopos-remote-reader"
        private const val VALIDITY_DURATION_SECONDS = 24L * 60 * 60
        private const val ACCEPT_TIMEOUT_MILLIS = 30_000
        private const val SESSION_READ_TIMEOUT_MILLIS = 90_000
        private const val CAUSE_CHAIN_LIMIT = 5

        private val DEFAULT_CERTIFICATE_FACTORY = HeldCertificateFactory { keyType ->
            HeldCertificate.Builder()
                .commonName(CERT_COMMON_NAME)
                .apply {
                    when (keyType) {
                        ECDSA_256 -> ecdsa256()
                        RSA_2048 -> rsa2048()
                    }
                }
                .duration(VALIDITY_DURATION_SECONDS, TimeUnit.SECONDS)
                .build()
        }

        // Conscrypt's actual reason only lives in the cause okhttp wraps, so flatten the chain.
        private fun Throwable.describeCauseChain(): String =
            generateSequence(this) { previous -> previous.cause.takeIf { it !== previous } }
                .take(CAUSE_CHAIN_LIMIT)
                .joinToString(separator = " <- ") { "${it::class.java.name}: ${it.message}" }
    }
}
