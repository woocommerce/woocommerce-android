package com.woocommerce.android.ui.login.storesuggestion

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume

class InstallReferrerSource @Inject constructor(
    private val clientFactory: InstallReferrerClientFactory,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend fun fetch(): InstallReferrerResult =
        debugOverride()
            ?: withTimeoutOrNull(TIMEOUT_MILLIS) { connectAndRead() }
            ?: InstallReferrerResult.Unavailable

    private fun debugOverride(): InstallReferrerResult? {
        if (!PackageUtils.isDebugBuild()) return null
        val referrer = appPrefsWrapper.debugInstallReferrerOverride.takeIf { it.isNotEmpty() } ?: return null
        val clickTimestampSeconds = appPrefsWrapper.debugInstallReferrerClickTimestampSeconds
        return InstallReferrerResult.Available(
            referrer = referrer,
            clickTimestampSeconds = clickTimestampSeconds,
            installBeginTimestampSeconds = clickTimestampSeconds,
        )
    }

    private suspend fun connectAndRead(): InstallReferrerResult = suspendCancellableCoroutine { continuation ->
        val client = clientFactory.create()
        continuation.invokeOnCancellation { client.endConnection() }

        val isFinished = AtomicBoolean(false)
        fun finish(result: InstallReferrerResult) {
            if (!isFinished.compareAndSet(false, true)) return
            client.endConnection()
            if (continuation.isActive) continuation.resume(result)
        }

        fun onConnectionFailed(e: RuntimeException) {
            WooLog.e(WooLog.T.LOGIN, "Install referrer connection failed", e)
            finish(InstallReferrerResult.Error)
        }

        val listener = object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                dispatchers.io.asExecutor().execute {
                    if (isFinished.get()) return@execute
                    finish(resultForResponseCode(responseCode) { readReferrer(client) })
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                finish(InstallReferrerResult.Unavailable)
            }
        }

        try {
            client.startConnection(listener)
        } catch (e: SecurityException) {
            onConnectionFailed(e)
        } catch (e: IllegalStateException) {
            onConnectionFailed(e)
        } catch (e: IllegalArgumentException) {
            onConnectionFailed(e)
        }
    }

    private fun readReferrer(client: InstallReferrerClient): InstallReferrerResult = try {
        val details = client.installReferrer
        InstallReferrerResult.Available(
            referrer = details.installReferrer.orEmpty(),
            clickTimestampSeconds = details.referrerClickTimestampSeconds,
            installBeginTimestampSeconds = details.installBeginTimestampSeconds,
        )
    } catch (e: RemoteException) {
        WooLog.e(WooLog.T.LOGIN, "Install referrer read failed", e)
        InstallReferrerResult.Error
    } catch (e: IllegalStateException) {
        WooLog.e(WooLog.T.LOGIN, "Install referrer read failed", e)
        InstallReferrerResult.Error
    }

    companion object {
        private const val TIMEOUT_MILLIS = 3_000L

        internal fun resultForResponseCode(
            responseCode: Int,
            readReferrer: () -> InstallReferrerResult,
        ): InstallReferrerResult = when (responseCode) {
            InstallReferrerResponse.OK -> readReferrer()
            InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
            InstallReferrerResponse.SERVICE_UNAVAILABLE -> InstallReferrerResult.Unavailable
            else -> InstallReferrerResult.Error
        }
    }
}

class InstallReferrerClientFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun create(): InstallReferrerClient = InstallReferrerClient.newBuilder(context).build()
}

sealed interface InstallReferrerResult {
    data class Available(
        val referrer: String,
        val clickTimestampSeconds: Long,
        val installBeginTimestampSeconds: Long,
    ) : InstallReferrerResult
    data object Unavailable : InstallReferrerResult
    data object Error : InstallReferrerResult
}
