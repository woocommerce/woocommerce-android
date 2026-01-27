package com.woocommerce.android.notifications.push

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
import org.mockito.kotlin.verifyNoInteractions
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

@ExperimentalCoroutinesApi
class PushNotificationRepositoryTest : BaseUnitTest() {
    private val wooPushNotificationsStore: WooPushNotificationsStore = mock()
    private val selectedSite: com.woocommerce.android.tools.SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val wpComPushNotificationStore: WpComPushNotificationStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker = mock()
    private val siteModel: SiteModel = mock()
    private val preferences: Preferences = mock()
    private val pushNotificationsDataStore: DataStore<Preferences> = mock {
        on { data } doReturn flowOf(preferences)
    }

    private lateinit var sut: PushNotificationRepository

    @Before
    fun setUp() {
        sut = PushNotificationRepository(
            wooPushNotificationsStore,
            selectedSite,
            appPrefsWrapper,
            wpComPushNotificationStore,
            wooCommerceStore,
            pushNotificationsDataStore,
            notificationAnalyticsTracker
        )
    }

    @Test
    fun `given no selected site, when registering push token called, then nothing happens`() = testBlocking {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        sut.registerPushTokenInWooCoreSystem("token")

        verifyNoInteractions(wooPushNotificationsStore, wpComPushNotificationStore)
    }

    @Test
    fun `given selected site and stored uuid, when registering push token succeeds, then saves token and disables wpcom notifications`() =
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(siteModel.siteId).thenReturn(SITE_ID)
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

            sut.registerPushTokenInWooCoreSystem("token")

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
    fun `given selected site and stored uuid, when registering push token fails, then wpcom notifications are not updated`() {
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            sut.registerPushTokenInWooCoreSystem("token")

            verify(wooPushNotificationsStore).registerPushToken(siteModel, "token", "stored-uuid")
            verify(wpComPushNotificationStore, never()).updateNotificationSettingsFor(any())
        }
    }

    @Test
    fun `given missing uuid, when registering push token called, then generates and stores new uuid `() =
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            sut.registerPushTokenInWooCoreSystem("token")

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
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(siteModel.siteId).thenReturn(SITE_ID)
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

            sut.registerPushTokenInWooCoreSystem("token")

            verify(notificationAnalyticsTracker).track(
                stat = eq(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_SUCCESS),
                siteId = eq(SITE_ID)
            )
        }

    @Test
    fun `given registration fails, when registering push token, then tracks error event`() =
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(siteModel.siteId).thenReturn(SITE_ID)
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(wooPushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            sut.registerPushTokenInWooCoreSystem("token")

            verify(notificationAnalyticsTracker).trackError(
                stat = eq(AnalyticsEvent.WOO_PUSH_TOKEN_REGISTER_ERROR),
                siteId = eq(SITE_ID),
                errorDescription = anyOrNull(),
                errorType = anyOrNull(),
                errorCode = anyOrNull()
            )
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
