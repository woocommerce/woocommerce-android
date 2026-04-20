package com.woocommerce.android.cardreader.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.security.auth.x500.X500Principal

class RemoteReaderTlsServer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private var serverSocket: SSLServerSocket? = null
    private var certificate: X509Certificate? = null

    val port: Int
        get() = requireNotNull(serverSocket) { "Server not started" }.localPort

    val fingerprint: String
        get() = RemoteReaderFingerprint.sha256Base64(
            requireNotNull(certificate) { "Server not started" }
        )

    suspend fun start(): Unit = withContext(ioDispatcher) {
        val keyPair = generateEcKeyPair()
        val cert = generateSelfSignedCertificate(keyPair)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry(KEY_ALIAS, keyPair.private, EMPTY_PASSWORD, arrayOf(cert))
        }
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, EMPTY_PASSWORD)
        }
        val sslContext = SSLContext.getInstance("TLSv1.3").apply {
            init(keyManagerFactory.keyManagers, null, SecureRandom())
        }

        val socket = sslContext.serverSocketFactory.createServerSocket(0) as SSLServerSocket
        serverSocket = socket
        certificate = cert
    }

    suspend fun acceptOne(): RemoteReaderConnection = withContext(ioDispatcher) {
        val socket = requireNotNull(serverSocket) { "Server not started" }
        val accepted = socket.accept()
        RemoteReaderConnection(accepted, ioDispatcher = ioDispatcher)
    }

    override fun close() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        certificate = null
    }

    private fun generateEcKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        return generator.generateKeyPair()
    }

    private fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val notBefore = Date(now - CLOCK_SKEW_MILLIS)
        val notAfter = Date(now + VALIDITY_MILLIS)
        val subject = X500Principal("CN=woopos-remote-reader")
        val serial = BigInteger(SERIAL_BITS, SecureRandom())

        val builder = JcaX509v3CertificateBuilder(subject, serial, notBefore, notAfter, subject, keyPair.public)
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        val holder = builder.build(signer)
        return JcaX509CertificateConverter().getCertificate(holder)
    }

    companion object {
        private const val KEY_ALIAS = "woopos-remote-reader"
        private val EMPTY_PASSWORD = CharArray(0)
        private const val CLOCK_SKEW_MILLIS = 5L * 60 * 1000
        private const val VALIDITY_MILLIS = 24L * 60 * 60 * 1000
        private const val SERIAL_BITS = 64
    }
}
