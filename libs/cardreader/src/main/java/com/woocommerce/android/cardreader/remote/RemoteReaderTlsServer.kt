package com.woocommerce.android.cardreader.remote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.security.auth.x500.X500Principal

class RemoteReaderTlsServer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private var serverSocket: SSLServerSocket? = null
    private var certificate: X509Certificate? = null
    private var keyAlias: String? = null

    val port: Int
        get() = requireNotNull(serverSocket) { "Server not started" }.localPort

    val fingerprint: String
        get() = RemoteReaderFingerprint.sha256Base64(
            requireNotNull(certificate) { "Server not started" }
        )

    suspend fun start(): Unit = withContext(ioDispatcher) {
        val alias = "woopos-remote-${UUID.randomUUID()}"
        generateSelfSignedKeyPair(alias)

        val keyStore = androidKeyStore()
        val cert = keyStore.getCertificate(alias) as X509Certificate

        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        ).apply { init(keyStore, null) }

        val sslContext = SSLContext.getInstance("TLSv1.3").apply {
            init(keyManagerFactory.keyManagers, null, SecureRandom())
        }

        val socket = sslContext.serverSocketFactory.createServerSocket(0) as SSLServerSocket
        serverSocket = socket
        certificate = cert
        keyAlias = alias
    }

    suspend fun acceptOne(): RemoteReaderConnection = withContext(ioDispatcher) {
        val socket = requireNotNull(serverSocket) { "Server not started" }
        val accepted = socket.accept()
        RemoteReaderConnection(accepted, ioDispatcher = ioDispatcher)
    }

    override fun close() {
        runCatching { serverSocket?.close() }
        keyAlias?.let { alias ->
            runCatching { androidKeyStore().deleteEntry(alias) }
        }
        serverSocket = null
        certificate = null
        keyAlias = null
    }

    private fun generateSelfSignedKeyPair(alias: String) {
        val now = System.currentTimeMillis()
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setCertificateSubject(X500Principal("CN=$CERT_COMMON_NAME"))
            .setCertificateSerialNumber(BigInteger(SERIAL_BITS, SecureRandom()))
            .setCertificateNotBefore(Date(now - CLOCK_SKEW_MILLIS))
            .setCertificateNotAfter(Date(now + VALIDITY_MILLIS))
            .build()

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )
        generator.initialize(spec)
        generator.generateKeyPair()
    }

    private fun androidKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val EC_CURVE = "secp256r1"
        private const val CERT_COMMON_NAME = "woopos-remote-reader"
        private const val CLOCK_SKEW_MILLIS = 5L * 60 * 1000
        private const val VALIDITY_MILLIS = 24L * 60 * 60 * 1000
        private const val SERIAL_BITS = 64
    }
}
