package com.woocommerce.android.notifications.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushIdentityStoreTest : BaseUnitTest(StandardTestDispatcher()) {
    private var dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore()
    private val tokenProvider: FirebaseMessagingTokenProvider = mock()

    @Test
    fun `given stored token cannot be read, when getting current token, then returns null`() = testBlocking {
        dataStore = mock {
            on { data } doReturn flow { throw IOException("read failed") }
        }

        assertThat(newStore().currentTokenOrNull()).isNull()
        verify(tokenProvider, never()).getToken()
    }

    @Test
    fun `given empty install, when preparing again after recreation, then reuses the persisted UUID and token`() =
        testBlocking {
            whenever(tokenProvider.getToken()).thenReturn(TOKEN)

            val identity = newStore().prepareRegistration()

            assertThat(identity.uuid).isNotBlank()
            assertThat(identity.token).isEqualTo(TOKEN)
            assertThat(identity.needsFullRegistration).isTrue()
            assertThat(newStore().prepareRegistration()).isEqualTo(identity)
            verify(tokenProvider).getToken()
        }

    @Test
    fun `given token fetch fails, when retried, then reuses the UUID persisted before failure`() = testBlocking {
        whenever(tokenProvider.getToken()).thenThrow(IllegalStateException("temporary failure"))
        assertThat(runCatching { newStore().prepareRegistration() }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
        val uuid = newStore().currentUuidOrNull()
        assertThat(uuid).isNotBlank()
        assertThat(dataStore.data.first()[PENDING]).isTrue()
        doReturn(TOKEN).whenever(tokenProvider).getToken()

        val identity = newStore().prepareRegistration()

        assertThat(identity.uuid).isEqualTo(uuid)
        assertThat(identity.needsFullRegistration).isTrue()
    }

    @Test
    fun `given empty SDK token, when preparing, then rejects it and leaves registration pending`() = testBlocking {
        whenever(tokenProvider.getToken()).thenReturn("")

        val failure = runCatching { newStore().prepareRegistration() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(dataStore.data.first()[PENDING]).isTrue()
        assertThat(newStore().currentTokenOrNull()).isNull()
    }

    @Test
    fun `given empty install, when token callback arrives, then persists identity without SDK fetch`() = testBlocking {
        newStore().onNewFcmToken(TOKEN)

        val identity = newStore().prepareRegistration()
        assertThat(identity.uuid).isNotBlank()
        assertThat(identity.token).isEqualTo(TOKEN)
        assertThat(identity.needsFullRegistration).isTrue()
        verify(tokenProvider, never()).getToken()
    }

    @Test
    fun `given existing per-site registration without UUID, when preparing, then preserves it and makes registration pending`() =
        testBlocking {
            dataStore = InMemoryPreferencesDataStore(
                preferencesOf(
                    stringPreferencesKey("push_token_123") to "registration-id",
                    stringPreferencesKey("push_token_value_123") to "fcm-token",
                    stringPreferencesKey("push_locale_123") to "en_US"
                )
            )
            whenever(tokenProvider.getToken()).thenReturn(TOKEN)

            val identity = newStore().prepareRegistration()
            val persisted = dataStore.data.first()

            assertThat(identity.uuid).isNotBlank()
            assertThat(identity.needsFullRegistration).isTrue()
            assertThat(persisted[stringPreferencesKey("push_token_123")]).isEqualTo("registration-id")
            assertThat(persisted[stringPreferencesKey("push_token_value_123")]).isEqualTo("fcm-token")
            assertThat(persisted[stringPreferencesKey("push_locale_123")]).isEqualTo("en_US")
        }

    @Test
    fun `given completed registration, when token changes, then keeps UUID and makes registration pending`() = testBlocking {
        seed(pending = false)

        newStore().onNewFcmToken("new-token")

        assertThat(newStore().prepareRegistration())
            .isEqualTo(WooPushIdentityStore.RegistrationIdentity(UUID, "new-token", true))
        verify(tokenProvider, never()).getToken()
    }

    @Test
    fun `given existing identity, when empty callback arrives, then leaves persisted state unchanged`() = testBlocking {
        seed(pending = false)
        val before = dataStore.data.first()

        val failure = runCatching { newStore().onNewFcmToken("") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(dataStore.data.first()).isEqualTo(before)
    }

    @Test
    fun `given completed registration, when forced SDK refresh changes token, then registration becomes pending`() =
        testBlocking {
            seed(pending = false)
            whenever(tokenProvider.getToken()).thenReturn("new-token")

            assertThat(newStore().prepareRegistration(forceRefresh = true))
                .isEqualTo(WooPushIdentityStore.RegistrationIdentity(UUID, "new-token", true))
        }

    @Test
    fun `given completed registration, when forced SDK refresh returns same token, then completion is preserved`() =
        testBlocking {
            seed(pending = false)
            whenever(tokenProvider.getToken()).thenReturn(TOKEN)

            assertThat(newStore().prepareRegistration(forceRefresh = true).needsFullRegistration).isFalse()
        }

    @Test
    fun `given pending registration, when matching identity completes, then completion survives recreation`() = testBlocking {
        seed(pending = true)

        newStore().markCoreRegistrationComplete(UUID, TOKEN)

        assertThat(newStore().prepareRegistration())
            .isEqualTo(WooPushIdentityStore.RegistrationIdentity(UUID, TOKEN, false))
        verify(tokenProvider, never()).getToken()
    }

    @Test
    fun `given token changes during registration, when completions use a wrong UUID or stale token, then remains pending`() =
        testBlocking {
            seed(pending = true)
            val store = newStore()
            store.onNewFcmToken(CALLBACK_TOKEN)

            store.markCoreRegistrationComplete("wrong-uuid", CALLBACK_TOKEN)
            store.markCoreRegistrationComplete(UUID, TOKEN)

            assertThat(newStore().prepareRegistration())
                .isEqualTo(WooPushIdentityStore.RegistrationIdentity(UUID, CALLBACK_TOKEN, true))
        }

    @Test
    fun `given callback token changes during SDK fetch, when preparation resumes, then preserves callback token`() =
        testBlocking {
            val releaseToken = CompletableDeferred<String>()
            whenever(tokenProvider.getToken()).doSuspendableAnswer { releaseToken.await() }
            val store = newStore()
            val preparation = async { store.prepareRegistration() }
            runCurrent()
            val uuid = requireNotNull(store.currentUuidOrNull())

            store.onNewFcmToken(CALLBACK_TOKEN)

            releaseToken.complete(TOKEN)

            assertThat(preparation.await())
                .isEqualTo(WooPushIdentityStore.RegistrationIdentity(uuid, CALLBACK_TOKEN, true))
            assertThat(store.currentTokenOrNull()).isEqualTo(CALLBACK_TOKEN)
        }

    @Test
    fun `given overlapping preparations, when token fetch suspends, then both use one UUID`() = testBlocking {
        val releaseToken = CompletableDeferred<String>()
        whenever(tokenProvider.getToken()).doSuspendableAnswer { releaseToken.await() }
        val store = newStore()
        val first = async { store.prepareRegistration() }
        runCurrent()
        val second = async { store.prepareRegistration() }
        runCurrent()

        releaseToken.complete(TOKEN)

        assertThat(first.await().uuid).isEqualTo(second.await().uuid)
        assertThat(store.currentUuidOrNull()).isEqualTo(first.await().uuid)
    }

    private fun seed(pending: Boolean) {
        dataStore = InMemoryPreferencesDataStore(
            preferencesOf(UUID_KEY to UUID, TOKEN_KEY to TOKEN, PENDING to pending)
        )
    }

    private fun newStore() = WooPushIdentityStore(dataStore, tokenProvider)

    private class InMemoryPreferencesDataStore(
        initial: Preferences = emptyPreferences()
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = mutex.withLock { transform(state.value).also { state.value = it } }
    }

    private companion object {
        const val UUID = "installation-uuid"
        const val TOKEN = "sdk-token"
        const val CALLBACK_TOKEN = "callback-token"
        val UUID_KEY = stringPreferencesKey("woo_core_uuid")
        val TOKEN_KEY = stringPreferencesKey("push_identity_fcm_token")
        val PENDING = booleanPreferencesKey("push_identity_full_registration_pending")
    }
}
