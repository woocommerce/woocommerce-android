package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.WooPushNotificationsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.SiteNotificationSetting
import org.wordpress.android.fluxc.utils.PreferenceUtils
import java.util.Locale

@ExperimentalCoroutinesApi
class PushNotificationRepositoryTest : BaseUnitTest() {
    private val wooPushNotificationsStore: WooPushNotificationsStore = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val wpComPushNotificationStore: WpComPushNotificationStore = mock {
        on {
            registerDevice(any(), any())
        } doReturn WpComPushNotificationStore.RegisterDeviceResponsePayload(deviceId = "device-id-123")
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker = mock()
    private val localeProvider: LocaleProvider = mock {
        on { provideLocale() } doReturn Locale.US
    }
    private val selectedSite: SelectedSite = mock()
    private val siteModel: SiteModel = mock()
    private val preferences: Preferences = mock()
    private val pushNotificationsDataStore: DataStore<Preferences> = mock {
        on { data } doReturn flowOf(preferences)
    }
    private val checkWooPluginPushNotificationsSupport: CheckWooPluginPushNotificationsSupport = mock {
        on { invoke(forceRefresh = false) } doReturn CheckWooPluginPushNotificationsSupport.Result.Compatible
    }

    private lateinit var sut: PushNotificationRepository

    @Before
    fun setUp() {
        whenever(prefsWrapper.getFluxCPreferences()).thenReturn(sharedPreferences)
        whenever(siteModel.siteId).thenReturn(SITE_ID)
        sut = PushNotificationRepository(
            wooPushNotificationsStore,
            appPrefsWrapper,
            wpComPushNotificationStore,
            wooCommerceStore,
            prefsWrapper,
            pushNotificationsDataStore,
            notificationAnalyticsTracker,
            localeProvider,
            checkWooPluginPushNotificationsSupport,
            coroutinesTestRule.testDispatchers,
            selectedSite
        )
    }

    @Test
    fun `given stored uuid, when registering push token succeeds, then saves token and disables wpcom notifications`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(WooResult(RETURNED_TOKEN))

            val mutablePreferences: MutablePreferences = mock()
            whenever(preferences.toMutablePreferences()).thenReturn(mutablePreferences)
            whenever(pushNotificationsDataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                testBlocking { transform(preferences) }
                preferences
            }

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isSuccess).isTrue()
            verify(wooPushNotificationsStore).registerPushToken(
                eq(siteModel),
                eq("token"),
                eq("stored-uuid"),
                any(),
                any()
            )
            val expectedTokenKey = stringPreferencesKey("push_token_$SITE_ID")
            verify(mutablePreferences)[expectedTokenKey] = RETURNED_TOKEN
            verify(wpComPushNotificationStore).updateNotificationSettingsFor(
                listOf(
                    SiteNotificationSetting(
                        siteId = SITE_ID,
                        newCommentEnabled = false,
                        storeOrderEnabled = false
                    )
                )
            )
            verify(appPrefsWrapper, never()).wooCorePushDeviceUUID = any()
        }

    @Test
    fun `given registration succeeds, when registering push token in woo core, then saves token id token and locale`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(WooResult(RETURNED_TOKEN))

            val mutablePreferences: MutablePreferences = mock()
            whenever(preferences.toMutablePreferences()).thenReturn(mutablePreferences)
            whenever(pushNotificationsDataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                testBlocking { transform(preferences) }
                preferences
            }

            sut.registerPushTokenInWooCoreSystem("token", siteModel)

            verify(mutablePreferences)[stringPreferencesKey("push_token_$SITE_ID")] = RETURNED_TOKEN
            verify(mutablePreferences)[stringPreferencesKey("push_token_value_$SITE_ID")] = "token"
            verify(mutablePreferences)[stringPreferencesKey("push_locale_$SITE_ID")] = "en_US"
        }

    @Test
    fun `given stored uuid and not wpcom registered, when registering push token fails, then falls back to wpcom registration`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = false)

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isFailure).isTrue()
            verify(wooPushNotificationsStore).registerPushToken(
                eq(siteModel),
                eq("token"),
                eq("stored-uuid"),
                any(),
                any()
            )
            verify(wpComPushNotificationStore, never()).updateNotificationSettingsFor(any())
            verify(wpComPushNotificationStore).registerDevice(
                "token",
                WpComPushNotificationStore.NotificationAppKey.WOOCOMMERCE
            )
        }

    @Test
    fun `given already wpcom registered, when registering push token fails, then does not fallback to wpcom registration`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = true)

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isFailure).isTrue()
            verify(wooPushNotificationsStore).registerPushToken(
                eq(siteModel),
                eq("token"),
                eq("stored-uuid"),
                any(),
                any()
            )
            verify(wpComPushNotificationStore, never()).registerDevice(any(), any())
        }

    @Test
    fun `when registering push token fails and falls back to wpcom, then does not save token to datastore`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = false)

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isFailure).isTrue()
            verify(pushNotificationsDataStore, never()).updateData(any())
        }

    @Test
    fun `given missing uuid, when registering push token called, then generates and stores new uuid`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = false)

            sut.registerPushTokenInWooCoreSystem("token", siteModel)

            val uuidCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper).wooCorePushDeviceUUID = uuidCaptor.capture()
            verify(wooPushNotificationsStore).registerPushToken(
                eq(siteModel),
                eq("token"),
                eq(uuidCaptor.firstValue),
                any(),
                any()
            )
        }

    @Test
    fun `when unregisterDevice is called, then unregisters wpcom token`() = testBlocking {
        whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf())
        setupWpComRegistration(isRegistered = true)

        sut.unregisterDeviceFromPushNotifications()

        verify(wpComPushNotificationStore).unregisterWpComPushToken()
    }

    @Test
    fun `when unregister device is called, then waits for wpcom cleanup to finish`() = testBlocking {
        // GIVEN
        whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf())
        setupWpComRegistration(isRegistered = true)
        val unregisterGate = CompletableDeferred<Unit>()
        whenever(wpComPushNotificationStore.unregisterWpComPushToken()).doSuspendableAnswer {
            unregisterGate.await()
            WpComPushNotificationStore.UnregisterDeviceResponsePayload()
        }

        // WHEN
        val job = launch { sut.unregisterDeviceFromPushNotifications() }
        runCurrent()

        // THEN
        assertThat(job.isCompleted).isFalse()

        unregisterGate.complete(Unit)
        advanceUntilIdle()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun `given site lookup throws, when unregister device is called, then still attempts wpcom cleanup`() =
        testBlocking {
            // GIVEN
            whenever(wooCommerceStore.getWooCommerceSites()).thenThrow(IllegalStateException("boom"))
            setupWpComRegistration(isRegistered = true)

            // WHEN
            val result = runCatching { sut.unregisterDeviceFromPushNotifications() }

            // THEN
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            verify(wpComPushNotificationStore).unregisterWpComPushToken()
        }

    @Test
    fun `given sites with stored tokens, when unregisterDevice called, then deletes tokens from server`() =
        testBlocking {
            val site1 = mock<SiteModel> { on { siteId } doReturn 123L }
            val site2 = mock<SiteModel> { on { siteId } doReturn 456L }
            whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf(site1, site2))

            whenever(preferences[stringPreferencesKey("push_token_123")]).thenReturn("token-id-1")
            whenever(preferences[stringPreferencesKey("push_token_value_123")]).thenReturn("token-1")
            whenever(preferences[stringPreferencesKey("push_locale_123")]).thenReturn("en_US")
            whenever(preferences[stringPreferencesKey("push_token_456")]).thenReturn("token-id-2")
            whenever(preferences[stringPreferencesKey("push_token_value_456")]).thenReturn("token-2")
            whenever(preferences[stringPreferencesKey("push_locale_456")]).thenReturn("en_US")

            whenever(wooPushNotificationsStore.deletePushToken(any(), any())).thenReturn(WooResult(Unit))

            sut.unregisterDeviceFromPushNotifications()

            verify(wooPushNotificationsStore).deletePushToken(site1, "token-id-1")
            verify(wooPushNotificationsStore).deletePushToken(site2, "token-id-2")
        }

    @Test
    fun `given site metadata stored, when unregistering woo token succeeds, then removes all site metadata keys`() =
        testBlocking {
            val site = mock<SiteModel> { on { siteId } doReturn SITE_ID }
            whenever(preferences[stringPreferencesKey("push_token_$SITE_ID")]).thenReturn("token-id-1")
            whenever(preferences[stringPreferencesKey("push_token_value_$SITE_ID")]).thenReturn("token")
            whenever(preferences[stringPreferencesKey("push_locale_$SITE_ID")]).thenReturn("en_US")
            whenever(wooPushNotificationsStore.deletePushToken(site, "token-id-1")).thenReturn(WooResult(Unit))

            val mutablePreferences: MutablePreferences = mock()
            whenever(preferences.toMutablePreferences()).thenReturn(mutablePreferences)
            whenever(pushNotificationsDataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                testBlocking { transform(preferences) }
                preferences
            }

            val result = sut.unregisterWooPushTokenForSite(site)

            assertThat(result.isSuccess).isTrue()
            verify(mutablePreferences).remove(stringPreferencesKey("push_token_$SITE_ID"))
            verify(mutablePreferences).remove(stringPreferencesKey("push_token_value_$SITE_ID"))
            verify(mutablePreferences).remove(stringPreferencesKey("push_locale_$SITE_ID"))
        }

    @Test
    fun `given site without stored metadata, when unregistering single woo site, then succeeds without deleting token`() =
        testBlocking {
            whenever(preferences[stringPreferencesKey("push_token_$SITE_ID")]).thenReturn(null)

            val result = sut.unregisterWooPushTokenForSite(siteModel)

            assertThat(result.isSuccess).isTrue()
            verify(wooPushNotificationsStore, never()).deletePushToken(eq(siteModel), any())
        }

    @Test
    fun `given site metadata stored, when unregistering single woo site fails, then returns failure and keeps metadata`() =
        testBlocking {
            whenever(preferences[stringPreferencesKey("push_token_$SITE_ID")]).thenReturn("token-id-1")
            whenever(preferences[stringPreferencesKey("push_token_value_$SITE_ID")]).thenReturn("token")
            whenever(preferences[stringPreferencesKey("push_locale_$SITE_ID")]).thenReturn("en_US")
            whenever(wooPushNotificationsStore.deletePushToken(siteModel, "token-id-1"))
                .thenReturn(PN_UNREGISTER_ERROR)

            val result = sut.unregisterWooPushTokenForSite(siteModel)

            assertThat(result.isFailure).isTrue()
            verify(pushNotificationsDataStore, never()).updateData(any())
        }

    @Test
    fun `given site without stored token, when unregisterDevice called, then does not call delete for that site`() =
        testBlocking {
            val site1 = mock<SiteModel> { on { siteId } doReturn 123L }
            val site2 = mock<SiteModel> { on { siteId } doReturn 456L }
            whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf(site1, site2))

            whenever(preferences[stringPreferencesKey("push_token_123")]).thenReturn("token-id-1")
            whenever(preferences[stringPreferencesKey("push_token_value_123")]).thenReturn("token-1")
            whenever(preferences[stringPreferencesKey("push_locale_123")]).thenReturn("en_US")
            whenever(preferences[stringPreferencesKey("push_token_456")]).thenReturn(null)

            whenever(wooPushNotificationsStore.deletePushToken(any(), any())).thenReturn(WooResult(Unit))

            sut.unregisterDeviceFromPushNotifications()

            verify(wooPushNotificationsStore).deletePushToken(site1, "token-id-1")
            verify(wooPushNotificationsStore, never()).deletePushToken(eq(site2), any())
        }

    @Test
    fun `given deletePushToken returns INVALID_ID error, when unregisterDevice called, then clears local token`() =
        testBlocking {
            val site = mock<SiteModel> { on { siteId } doReturn 123L }
            whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf(site))
            whenever(preferences[stringPreferencesKey("push_token_123")]).thenReturn("token-id-1")
            whenever(preferences[stringPreferencesKey("push_token_value_123")]).thenReturn("token-1")
            whenever(preferences[stringPreferencesKey("push_locale_123")]).thenReturn("en_US")
            whenever(wooPushNotificationsStore.deletePushToken(any(), any())).thenReturn(
                WooResult(WooError(WooErrorType.INVALID_ID, BaseRequest.GenericErrorType.NOT_FOUND, "Not found"))
            )
            val mutablePreferences: MutablePreferences = mock()
            whenever(preferences.toMutablePreferences()).thenReturn(mutablePreferences)
            whenever(pushNotificationsDataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                testBlocking { transform(preferences) }
                preferences
            }

            sut.unregisterDeviceFromPushNotifications()

            verify(mutablePreferences).remove(stringPreferencesKey("push_token_123"))
            verify(mutablePreferences).remove(stringPreferencesKey("push_token_value_123"))
            verify(mutablePreferences).remove(stringPreferencesKey("push_locale_123"))
        }

    @Test
    fun `given deletePushToken returns non-INVALID_ID error, when unregisterDevice called, then keeps local token`() =
        testBlocking {
            val site = mock<SiteModel> { on { siteId } doReturn 123L }
            whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf(site))
            whenever(preferences[stringPreferencesKey("push_token_123")]).thenReturn("token-id-1")
            whenever(preferences[stringPreferencesKey("push_token_value_123")]).thenReturn("token-1")
            whenever(preferences[stringPreferencesKey("push_locale_123")]).thenReturn("en_US")
            whenever(wooPushNotificationsStore.deletePushToken(any(), any())).thenReturn(
                WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN, "oops"))
            )

            sut.unregisterDeviceFromPushNotifications()

            verify(pushNotificationsDataStore, never()).updateData(any())
        }

    @Test
    fun `given token exists for site and plugin is compatible, when isWooPushTokenRegisteredForSite called, then returns true`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("token-id-1")

            val result = sut.isWooPushTokenRegisteredForSite(SITE_ID)

            assertThat(result).isTrue()
        }

    @Test
    fun `given no token exists for site, when isWooPushTokenRegisteredForSite called, then returns false`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn(null)

            val result = sut.isWooPushTokenRegisteredForSite(SITE_ID)

            assertThat(result).isFalse()
        }

    @Test
    fun `given empty token id exists for site, when isWooPushTokenRegisteredForSite called, then returns false`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("")

            val result = sut.isWooPushTokenRegisteredForSite(SITE_ID)

            assertThat(result).isFalse()
        }

    @Test
    fun `given token stored but plugin incompatible, when isWooPushTokenRegisteredForSite called, then returns false`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("token-id-1")
            setupPluginCompatibility(isCompatible = false)

            val result = sut.isWooPushTokenRegisteredForSite(SITE_ID)

            assertThat(result).isFalse()
        }

    @Test
    fun `given token stored but plugin version not cached, when isWooPushTokenRegisteredForSite called, then returns true`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("token-id-1")
            doReturn(CheckWooPluginPushNotificationsSupport.Result.Error)
                .whenever(checkWooPluginPushNotificationsSupport).invoke(forceRefresh = false)

            val result = sut.isWooPushTokenRegisteredForSite(SITE_ID)

            assertThat(result).isTrue()
        }

    @Test
    fun `given token stored and plugin compatible, when observeWooPushTokenRegisteredForSite, then emits true`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("token-id-1")

            val result = sut.observeWooPushTokenRegisteredForSite(SITE_ID).first()

            assertThat(result).isTrue()
        }

    @Test
    fun `given token stored but plugin incompatible, when observeWooPushTokenRegisteredForSite, then emits false`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("token-id-1")
            setupPluginCompatibility(isCompatible = false)

            val result = sut.observeWooPushTokenRegisteredForSite(SITE_ID).first()

            assertThat(result).isFalse()
        }

    @Test
    fun `given token stored but plugin version not cached, when observeWooPushTokenRegisteredForSite, then emits true`() =
        testBlocking {
            val tokenKey = stringPreferencesKey("push_token_$SITE_ID")
            whenever(preferences[tokenKey]).thenReturn("token-id-1")
            doReturn(CheckWooPluginPushNotificationsSupport.Result.Error)
                .whenever(checkWooPluginPushNotificationsSupport).invoke(forceRefresh = false)

            val result = sut.observeWooPushTokenRegisteredForSite(SITE_ID).first()

            assertThat(result).isTrue()
        }

    @Test
    fun `given woo locale is missing, when checking whether woo push should register, then returns true`() =
        testBlocking {
            whenever(preferences[stringPreferencesKey("push_token_$SITE_ID")]).thenReturn(RETURNED_TOKEN)
            whenever(preferences[stringPreferencesKey("push_token_value_$SITE_ID")]).thenReturn("token")
            whenever(preferences[stringPreferencesKey("push_locale_$SITE_ID")]).thenReturn(null)

            val result = sut.shouldRegisterWooPushForSite(currentToken = "token", siteId = SITE_ID)

            assertThat(result).isTrue()
        }

    @Test
    fun `given woo token locale and id match, when checking whether woo push should register, then returns false`() =
        testBlocking {
            whenever(preferences[stringPreferencesKey("push_token_$SITE_ID")]).thenReturn(RETURNED_TOKEN)
            whenever(preferences[stringPreferencesKey("push_token_value_$SITE_ID")]).thenReturn("token")
            whenever(preferences[stringPreferencesKey("push_locale_$SITE_ID")]).thenReturn("en_US")

            val result = sut.shouldRegisterWooPushForSite(currentToken = "token", siteId = SITE_ID)

            assertThat(result).isFalse()
        }

    @Test
    fun `given registration succeeds, when registering push token, then tracks success event`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(WooResult(RETURNED_TOKEN))

            val mutablePreferences: MutablePreferences = mock()
            whenever(preferences.toMutablePreferences()).thenReturn(mutablePreferences)
            whenever(pushNotificationsDataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                testBlocking { transform(preferences) }
                preferences
            }

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isSuccess).isTrue()
            verify(notificationAnalyticsTracker).track(
                stat = eq(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_SUCCESS),
                siteId = eq(SITE_ID)
            )
        }

    @Test
    fun `given registration fails, when registering push token, then tracks error event`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isFailure).isTrue()
            verify(notificationAnalyticsTracker).trackError(
                stat = eq(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_ERROR),
                siteId = eq(SITE_ID),
                errorDescription = anyOrNull(),
                errorType = anyOrNull(),
                errorCode = anyOrNull()
            )
        }

    @Test
    fun `given registration fails and wpcom fallback is disabled, when registering push token, then does not register in wpcom`() =
        testBlocking {
            // GIVEN
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            // WHEN
            sut.registerPushTokenInWooCoreSystem(
                token = "token",
                selectedSite = siteModel,
                allowWpComFallback = false
            )

            // THEN
            verify(wpComPushNotificationStore, never()).registerDevice(any(), any())
        }

    @Test
    fun `when registering push token, then sends device locale and metadata`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            sut.registerPushTokenInWooCoreSystem("token", siteModel)

            val localeCaptor = argumentCaptor<String>()
            val metadataCaptor = argumentCaptor<Map<String, String>>()
            verify(wooPushNotificationsStore).registerPushToken(
                any(),
                any(),
                any(),
                localeCaptor.capture(),
                metadataCaptor.capture()
            )
            assertThat(localeCaptor.firstValue).isEqualTo("en_US")
            assertThat(metadataCaptor.firstValue).containsKeys("app_version", "device_model", "os_version")
        }

    @Test
    fun `given wpcom disable fails with api code, when registration succeeds, then error code is tracked`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any(), any(), any()))
                .thenReturn(WooResult(RETURNED_TOKEN))
            whenever(wpComPushNotificationStore.updateNotificationSettingsFor(any()))
                .thenReturn(
                    Result.failure(
                        WpComPushNotificationStore.NotificationSettingsUpdateError(
                            type = WpComPushNotificationStore.NotificationSettingErrorType.ApiError(
                                apiErrorMessage = "forbidden",
                                apiErrorCode = "rest_forbidden"
                            )
                        )
                    )
                )

            val mutablePreferences: MutablePreferences = mock()
            whenever(preferences.toMutablePreferences()).thenReturn(mutablePreferences)
            whenever(pushNotificationsDataStore.updateData(any())).thenAnswer { invocation ->
                val transform = invocation.getArgument<suspend (Preferences) -> Preferences>(0)
                testBlocking { transform(preferences) }
                preferences
            }

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isSuccess).isTrue()
            verify(notificationAnalyticsTracker).trackError(
                stat = eq(AnalyticsEvent.WPCOM_DEVICE_DISABLE_PUSH_NOTIFICATIONS_ERROR),
                siteId = eq(SITE_ID),
                errorDescription = anyOrNull(),
                errorType = anyOrNull(),
                errorCode = eq("rest_forbidden")
            )
        }

    private fun setupWpComRegistration(isRegistered: Boolean) {
        val deviceId = if (isRegistered) "device-id-123" else null
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn(deviceId)
    }

    private suspend fun setupPluginCompatibility(isCompatible: Boolean) {
        val result = if (isCompatible) {
            CheckWooPluginPushNotificationsSupport.Result.Compatible
        } else {
            CheckWooPluginPushNotificationsSupport.Result.UpdateRequired(currentVersion = "9.0.0")
        }
        doReturn(result).whenever(checkWooPluginPushNotificationsSupport).invoke(forceRefresh = false)
    }

    private companion object {
        const val RETURNED_TOKEN = "returned-token-123"
        const val SITE_ID = 123L

        val PN_REGISTRATION_ERROR = WooResult<String>(
            WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.UNKNOWN,
                "oops"
            )
        )

        val PN_UNREGISTER_ERROR = WooResult<Unit>(
            WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.UNKNOWN,
                "oops"
            )
        )
    }
}
