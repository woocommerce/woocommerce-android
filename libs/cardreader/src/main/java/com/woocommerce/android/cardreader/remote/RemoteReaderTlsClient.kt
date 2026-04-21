package com.woocommerce.android.cardreader.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class RemoteReaderTlsClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun connect(
        host: InetAddress,
        port: Int,
        pinnedFingerprintBase64: String,
    ): RemoteReaderConnection = withContext(ioDispatcher) {
        val trustManager = PinnedFingerprintTrustManager(pinnedFingerprintBase64)
        val sslContext = SSLContext.getInstance("TLSv1.3").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        val socket = (sslContext.socketFactory.createSocket(host, port) as SSLSocket).apply {
            soTimeout = SESSION_READ_TIMEOUT_MILLIS
        }
        socket.startHandshake()
        RemoteReaderConnection(socket, ioDispatcher = ioDispatcher)
    }

    private class PinnedFingerprintTrustManager(
        private val pinnedFingerprintBase64: String,
    ) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
            throw CertificateException("Client authentication not supported")
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            val leaf = chain.firstOrNull() ?: throw CertificateException("Empty certificate chain")
            val presented = RemoteReaderFingerprint.sha256Base64(leaf)
            if (presented != pinnedFingerprintBase64) {
                throw CertificateException("Server fingerprint mismatch")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    companion object {
        private const val SESSION_READ_TIMEOUT_MILLIS = 90_000
    }
}
