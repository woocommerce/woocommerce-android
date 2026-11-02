package com.woocommerce.android.ui.login.storesuggestion

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

class InstallReferrerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    suspend fun fetch(): InstallReferrerResult =
        debugOverride()
            ?: withTimeoutOrNull(TIMEOUT_MILLIS) { connectAndRead() }
            ?: InstallReferrerResult.Unavailable

    private fun debugOverride(): InstallReferrerResult? {
        if (!PackageUtils.isDebugBuild()) return null
        val referrer = appPrefsWrapper.debugInstallReferrerOverride.takeIf { it.isNotEmpty() } ?: return null
        return InstallReferrerResult.Available(
            referrer = referrer,
            clickTimestampSeconds = appPrefsWrapper.debugInstallReferrerClickTimestampSeconds,
        )
    }

    private suspend fun connectAndRead(): InstallReferrerResult = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()
        continuation.invokeOnCancellation { client.endConnection() }

        fun finish(result: InstallReferrerResult) {
            client.endConnection()
            if (continuation.isActive) continuation.resume(result)
        }

        val listener = object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                finish(resultForResponseCode(responseCode) { readReferrer(client) })
            }

            override fun onInstallReferrerServiceDisconnected() {
                finish(InstallReferrerResult.Unavailable)
            }
        }

        try {
            client.startConnection(listener)
        } catch (e: SecurityException) {
            WooLog.e(WooLog.T.LOGIN, "Install referrer connection failed", e)
            finish(InstallReferrerResult.Error)
        }
    }

    private fun readReferrer(client: InstallReferrerClient): InstallReferrerResult = try {
        val details = client.installReferrer
        InstallReferrerResult.Available(
            referrer = details.installReferrer.orEmpty(),
            clickTimestampSeconds = details.referrerClickTimestampSeconds,
        )
    } catch (e: RemoteException) {
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

sealed interface InstallReferrerResult {
    data class Available(val referrer: String, val clickTimestampSeconds: Long) : InstallReferrerResult
    data object Unavailable : InstallReferrerResult
    data object Error : InstallReferrerResult
}
