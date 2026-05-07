package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
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

internal class CardReaderRemoteTlsClient(
    private val logWrapper: LogWrapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun connect(
        host: InetAddress,
        port: Int,
        pinnedFingerprintBase64: String,
    ): CardReaderRemoteConnection = withContext(ioDispatcher) {
        logWrapper.d(TAG, "Connecting TLS to ${host.hostAddress}:$port")
        val trustManager = PinnedFingerprintTrustManager(pinnedFingerprintBase64)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        val socket = (sslContext.socketFactory.createSocket(host, port) as SSLSocket).apply {
            soTimeout = SESSION_READ_TIMEOUT_MILLIS
        }
        logWrapper.d(TAG, "TCP connected, starting handshake")
        socket.startHandshake()
        logWrapper.d(TAG, "Handshake complete")
        CardReaderRemoteConnection(socket = socket, logWrapper = logWrapper, ioDispatcher = ioDispatcher)
    }

    internal class PinnedFingerprintTrustManager(
        private val pinnedFingerprintBase64: String,
    ) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
            throw CertificateException("Client authentication not supported")
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            val leaf = chain.firstOrNull() ?: throw CertificateException("Empty certificate chain")
            val presented = CardReaderRemoteFingerprint.sha256Base64(leaf)
            if (presented != pinnedFingerprintBase64) {
                throw CertificateException(
                    "Server fingerprint mismatch: " +
                        "pinned=${pinnedFingerprintBase64.takeLast(FINGERPRINT_LOG_SUFFIX_LENGTH)} " +
                        "presented=${presented.takeLast(FINGERPRINT_LOG_SUFFIX_LENGTH)}"
                )
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    companion object {
        private const val SESSION_READ_TIMEOUT_MILLIS = 90_000
        private const val TAG = "CardReaderRemoteTlsClient"
        private const val FINGERPRINT_LOG_SUFFIX_LENGTH = 8
    }
}
