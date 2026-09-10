package com.woocommerce.android.notifications.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType.WOO_CORE_PUSH_NOTIFICATIONS_TOKENS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPushIdentityStore @Inject constructor(
    @DataStoreQualifier(WOO_CORE_PUSH_NOTIFICATIONS_TOKENS)
    private val pushNotificationsDataStore: DataStore<Preferences>,
    private val appPrefs: AppPrefsWrapper,
    private val firebaseMessagingTokenProvider: FirebaseMessagingTokenProvider
) {
    private val mutex = Mutex()

    suspend fun prepareRegistration(forceRefresh: Boolean = false): RegistrationIdentity {
        val tokenBeforeFetch = mutex.withLock {
            val state = pushNotificationsDataStore.data.first()
            if (state[WOO_CORE_UUID] == null) createIdentity()
            state[FCM_TOKEN]
        }
        val candidateToken = if (tokenBeforeFetch == null || forceRefresh) {
            firebaseMessagingTokenProvider.getToken().also { require(it.isNotEmpty()) }
        } else {
            tokenBeforeFetch
        }

        return mutex.withLock {
            val state = pushNotificationsDataStore.data.first()
            val uuid = requireNotNull(state[WOO_CORE_UUID])
            val currentStoredToken = state[FCM_TOKEN]
            val token = if (currentStoredToken != null && currentStoredToken != tokenBeforeFetch) {
                currentStoredToken
            } else {
                candidateToken
            }
            val needsFullRegistration = state[FULL_REGISTRATION_PENDING] == true || token != currentStoredToken

            if (token != currentStoredToken) {
                saveIdentity(uuid, token, needsFullRegistration)
            }
            appPrefs.setFCMToken(token)
            RegistrationIdentity(uuid, token, needsFullRegistration)
        }
    }

    suspend fun onNewFcmToken(token: String) = mutex.withLock {
        require(token.isNotEmpty())
        val state = pushNotificationsDataStore.data.first()
        val uuid = state[WOO_CORE_UUID] ?: UUID.randomUUID().toString()
        saveIdentity(uuid, token, pending = true)
        appPrefs.setFCMToken(token)
    }

    suspend fun currentUuidOrNull(): String? = pushNotificationsDataStore.data.first()[WOO_CORE_UUID]

    suspend fun currentTokenOrNull(): String? = pushNotificationsDataStore.data.first()[FCM_TOKEN]

    suspend fun markCoreRegistrationComplete(uuid: String, token: String) = mutex.withLock {
        val state = pushNotificationsDataStore.data.first()
        if (
            state[FULL_REGISTRATION_PENDING] == true &&
            state[WOO_CORE_UUID] == uuid &&
            state[FCM_TOKEN] == token
        ) {
            saveIdentity(uuid, token, pending = false)
        }
    }

    private suspend fun createIdentity() {
        val uuid = UUID.randomUUID().toString()
        pushNotificationsDataStore.edit { preferences ->
            preferences[WOO_CORE_UUID] = uuid
            preferences[FULL_REGISTRATION_PENDING] = true
        }
    }

    private suspend fun saveIdentity(uuid: String, token: String, pending: Boolean) {
        pushNotificationsDataStore.edit { preferences ->
            preferences[WOO_CORE_UUID] = uuid
            preferences[FCM_TOKEN] = token
            preferences[FULL_REGISTRATION_PENDING] = pending
        }
    }

    data class RegistrationIdentity(
        val uuid: String,
        val token: String,
        val needsFullRegistration: Boolean
    )

    companion object {
        internal val WOO_CORE_UUID = stringPreferencesKey("woo_core_uuid")
        private val FCM_TOKEN = stringPreferencesKey("push_identity_fcm_token")
        private val FULL_REGISTRATION_PENDING = booleanPreferencesKey("push_identity_full_registration_pending")
    }
}
