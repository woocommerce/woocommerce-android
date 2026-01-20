package com.woocommerce.android.notifications.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import org.wordpress.android.fluxc.store.NotificationStore

@ExperimentalCoroutinesApi
class PushNotificationRepositoryTest : BaseUnitTest() {
    private val pushNotificationsStore: PushNotificationsStore = mock()
    private val selectedSite: com.woocommerce.android.tools.SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val notificationStore: NotificationStore = mock()
    private val siteModel: SiteModel = mock()
    private val pushNotificationsDataStore: DataStore<Preferences> = mock()

    private lateinit var sut: PushNotificationRepository

    @Before
    fun setUp() {
        sut = PushNotificationRepository(
            pushNotificationsStore,
            selectedSite,
            appPrefsWrapper,
            notificationStore,
            pushNotificationsDataStore
        )
    }

    @Test
    fun `given no selected site, when registering push token called, then nothing happens`() = testBlocking {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        sut.registerPushToken("token")

        verifyNoInteractions(pushNotificationsStore, notificationStore)
    }

    @Test
    fun `given selected site and stored uuid, when registering push token succeeds, then unregisters wpcom token`() =
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(pushNotificationsStore.registerPushToken(siteModel, "token", "stored-uuid"))
                .thenReturn(WooResult(RETURNED_TOKEN))

            sut.registerPushToken("token")

            verify(pushNotificationsStore).registerPushToken(siteModel, "token", "stored-uuid")
            verify(notificationStore).unregisterWpComPushToken()
            verify(appPrefsWrapper, never()).wooCorePushDeviceUUID = any()
        }

    @Test
    fun `given selected site and stored uuid, when registering push token fails, then wpcom PN token is not unregistered`() {
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("stored-uuid")
            whenever(pushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            sut.registerPushToken("token")

            verify(pushNotificationsStore).registerPushToken(siteModel, "token", "stored-uuid")
            verify(notificationStore, never()).unregisterWpComPushToken()
        }
    }

    @Test
    fun `given missing uuid, when registering push token called, then generates and stores new uuid `() =
        testBlocking {
            whenever(selectedSite.getIfExists()).thenReturn(siteModel)
            whenever(appPrefsWrapper.wooCorePushDeviceUUID).thenReturn("")
            whenever(pushNotificationsStore.registerPushToken(any(), any(), any()))
                .thenReturn(PN_REGISTRATION_ERROR)

            sut.registerPushToken("token")

            val uuidCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper).wooCorePushDeviceUUID = uuidCaptor.capture()
            verify(pushNotificationsStore).registerPushToken(
                eq(siteModel),
                eq("token"),
                eq(uuidCaptor.firstValue)
            )
        }

    private companion object {
        const val RETURNED_TOKEN = "returned-token-123"

        val PN_REGISTRATION_ERROR = WooResult<String>(
            WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.UNKNOWN,
                "oops"
            )
        )
    }
}
