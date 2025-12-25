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
            persistPushToken(site, payload.result.id)
            WooResult(Unit)
        }
    }

    suspend fun deletePushToken(
        site: SiteModel
    ): WooResult<Unit> = coroutineEngine.withDefaultContext(T.API, this, "deletePushToken") {
        getPersistedPushToken(site)?.let { pushTokenId ->
            val result = pushNotificationsRestClient.deletePushToken(site, pushTokenId).asWooResult()
            if (!result.isError) {
                clearPersistedPushToken(site)
            }
            result
        } ?: WooResult(
            WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.NOT_FOUND,
                "No persisted push token found for site ${site.id}"
            )
        )
    }

    @Synchronized
    private fun getPersistedPushToken(site: SiteModel): Int? = getPersistedTokenSet()
        .firstOrNull { it.startsWith(getSiteTokenPrefix(site)) }
        ?.substringAfter(getSiteTokenPrefix(site))
        ?.toIntOrNull()

    @Synchronized
    private fun persistPushToken(site: SiteModel, tokenId: Int) {
        val persistedPushTokens = getPersistedTokenSet()

        val updatedPushTokens = persistedPushTokens.filterNot { it.startsWith(getSiteTokenPrefix(site)) }.toMutableSet()
        updatedPushTokens.add(getSiteTokenPrefix(site) + tokenId)

        if (persistedPushTokens != updatedPushTokens) {
            preferences.edit { putStringSet(PUSH_TOKENS, updatedPushTokens) }
        }
    }

    @Synchronized
    private fun clearPersistedPushToken(site: SiteModel) {
        val persistedPushTokens = getPersistedTokenSet()

        val updatedPushTokens = persistedPushTokens.filterNot { it.startsWith(getSiteTokenPrefix(site)) }.toSet()

        if (persistedPushTokens != updatedPushTokens) {
            preferences.edit { putStringSet(PUSH_TOKENS, updatedPushTokens) }
        }
    }

    private fun getPersistedTokenSet() = preferences.getStringSet(PUSH_TOKENS, null) ?: setOf()

    private fun getSiteTokenPrefix(site: SiteModel) = "${site.id}:"

    companion object {
        private const val ORIGIN = "com.woocommerce.android"
        private const val ORIGIN_DEV = "com.woocommerce.android:dev"
        private const val PUSH_TOKENS = "push_tokens"
    }
}
