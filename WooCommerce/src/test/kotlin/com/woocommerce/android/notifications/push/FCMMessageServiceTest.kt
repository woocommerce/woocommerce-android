package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.push.RegisterDevice.Trigger.TOKEN_REFRESH
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.timeout
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.InvalidateDeviceRegistration

@OptIn(ExperimentalCoroutinesApi::class)
class FCMMessageServiceTest : BaseUnitTest() {
    private val identityStore: WooPushIdentityStore = mock()
    private val registerDevice: RegisterDevice = mock()
    private val invalidateDeviceRegistration: InvalidateDeviceRegistration = mock()

    private val sut = FCMMessageService().apply {
        this.identityStore = this@FCMMessageServiceTest.identityStore
        this.registerDevice = this@FCMMessageServiceTest.registerDevice
        this.invalidateDeviceRegistration = this@FCMMessageServiceTest.invalidateDeviceRegistration
    }

    @Test
    fun `when FCM token changes, then persists identity before invalidating and scheduling registration`() {
        val events = mutableListOf<String>()
        val registrationObservedPersistence = CompletableDeferred<Boolean>()
        runBlocking {
            doSuspendableAnswer {
                delay(PERSISTENCE_DELAY_MS)
                events += "persisted"
            }.whenever(identityStore).onNewFcmToken(TOKEN)
            doAnswer { events += "invalidated" }.whenever(invalidateDeviceRegistration).invoke()
            doSuspendableAnswer {
                registrationObservedPersistence.complete(events == listOf("persisted", "invalidated"))
                Unit
            }.whenever(registerDevice).invoke(TOKEN_REFRESH)
        }

        sut.onNewToken(TOKEN)

        runBlocking {
            verify(registerDevice, timeout(REGISTRATION_TIMEOUT_MS)).invoke(TOKEN_REFRESH)
            assertThat(registrationObservedPersistence.await()).isTrue()
        }
        assertThat(events).containsExactly("persisted", "invalidated")
    }

    @Test
    fun `given token persistence fails, when FCM token changes, then does not schedule registration`() {
        val exception = IllegalStateException("store unavailable")
        runBlocking {
            whenever(identityStore.onNewFcmToken(TOKEN)).thenThrow(exception)
        }

        val result = runCatching { sut.onNewToken(TOKEN) }

        verify(invalidateDeviceRegistration, never()).invoke()
        runBlocking {
            verify(registerDevice, never()).invoke(TOKEN_REFRESH)
        }
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    private companion object {
        const val TOKEN = "new-token"
        const val PERSISTENCE_DELAY_MS = 10L
        const val REGISTRATION_TIMEOUT_MS = 1_000L
    }
}
