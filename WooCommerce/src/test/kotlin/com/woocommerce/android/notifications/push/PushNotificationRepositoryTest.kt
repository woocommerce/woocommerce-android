package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
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

@ExperimentalCoroutinesApi
class PushNotificationRepositoryTest : BaseUnitTest() {
    private val wooPushNotificationsStore: WooPushNotificationsStore = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val wpComPushNotificationStore: WpComPushNotificationStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker = mock()
    private val siteModel: SiteModel = mock()
    private val preferences: Preferences = mock()
    private val pushNotificationsDataStore: DataStore<Preferences> = mock {
        on { data } doReturn flowOf(preferences)
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
            notificationAnalyticsTracker
        )
    }

    @Test
    fun `given stored uuid, when registering push token succeeds, then saves token and disables wpcom notifications`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(siteModel, "token", "stored-uuid"))
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
            verify(wooPushNotificationsStore).registerPushToken(siteModel, "token", "stored-uuid")
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
    fun `given stored uuid and not wpcom registered, when registering push token fails, then falls back to wpcom registration`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = false)

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isFailure).isTrue()
            verify(wooPushNotificationsStore).registerPushToken(siteModel, "token", "stored-uuid")
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
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = true)

            val result = sut.registerPushTokenInWooCoreSystem("token", siteModel)

            assertThat(result.isFailure).isTrue()
            verify(wooPushNotificationsStore).registerPushToken(siteModel, "token", "stored-uuid")
            verify(wpComPushNotificationStore, never()).registerDevice(any(), any())
        }

    @Test
    fun `when registering push token fails and falls back to wpcom, then does not save token to datastore`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
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
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)
            setupWpComRegistration(isRegistered = false)

            sut.registerPushTokenInWooCoreSystem("token", siteModel)

            val uuidCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper).wooCorePushDeviceUUID = uuidCaptor.capture()
            verify(wooPushNotificationsStore).registerPushToken(
                eq(siteModel),
                eq("token"),
                eq(uuidCaptor.firstValue)
            )
        }

    @Test
    fun `when unregisterDevice is called, then unregisters wpcom token`() = testBlocking {
        whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf())

        sut.unregisterDeviceFromAllPushes()

        verify(wpComPushNotificationStore).unregisterWpComPushToken()
    }

    @Test
    fun `given sites with stored tokens, when unregisterDevice called, then deletes tokens from server`() =
        testBlocking {
            val site1 = mock<SiteModel> { on { siteId } doReturn 123L }
            val site2 = mock<SiteModel> { on { siteId } doReturn 456L }
            whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf(site1, site2))

            val tokenKey1 = stringPreferencesKey("push_token_123")
            val tokenKey2 = stringPreferencesKey("push_token_456")
            whenever(preferences[tokenKey1]).thenReturn("token-id-1")
            whenever(preferences[tokenKey2]).thenReturn("token-id-2")

            whenever(wooPushNotificationsStore.deletePushToken(any(), any())).thenReturn(WooResult(Unit))

            sut.unregisterDeviceFromAllPushes()

            verify(wooPushNotificationsStore).deletePushToken(site1, "token-id-1")
            verify(wooPushNotificationsStore).deletePushToken(site2, "token-id-2")
        }

    @Test
    fun `given site without stored token, when unregisterDevice called, then does not call delete for that site`() =
        testBlocking {
            val site1 = mock<SiteModel> { on { siteId } doReturn 123L }
            val site2 = mock<SiteModel> { on { siteId } doReturn 456L }
            whenever(wooCommerceStore.getWooCommerceSites()).thenReturn(mutableListOf(site1, site2))

            val tokenKey1 = stringPreferencesKey("push_token_123")
            val tokenKey2 = stringPreferencesKey("push_token_456")
            whenever(preferences[tokenKey1]).thenReturn("token-id-1")
            whenever(preferences[tokenKey2]).thenReturn(null)

            whenever(wooPushNotificationsStore.deletePushToken(any(), any())).thenReturn(WooResult(Unit))

            sut.unregisterDeviceFromAllPushes()

            verify(wooPushNotificationsStore).deletePushToken(site1, "token-id-1")
            verify(wooPushNotificationsStore, never()).deletePushToken(eq(site2), any())
        }

    @Test
    fun `given token exists for site, when isWooPushTokenRegisteredForSite called, then returns true`() =
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
    fun `given registration succeeds, when registering push token, then tracks success event`() =
        testBlocking {
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(siteModel, "token", "stored-uuid"))
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
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
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

    private fun setupWpComRegistration(isRegistered: Boolean) {
        val deviceId = if (isRegistered) "device-id-123" else null
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn(deviceId)
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
    }
}
