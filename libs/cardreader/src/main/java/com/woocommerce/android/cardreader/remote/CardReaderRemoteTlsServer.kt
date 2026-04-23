package com.woocommerce.android.cardreader.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.tls.HeldCertificate
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

internal class CardReaderRemoteTlsServer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private var serverSocket: SSLServerSocket? = null
    private var certificate: X509Certificate? = null

    val port: Int
        get() = requireNotNull(serverSocket) { "Server not started" }.localPort

    val fingerprint: String
        get() = CardReaderRemoteFingerprint.sha256Base64(
            requireNotNull(certificate) { "Server not started" }
        )

    suspend fun start(): Unit = withContext(ioDispatcher) {
        check(serverSocket == null) { "Server already started" }

        val held = HeldCertificate.Builder()
            .commonName(CERT_COMMON_NAME)
            .ecdsa256()
            .duration(VALIDITY_DURATION_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setKeyEntry(
                KEY_ALIAS,
                held.keyPair.private,
                KEYSTORE_PASSWORD,
                arrayOf<java.security.cert.Certificate>(held.certificate),
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
        certificate = held.certificate
    }

    suspend fun acceptOne(): CardReaderRemoteConnection = withContext(ioDispatcher) {
        val socket = requireNotNull(serverSocket) { "Server not started" }
        val accepted = socket.accept().apply {
            soTimeout = SESSION_READ_TIMEOUT_MILLIS
        }
        CardReaderRemoteConnection(accepted, ioDispatcher = ioDispatcher)
    }

    override fun close() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        certificate = null
    }

    companion object {
        private const val KEY_ALIAS = "woopos-remote"
        private val KEYSTORE_PASSWORD = CharArray(0)
        private const val CERT_COMMON_NAME = "woopos-remote-reader"
        private const val VALIDITY_DURATION_SECONDS = 24L * 60 * 60
        private const val ACCEPT_TIMEOUT_MILLIS = 30_000
        private const val SESSION_READ_TIMEOUT_MILLIS = 90_000
    }
}
