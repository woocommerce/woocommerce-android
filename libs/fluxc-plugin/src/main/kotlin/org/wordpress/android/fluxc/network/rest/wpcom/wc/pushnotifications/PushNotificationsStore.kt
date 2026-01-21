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
            persistPushTokenId(site, payload.result.id)
            WooResult(Unit)
        }
    }

    suspend fun deletePushToken(
        site: SiteModel
    ): WooResult<Unit> = coroutineEngine.withDefaultContext(T.API, this, "deletePushToken") {
        getPersistedPushTokenId(site)?.let { pushTokenId ->
            val result = pushNotificationsRestClient.deletePushToken(site, pushTokenId).asWooResult()
            if (!result.isError) {
                clearPersistedPushTokenId(site)
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
    private fun getPersistedPushTokenId(site: SiteModel): String? = getPersistedTokenIdSet()
        .firstOrNull { it.startsWith(getSiteTokenIdPrefix(site)) }
        ?.substringAfter(getSiteTokenIdPrefix(site))

    @Synchronized
    private fun persistPushTokenId(site: SiteModel, tokenId: String) {
        val persistedPushTokenIds = getPersistedTokenIdSet()

        val updatedPushTokenIds =
            persistedPushTokenIds.filterNot { it.startsWith(getSiteTokenIdPrefix(site)) }.toMutableSet()
        updatedPushTokenIds.add(getSiteTokenIdPrefix(site) + tokenId)

        if (persistedPushTokenIds != updatedPushTokenIds) {
            preferences.edit { putStringSet(PUSH_TOKEN_IDS, updatedPushTokenIds) }
        }
    }

    @Synchronized
    private fun clearPersistedPushTokenId(site: SiteModel) {
        val persistedPushTokens = getPersistedTokenIdSet()

        val updatedPushTokens = persistedPushTokens.filterNot { it.startsWith(getSiteTokenIdPrefix(site)) }.toSet()

        if (persistedPushTokens != updatedPushTokens) {
            preferences.edit { putStringSet(PUSH_TOKEN_IDS, updatedPushTokens) }
        }
    }

    private fun getPersistedTokenIdSet() = preferences.getStringSet(PUSH_TOKEN_IDS, null) ?: setOf()

    private fun getSiteTokenIdPrefix(site: SiteModel) = "${site.id}:"

    fun hasPushToken(site: SiteModel): Boolean = getPersistedPushTokenId(site) != null

    companion object {
        private const val ORIGIN = "com.woocommerce.android"
        private const val ORIGIN_DEV = "com.woocommerce.android:dev"
        private const val PUSH_TOKEN_IDS = "push_token_ids"
    }
}
