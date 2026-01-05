package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import androidx.core.content.edit
import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationsStore @Inject internal constructor(
    private val pushNotificationsRestClient: PushNotificationsRestClient,
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    private val coroutineEngine: CoroutineEngine,
) {
    private val preferences by lazy { prefsWrapper.getFluxCPreferences() }

    suspend fun registerPushToken(
        site: SiteModel,
        token: String,
        deviceUuid: String
    ): WooResult<Unit> = coroutineEngine.withDefaultContext(T.API, this, "registerPushToken") {
        val origin = if (BuildConfig.DEBUG) ORIGIN_DEV else ORIGIN
        val payload = pushNotificationsRestClient.registerPushToken(site, token, origin, deviceUuid)

        if (payload.isError || payload.result == null) {
            WooResult(payload.error)
        } else {
            persistPushTokenId(payload.result.id)
            WooResult(Unit)
        }
    }

    suspend fun deletePushToken(
        site: SiteModel
    ): WooResult<Unit> = coroutineEngine.withDefaultContext(T.API, this, "deletePushToken") {
        getPersistedPushTokenId()?.let { pushTokenId ->
            val result = pushNotificationsRestClient.deletePushToken(site, pushTokenId).asWooResult()
            if (!result.isError) {
                preferences.edit { remove(PUSH_TOKEN_ID) }
            }
            result
        } ?: WooResult(
            WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.NOT_FOUND,
                "No persisted push token id found"
            )
        )
    }

    @Synchronized
    private fun getPersistedPushTokenId(): String? = preferences.getString(PUSH_TOKEN_ID, null)

    @Synchronized
    private fun persistPushTokenId(tokenId: String) {
        if (getPersistedPushTokenId() != tokenId) {
            preferences.edit { putString(PUSH_TOKEN_ID, tokenId) }
        }
    }

    fun hasPushToken(): Boolean = getPersistedPushTokenId() != null

    companion object {
        private const val ORIGIN = "com.woocommerce.android"
        private const val ORIGIN_DEV = "com.woocommerce.android:dev"
        private const val PUSH_TOKEN_ID = "push_token_id"
    }
}
