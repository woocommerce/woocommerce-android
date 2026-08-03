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
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import javax.inject.Singleton
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
            lenient().doReturn(emptySet<Long>())
                .whenever(pushNotificationRepository).getWooPushRegisteredSiteIds()
            lenient().doReturn(WpComPushNotificationStore.RegisterDeviceResponsePayload(deviceId = "device-id-123"))
                .whenever(pushNotificationRepository).registerPushTokenInWpComSystem(any())
            lenient().doReturn(Result.success(Unit))
                .whenever(pushNotificationRepository).enableWpComNotificationsForSites(any())
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
        verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)
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
            verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteOne, false)
            verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteTwo, false)
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
    fun `given M1 flag enabled, when a site registers, then clears its stale registration before registering`() =
        testBlocking {
            // WHEN
            sut(TOKEN_REFRESH)

            // THEN
            val siteOneOrder = inOrder(pushNotificationRepository)
            siteOneOrder.verify(pushNotificationRepository)
                .clearWooPushRegistrationForStaleToken(SITE_ID_ONE, TEST_TOKEN)
            siteOneOrder.verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteOne, false)
            verify(pushNotificationRepository).clearWooPushRegistrationForStaleToken(SITE_ID_TWO, TEST_TOKEN)
        }

    @Test
    fun `given app foreground trigger, when a site does not need registration, then does not clear its registration`() =
        testBlocking {
            // GIVEN
            whenever(
                pushNotificationRepository.shouldRegisterWooPushForSite(TEST_TOKEN, SELECTED_SITE_ID)
            ).thenReturn(false)

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never())
                .clearWooPushRegistrationForStaleToken(eq(SELECTED_SITE_ID), any())
        }

    @Test
    fun `given M1 flag disabled, when registration runs, then does not clear stale Woo registrations`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).clearWooPushRegistrationForStaleToken(any(), any())
        }

    @Test
    fun `given M1 flag disabled and no Woo registrations, when registration runs, then does not migrate`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
            verify(pushNotificationRepository, never()).unregisterWooPushRegisteredSites(any())
        }

    @Test
    fun `given M1 flag disabled and visible jetpack site, when WPCom takes over, then unregisters all Woo sites`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)
            stubJetpackConnection(siteOne)
            runBlocking {
                whenever(pushNotificationRepository.getWooPushRegisteredSiteIds())
                    .thenReturn(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))
            }

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            val migrationOrder = inOrder(pushNotificationRepository)
            migrationOrder.verify(pushNotificationRepository).enableWpComNotificationsForSites(setOf(SITE_ID_ONE))
            migrationOrder.verify(pushNotificationRepository)
                .unregisterWooPushRegisteredSites(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))
        }

    @Test
    fun `given M1 flag disabled, when WPCom registration fails, then unregisters only sites without fallback`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)
            stubJetpackConnection(siteOne)
            runBlocking {
                whenever(pushNotificationRepository.getWooPushRegisteredSiteIds())
                    .thenReturn(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))
                whenever(pushNotificationRepository.registerPushTokenInWpComSystem(any())).thenReturn(
                    WpComPushNotificationStore.RegisterDeviceResponsePayload(
                        WpComPushNotificationStore.DeviceRegistrationError()
                    )
                )
            }

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
            verify(pushNotificationRepository).unregisterWooPushRegisteredSites(setOf(HIDDEN_SITE_ID))
        }

    @Test
    fun `given M1 flag disabled, when enabling WPCom notifications fails, then keeps fallback sites on Woo`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)
            stubJetpackConnection(siteOne)
            runBlocking {
                whenever(pushNotificationRepository.getWooPushRegisteredSiteIds())
                    .thenReturn(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))
                whenever(pushNotificationRepository.enableWpComNotificationsForSites(any()))
                    .thenReturn(Result.failure(Exception("enable failed")))
            }

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository).unregisterWooPushRegisteredSites(setOf(HIDDEN_SITE_ID))
        }

    @Test
    fun `given M1 flag disabled and no WPCom account, when migration runs, then unregisters all without WPCom calls`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)
            whenever(accountStore.hasAccessToken()).thenReturn(false)
            runBlocking {
                whenever(pushNotificationRepository.getWooPushRegisteredSiteIds())
                    .thenReturn(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))
            }

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(any())
            verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
            verify(pushNotificationRepository).unregisterWooPushRegisteredSites(setOf(SITE_ID_ONE, HIDDEN_SITE_ID))
        }

    @Test
    fun `given M1 flag disabled and WPCom already registered, when migration runs, then skips WPCom device registration`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)
            stubJetpackConnection(siteOne)
            runBlocking {
                whenever(pushNotificationRepository.isWpComPushRegistered()).thenReturn(true)
                whenever(pushNotificationRepository.getWooPushRegisteredSiteIds()).thenReturn(setOf(SITE_ID_ONE))
            }

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(any())
            verify(pushNotificationRepository).enableWpComNotificationsForSites(setOf(SITE_ID_ONE))
            verify(pushNotificationRepository).unregisterWooPushRegisteredSites(setOf(SITE_ID_ONE))
        }

    @Test
    fun `given M1 flag disabled and visible non-jetpack site, when migration runs, then unregisters it unconditionally`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
                .thenReturn(false)
            runBlocking {
                whenever(pushNotificationRepository.getWooPushRegisteredSiteIds()).thenReturn(setOf(SITE_ID_TWO))
            }

            // WHEN
            sut(APP_FOREGROUND)

            // THEN
            verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
            verify(pushNotificationRepository).unregisterWooPushRegisteredSites(setOf(SITE_ID_TWO))
        }

    @Test
    fun `given M1 flag disabled, when site switch trigger runs, then does not migrate`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1))
            .thenReturn(false)

        // WHEN
        sut(SITE_SWITCH)

        // THEN
        verify(pushNotificationRepository, never()).enableWpComNotificationsForSites(any())
        verify(pushNotificationRepository, never()).unregisterWooPushRegisteredSites(any())
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
            verify(pushNotificationRepository, never())
                .registerPushTokenInWooCoreSystem(eq(TEST_TOKEN), eq(selectedSiteModel), any())
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
                    pushNotificationRepository.registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)
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
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)

            // WHEN
            advanceTimeBy(1001.milliseconds)
            queuedTrigger.join()
            advanceUntilIdle()

            // THEN
            verify(
                pushNotificationRepository,
                times(2)
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)
        }

    @Test
    fun `given unforced run in progress, when token refresh is kicked off, then it cancels the unforced run and restarts with force`() =
        testBlocking {
            // GIVEN
            var isFirstCall = true
            runBlocking {
                whenever(
                    pushNotificationRepository.registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)
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
            ).registerPushTokenInWooCoreSystem(TEST_TOKEN, selectedSiteModel, false)
            verify(pushNotificationRepository, atLeast(1)).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteOne, false)
            verify(pushNotificationRepository, atLeast(1)).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteTwo, false)
            verify(pushNotificationRepository, times(1)).registerPushTokenInWpComSystem(TEST_TOKEN)
        }

    @Test
    fun `given multiple sites failing woo registration, when login trigger runs, then wpcom is registered exactly once`() =
        testBlocking {
            // GIVEN
            runBlocking {
                whenever(pushNotificationRepository.isWpComPushRegistered()).thenReturn(false)
                whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                    .thenReturn(Result.failure(Exception("registration failed")))
            }

            // WHEN
            sut(LOGIN_SUCCESS)

            // THEN
            verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteOne, false)
            verify(pushNotificationRepository).registerPushTokenInWooCoreSystem(TEST_TOKEN, siteTwo, false)
            verify(pushNotificationRepository, times(1)).registerPushTokenInWpComSystem(TEST_TOKEN)
        }

    @Test
    fun `given login registration in progress, when a second login trigger runs, then wpcom is registered only once`() =
        testBlocking {
            // GIVEN
            var isWpComRegistered = false
            whenever(pushNotificationRepository.isWpComPushRegistered()).thenAnswer { isWpComRegistered }
            runBlocking {
                whenever(pushNotificationRepository.registerPushTokenInWpComSystem(any())).doSuspendableAnswer {
                    delay(1000.milliseconds)
                    isWpComRegistered = true
                    WpComPushNotificationStore.RegisterDeviceResponsePayload(deviceId = "device-id-123")
                }
            }

            // WHEN
            sut.kickoff(LOGIN_SUCCESS)
            sut.kickoff(LOGIN_SUCCESS)
            advanceUntilIdle()

            // THEN
            verify(pushNotificationRepository, times(1)).registerPushTokenInWpComSystem(TEST_TOKEN)
        }

    @Test
    fun `given instance level orchestration state, when register device is resolved by di, then it is singleton scoped`() {
        assertThat(RegisterDevice::class.java.isAnnotationPresent(Singleton::class.java)).isTrue()
    }

    private fun stubJetpackConnection(site: SiteModel) {
        whenever(site.origin).thenReturn(SiteModel.ORIGIN_WPCOM_REST)
        whenever(site.isJetpackConnected).thenReturn(true)
    }

    companion object {
        private const val TEST_TOKEN = "test-fcm-token-123"
        private const val SELECTED_SITE_ID = 123L
        private const val SITE_ID_ONE = 456L
        private const val SITE_ID_TWO = 789L
        private const val HIDDEN_SITE_ID = 999L
    }
}
