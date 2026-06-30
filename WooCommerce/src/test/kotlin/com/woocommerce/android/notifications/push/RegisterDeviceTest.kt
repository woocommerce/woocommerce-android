package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.push.RegisterDevice.Trigger.APP_FOREGROUND
import com.woocommerce.android.notifications.push.RegisterDevice.Trigger.LOGIN_SUCCESS
import com.woocommerce.android.notifications.push.RegisterDevice.Trigger.SITE_SWITCH
import com.woocommerce.android.notifications.push.RegisterDevice.Trigger.TOKEN_REFRESH
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.sitevisibility.GetWooVisibleSites
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalCoroutinesApi
class RegisterDeviceTest : BaseUnitTest(StandardTestDispatcher()) {
    private lateinit var sut: RegisterDevice

    private val appPrefs: AppPrefsWrapper = mock {
        on { getFCMToken() } doReturn TEST_TOKEN
    }
    private val accountStore: AccountStore = mock {
        on { hasAccessToken() } doReturn true
    }
    private val pushNotificationRepository: PushNotificationRepository = mock {
        on { shouldRegisterWooPushForSite(any(), any()) } doReturn true
    }
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1) } doReturn true
    }
    private val selectedSiteModel: SiteModel = mock {
        on { siteId } doReturn SELECTED_SITE_ID
    }
    private val siteOne: SiteModel = mock {
        on { siteId } doReturn SITE_ID_ONE
    }
    private val siteTwo: SiteModel = mock {
        on { siteId } doReturn SITE_ID_TWO
    }
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() } doReturn selectedSiteModel
    }
    private val getWooVisibleSites: GetWooVisibleSites = mock {
        on { invoke() } doReturn listOf(siteOne, siteTwo)
    }
    private val appCoroutineScope by lazy { CoroutineScope(coroutinesTestRule.testDispatcher) }

    @Before
    fun setUp() {
        runBlocking {
            whenever(pushNotificationRepository.clearWooPushRegistrationsForStaleToken(any())).thenReturn(emptySet())
            whenever(pushNotificationRepository.getWooPushRegisteredSiteIds()).thenReturn(emptySet())
            whenever(pushNotificationRepository.registerPushTokenInWpComSystem(any())).thenReturn(
                WpComPushNotificationStore.RegisterDeviceResponsePayload(deviceId = "device-id-123")
            )
        }
        sut = RegisterDevice(
            appPrefsWrapper = appPrefs,
            accountStore = accountStore,
            pushNotificationRepository = pushNotificationRepository,
            featureFlagRepository = featureFlagRepository,
            selectedSite = selectedSite,
            getWooVisibleSites = getWooVisibleSites,
            appCoroutineScope = appCoroutineScope
        )
    }

    @Test
    fun `given no FCM token, when app foreground trigger runs, then does not register`() = testBlocking {
        // GIVEN
        whenever(appPrefs.getFCMToken()).thenReturn("")

        // WHEN
        sut(APP_FOREGROUND)

        // THEN
        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(eq(TEST_TOKEN), any(), any())
    }

    @Test
    fun `given app foreground trigger, when selected site exists, then evaluates only selected site`() = testBlocking {
        // WHEN
        sut(APP_FOREGROUND)

        // THEN
        verify(selectedSite).getIfExists()
        verify(pushNotificationRepository).shouldRegisterWooPushForSite(TEST_TOKEN, SELECTED_SITE_ID)
        verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel)
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_ONE)
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_TWO)
        verify(getWooVisibleSites, never()).invoke()
    }

    @Test
    fun `given site switch trigger, when selected site exists, then evaluates only selected site and skips wpcom`() = testBlocking {
        // WHEN
        sut(SITE_SWITCH)

        // THEN
        val fallbackCaptor = argumentCaptor<Boolean>()
        verify(selectedSite).getIfExists()
        verify(pushNotificationRepository).shouldRegisterWooPushForSite(TEST_TOKEN, SELECTED_SITE_ID)
        verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(
            token = eq(TEST_TOKEN),
            selectedSite = eq(selectedSiteModel),
            allowWpComFallback = fallbackCaptor.capture()
        )
        assertThat(fallbackCaptor.firstValue).isFalse()
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_ONE)
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_TWO)
        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(getWooVisibleSites, never()).invoke()
    }

    @Test
    fun `given token refresh trigger, when registration runs, then forces Woo and WPCom registration regardless of current state`() =
        testBlocking {
            // WHEN
            sut(TOKEN_REFRESH)

            // THEN
            verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_ONE)
            verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_TWO)
            verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteOne)
            verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteTwo)
            verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
        }

    @Test
    fun `given login success trigger, when WPCom is not registered, then registers in WPCom`() = testBlocking {
        // GIVEN
        runBlocking { whenever(pushNotificationRepository.isWpComPushRegistered()).thenReturn(false) }

        // WHEN
        sut(LOGIN_SUCCESS)

        // THEN
        verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given app foreground trigger, when WPCom is already registered, then skips WPCom registration`() = testBlocking {
        // GIVEN
        runBlocking { whenever(pushNotificationRepository.isWpComPushRegistered()).thenReturn(true) }

        // WHEN
        sut(APP_FOREGROUND)

        // THEN
        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given no access token, when app foreground trigger runs, then does not register in WPCom`() = testBlocking {
        // GIVEN
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        runBlocking {
            whenever(pushNotificationRepository.isWpComPushRegistered()).thenReturn(false)
        }

        // WHEN
        sut(APP_FOREGROUND)

        // THEN
        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given M1 flag disabled, when app foreground trigger runs, then skips Woo registration`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1)).thenReturn(false)

        // WHEN
        sut(APP_FOREGROUND)

        // THEN
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_ONE)
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_TWO)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(eq(TEST_TOKEN), any(), any())
    }

    @Test
    fun `given M1 flag disabled, when token refresh trigger runs, then clears stale Woo registrations`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)

            // WHEN
            sut(TOKEN_REFRESH)

            // THEN
            verify(pushNotificationRepository).clearWooPushRegistrationsForStaleToken(TEST_TOKEN)
            verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(eq(TEST_TOKEN), any(), any())
            verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
        }

    @Test
    fun `given stale visible Woo registration, when token refresh registers in WPCom, then enables WPCom notifications`() =
        testBlocking {
            // GIVEN
            whenever(pushNotificationRepository.clearWooPushRegistrationsForStaleToken(TEST_TOKEN))
                .thenReturn(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))

            // WHEN
            sut(TOKEN_REFRESH)

            // THEN
            verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
            verify(pushNotificationRepository).enableWpComNotificationsForSites(setOf(SITE_ID_ONE))
        }

    @Test
    fun `given stale visible site is still Woo registered, when WPCom registration succeeds, then does not enable WPCom notifications`() =
        testBlocking {
            // GIVEN
            whenever(pushNotificationRepository.clearWooPushRegistrationsForStaleToken(TEST_TOKEN))
                .thenReturn(setOf(SITE_ID_ONE))
            whenever(pushNotificationRepository.getWooPushRegisteredSiteIds()).thenReturn(setOf(SITE_ID_ONE))

            // WHEN
            sut(TOKEN_REFRESH)

            // THEN
            verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
            verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
        }

    @Test
    fun `given stale visible Woo registration, when WPCom token refresh registration fails, then does not enable WPCom notifications`() =
        testBlocking {
            // GIVEN
            whenever(pushNotificationRepository.clearWooPushRegistrationsForStaleToken(TEST_TOKEN))
                .thenReturn(setOf(SITE_ID_ONE))
            whenever(pushNotificationRepository.registerPushTokenInWpComSystem(TEST_TOKEN)).thenReturn(
                WpComPushNotificationStore.RegisterDeviceResponsePayload(
                    WpComPushNotificationStore.DeviceRegistrationError()
                )
            )

            // WHEN
            sut(TOKEN_REFRESH)

            // THEN
            verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
            verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
        }

    @Test
    fun `given app foreground trigger, when Woo registration is unchanged for one site, then registers only stale sites`() =
        testBlocking {
            // GIVEN
            whenever(
                pushNotificationRepository.shouldRegisterWooPushForSite(TEST_TOKEN, SELECTED_SITE_ID)
            ).thenReturn(false)

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel)
        }

    @Test
    fun `given app foreground trigger, when no selected site exists, then Woo registration is skipped`() = testBlocking {
        // GIVEN
        whenever(selectedSite.getIfExists()).thenReturn(null)

        // WHEN
        sut(APP_FOREGROUND)

        // THEN
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(eq(TEST_TOKEN), any())
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(eq(TEST_TOKEN), any(), any())
        verify(getWooVisibleSites, never()).invoke()
    }

    @Test
    fun `given site switch trigger, when no selected site exists, then does not attempt registration`() = testBlocking {
        // GIVEN
        whenever(selectedSite.getIfExists()).thenReturn(null)

        // WHEN
        sut(SITE_SWITCH)

        // THEN
        verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(eq(TEST_TOKEN), any())
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(eq(TEST_TOKEN), any(), any())
        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given unforced run in progress, when site switch is kicked off, then it waits for unforced run to finish`() =
        testBlocking {
            // GIVEN
            runBlocking {
                whenever(
                    pushNotificationRepository.registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, true)
                ).doSuspendableAnswer {
                    delay(1000.milliseconds)
                    Result.success(Unit)
                }
            }

            // WHEN
            sut.kickoff(APP_FOREGROUND)
            runCurrent()

            val queuedTrigger = launch {
                sut.kickoff(SITE_SWITCH)
            }
            runCurrent()

            // THEN
            verify(
                pushNotificationRepository,
                times(1)
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, true)
            verify(
                pushNotificationRepository,
                never()
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)

            // WHEN
            advanceTimeBy(1001.milliseconds)
            queuedTrigger.join()
            advanceUntilIdle()

            // THEN
            verify(
                pushNotificationRepository,
                times(1)
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)
        }

    @Test
    fun `given unforced run in progress, when token refresh is kicked off, then it cancels the unforced run and restarts with force`() =
        testBlocking {
            // GIVEN
            var isFirstCall = true
            runBlocking {
                whenever(
                    pushNotificationRepository.registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, true)
                ).doSuspendableAnswer {
                    if (isFirstCall) {
                        isFirstCall = false
                        delay(Long.MAX_VALUE.milliseconds) // Suspend indefinitely until cancelled
                    }
                    Result.success(Unit)
                }
            }

            // WHEN
            sut.kickoff(APP_FOREGROUND)
            runCurrent()

            sut.kickoff(TOKEN_REFRESH)
            advanceUntilIdle()

            // THEN
            verify(pushNotificationRepository, times(1)).shouldRegisterWooPushForSite(TEST_TOKEN, SELECTED_SITE_ID)
            verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_ONE)
            verify(pushNotificationRepository, never()).shouldRegisterWooPushForSite(TEST_TOKEN, SITE_ID_TWO)
            verify(
                pushNotificationRepository,
                times(1)
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, true)
            verify(pushNotificationRepository, atLeast(1)).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteOne, true)
            verify(pushNotificationRepository, atLeast(1)).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteTwo, true)
            verify(pushNotificationRepository, times(1)).registerPushTokenInWpComSystem(TEST_TOKEN)
        }

    companion object {
        private const val TEST_TOKEN = "test-fcm-token-123"
        private const val SELECTED_SITE_ID = 123L
        private const val SITE_ID_ONE = 456L
        private const val SITE_ID_TWO = 789L
        private const val HIDDEN_SITE_ID = 999L
    }
}
