package org.wordpress.android.fluxc.store

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.notifications.NotificationTestUtils
import org.wordpress.android.fluxc.persistence.NotificationMapper
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.DeviceRegistrationError
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.DeviceRegistrationErrorType
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationHashesResponsePayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationResponsePayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationsResponsePayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.MarkNotificationsReadResponsePayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationAppKey
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationError
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationErrorType
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.RegisterDeviceResponsePayload
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.SiteNotificationSetting
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.UnregisterDeviceResponsePayload
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.PreferenceUtils

private const val API_RESPONSE = "notifications/notifications-api-response.json"

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("UnitTestNamingRule")
class WpComPushNotificationStoreTest {
    @Rule
    @JvmField
    val databaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val mockNotificationRestClient = mock<NotificationRestClient>()
    private val notificationMapper = NotificationMapper(FormattableContentMapper(Gson()))
    private lateinit var store: WpComPushNotificationStore

    private val site = SiteModel().apply { siteId = 153482281 } // FYI: found within api response file.
    private val token = "fcm_token"
    private val deviceId = "device_123"
    private val uuid = "uuid_123"

    @Before
    fun setUp() {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext())
            .edit()
            .clear()
            .apply()

        store = WpComPushNotificationStore(
            dispatcher = Dispatcher(),
            context = ApplicationProvider.getApplicationContext(),
            notificationRestClient = mockNotificationRestClient,
            notificationDao = databaseRule.db.notificationDao(),
            notificationMapper = notificationMapper,
            coroutineEngine = initCoroutineEngine()
        )
    }

    // region getNotificationsForSite
    @Test
    fun `given no notifications, when get notifications for site, then returns empty list`() = runTest {
        val notifications = store.getNotificationsForSite(site)

        assertThat(notifications).isEmpty()
    }

    @Test
    fun `given notifications exist, when get notifications for site, then returns site notifications`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.getNotificationsForSite(site)

        assertThat(notifications).hasSize(3)
    }

    @Test
    fun `given notifications exist, when get for site with type filter, then returns filtered`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.getNotificationsForSite(
            site,
            filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString())
        )

        assertThat(notifications).hasSize(1)
    }

    @Test
    fun `given notifications exist, when get for site with subtype filter, then returns filtered`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.getNotificationsForSite(
            site,
            filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())
        )

        assertThat(notifications).hasSize(2)
    }

    @Test
    fun `given notifications exist, when get for site with both filters, then returns combined`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.getNotificationsForSite(
            site,
            filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
            filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())
        )

        assertThat(notifications).hasSize(3)
    }

    @Test
    fun `given notifications exist, when get for different site, then returns empty list`() = runTest {
        insertNotificationsFromJson()
        val differentSite = SiteModel().apply { siteId = 999999 }

        val notifications = store.getNotificationsForSite(differentSite)

        assertThat(notifications).isEmpty()
    }

    @Test
    fun `given notifications exist, when get for site, then returns ordered by timestamp desc`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.getNotificationsForSite(site)

        assertThat(notifications[0].remoteNoteId).isEqualTo(3675336239L) // Dec 4, 2018
        assertThat(notifications[1].remoteNoteId).isEqualTo(3616322875L) // Nov 1, 2018
        assertThat(notifications[2].remoteNoteId).isEqualTo(3617558725L) // Oct 30, 2018
    }
    // endregion

    // region observeNotificationsForSite
    @Test
    fun `given notifications exist, when observe for site, then emits notifications`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.observeNotificationsForSite(site).first()

        assertThat(notifications).hasSize(3)
    }

    @Test
    fun `given no notifications, when observe for site, then emits empty list`() = runTest {
        val notifications = store.observeNotificationsForSite(site).first()

        assertThat(notifications).isEmpty()
    }

    @Test
    fun `given notifications exist, when observe with type filter, then emits filtered`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.observeNotificationsForSite(
            site,
            filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString())
        ).first()

        assertThat(notifications).hasSize(1)
    }

    @Test
    fun `given notifications exist, when observe with subtype filter, then emits filtered`() = runTest {
        insertNotificationsFromJson()

        val notifications = store.observeNotificationsForSite(
            site,
            filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())
        ).first()

        assertThat(notifications).hasSize(2)
    }
    // endregion

    // region hasUnreadNotificationsForSite
    @Test
    fun `given unread notifications exist, when has unread for site, then returns true`() = runTest {
        insertNotificationsFromJson()

        val hasUnread = store.hasUnreadNotificationsForSite(site)

        assertThat(hasUnread).isTrue()
    }

    @Test
    fun `given no notifications, when has unread for site, then returns false`() = runTest {
        val hasUnread = store.hasUnreadNotificationsForSite(site)

        assertThat(hasUnread).isFalse()
    }

    @Test
    fun `given all notifications read, when has unread for site, then returns false`() = runTest {
        insertNotificationsFromJson()
        markAllNotificationsAsRead()

        val hasUnread = store.hasUnreadNotificationsForSite(site)

        assertThat(hasUnread).isFalse()
    }

    @Test
    fun `given unread exists, when has unread with matching type filter, then returns true`() = runTest {
        insertNotificationsFromJson()

        val hasUnread = store.hasUnreadNotificationsForSite(
            site,
            filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())
        )

        assertThat(hasUnread).isTrue()
    }

    @Test
    fun `given unread exists, when has unread with non-matching type filter, then returns false`() = runTest {
        insertNotificationsFromJson()

        val hasUnread = store.hasUnreadNotificationsForSite(
            site,
            filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString())
        )

        assertThat(hasUnread).isFalse()
    }
    // endregion

    // region registerDevice
    @Test
    fun `given successful registration, when register device, then returns device id`() = runTest {
        whenever(mockNotificationRestClient.registerDevice(any(), any(), any()))
            .thenReturn(RegisterDeviceResponsePayload(deviceId))

        val result = store.registerDevice(token, NotificationAppKey.WOOCOMMERCE)

        assertThat(result.deviceId).isEqualTo(deviceId)
        assertThat(result.isError).isFalse()
    }

    @Test
    fun `given successful registration, when register device, then stores device id in prefs`() = runTest {
        whenever(mockNotificationRestClient.registerDevice(any(), any(), any()))
            .thenReturn(RegisterDeviceResponsePayload(deviceId))

        store.registerDevice(token, NotificationAppKey.WOOCOMMERCE)

        val storedDeviceId = PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext())
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        assertThat(storedDeviceId).isEqualTo(deviceId)
    }

    @Test
    fun `given registration error, when register device, then returns error`() = runTest {
        val error = DeviceRegistrationError(DeviceRegistrationErrorType.GENERIC_ERROR, "Failed")
        whenever(mockNotificationRestClient.registerDevice(any(), any(), any()))
            .thenReturn(RegisterDeviceResponsePayload(error))

        val result = store.registerDevice(token, NotificationAppKey.WOOCOMMERCE)

        assertThat(result.isError).isTrue()
        assertThat(result.error.type).isEqualTo(DeviceRegistrationErrorType.GENERIC_ERROR)
    }

    @Test
    fun `given no uuid stored, when register device, then generates and stores uuid`() = runTest {
        whenever(mockNotificationRestClient.registerDevice(any(), any(), any()))
            .thenReturn(RegisterDeviceResponsePayload(deviceId))

        store.registerDevice(token, NotificationAppKey.WOOCOMMERCE)

        val storedUuid = PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext())
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_UUID, null)
        assertThat(storedUuid).isNotNull()
    }

    @Test
    fun `given uuid already stored, when register device, then reuses existing uuid`() = runTest {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext()).edit()
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_UUID, uuid)
            .apply()
        whenever(mockNotificationRestClient.registerDevice(any(), any(), any()))
            .thenReturn(RegisterDeviceResponsePayload(deviceId))

        store.registerDevice(token, NotificationAppKey.WOOCOMMERCE)

        verify(mockNotificationRestClient).registerDevice(
            fcmToken = token,
            appKey = NotificationAppKey.WOOCOMMERCE,
            uuid = uuid
        )
    }
    // endregion

    // region unregisterWpComPushToken
    @Test
    fun `given device id stored, when unregister, then calls rest client`() = runTest {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext()).edit()
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, deviceId)
            .apply()
        whenever(mockNotificationRestClient.unregisterDevice(deviceId))
            .thenReturn(UnregisterDeviceResponsePayload())

        store.unregisterWpComPushToken()

        verify(mockNotificationRestClient).unregisterDevice(deviceId)
    }

    @Test
    fun `given successful unregister, when unregister, then clears preferences`() = runTest {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext()).edit()
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, deviceId)
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_UUID, uuid)
            .apply()
        whenever(mockNotificationRestClient.unregisterDevice(deviceId))
            .thenReturn(UnregisterDeviceResponsePayload())

        store.unregisterWpComPushToken()

        val prefs = PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext())
        assertThat(prefs.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)).isNull()
        assertThat(prefs.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_UUID, null)).isNull()
    }

    @Test
    fun `given no device id, when unregister, then returns error without calling rest client`() = runTest {
        val result = store.unregisterWpComPushToken()

        assertThat(result.isError).isTrue()
    }
    // endregion

    // region markNotificationsRead
    @Test
    fun `given notifications, when mark as read succeeds, then updates database`() = runTest {
        insertNotificationsFromJson()
        val notifications = store.getNotificationsForSite(site)
        val payload = MarkNotificationsReadPayload(notifications)
        whenever(mockNotificationRestClient.markNotificationRead(any()))
            .thenReturn(MarkNotificationsReadResponsePayload(notifications, success = true))

        val result = store.markNotificationsRead(payload)

        assertThat(result.success).isTrue()
    }

    @Test
    fun `given notifications, when mark as read succeeds, then notifications are marked read in db`() = runTest {
        insertNotificationsFromJson()
        val unreadNotification = store.getNotificationsForSite(site).first { !it.read }
        val payload = MarkNotificationsReadPayload(listOf(unreadNotification))
        whenever(mockNotificationRestClient.markNotificationRead(any()))
            .thenReturn(MarkNotificationsReadResponsePayload(listOf(unreadNotification), success = true))

        store.markNotificationsRead(payload)

        val updatedNotification = databaseRule.db.notificationDao().getNotificationByRemoteId(
            RemoteId(unreadNotification.remoteNoteId)
        )
        assertThat(updatedNotification?.read).isTrue()
    }

    @Test
    fun `given notifications, when mark as read fails, then returns error`() = runTest {
        insertNotificationsFromJson()
        val notifications = store.getNotificationsForSite(site)
        val payload = MarkNotificationsReadPayload(notifications)
        val errorPayload = MarkNotificationsReadResponsePayload().apply {
            error = NotificationError(NotificationErrorType.GENERIC_ERROR)
        }
        whenever(mockNotificationRestClient.markNotificationRead(any())).thenReturn(errorPayload)

        val result = store.markNotificationsRead(payload)

        assertThat(result.success).isFalse()
        assertThat(result.error).isNotNull()
    }

    // endregion

    // region updateNotificationSettingsFor
    @Test
    fun `given no device registered, when update settings, then returns failure`() = runTest {
        val settings = listOf(SiteNotificationSetting(site.siteId, true, true))

        val result = store.updateNotificationSettingsFor(settings)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given device registered, when update settings succeeds, then returns success`() = runTest {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext()).edit()
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, deviceId)
            .apply()
        val settings = listOf(SiteNotificationSetting(site.siteId, true, true))
        whenever(mockNotificationRestClient.disableNotificationsFor(any()))
            .thenReturn(Response.Success(Unit, emptyList()))

        val result = store.updateNotificationSettingsFor(settings)

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given device registered, when update settings fails, then returns failure`() = runTest {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext()).edit()
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, deviceId)
            .apply()
        val settings = listOf(SiteNotificationSetting(site.siteId, true, true))
        val networkError = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.NETWORK_ERROR, "Error"))
        whenever(mockNotificationRestClient.disableNotificationsFor(any()))
            .thenReturn(Response.Error(networkError))

        val result = store.updateNotificationSettingsFor(settings)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given device registered, when update settings, then calls rest client with correct payload`() = runTest {
        PreferenceUtils.getFluxCPreferences(ApplicationProvider.getApplicationContext()).edit()
            .putString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, deviceId)
            .apply()
        val settings = listOf(
            SiteNotificationSetting(siteId = 123L, newCommentEnabled = true, storeOrderEnabled = false)
        )
        whenever(mockNotificationRestClient.disableNotificationsFor(any()))
            .thenReturn(Response.Success(Unit, emptyList()))

        store.updateNotificationSettingsFor(settings)

        verify(mockNotificationRestClient).disableNotificationsFor(any())
    }
    // endregion

    // region onAction - synchronizeNotifications (via FETCH_NOTIFICATIONS)
    @Test
    fun `given no cached notifications, when fetch notifications, then fetches all from remote`() = runTest {
        store.onAction(NotificationActionBuilder.newFetchNotificationsAction(FetchNotificationsPayload()))

        verify(mockNotificationRestClient).fetchNotifications()
    }

    @Test
    fun `given cached notifications exist, when fetch notifications, then fetches hashes first`() = runTest {
        insertNotificationsFromJson()

        store.onAction(NotificationActionBuilder.newFetchNotificationsAction(FetchNotificationsPayload()))

        verify(mockNotificationRestClient).fetchNotificationHashes()
    }
    // endregion

    // region onAction - handleFetchNotificationHashesCompleted (via FETCHED_NOTIFICATION_HASHES)
    @Test
    fun `given error in hashes response, when handle hashes completed, then does not fetch notifications`() = runTest {
        val payload = FetchNotificationHashesResponsePayload().apply {
            error = NotificationError(NotificationErrorType.GENERIC_ERROR, "Error")
        }

        store.onAction(NotificationActionBuilder.newFetchedNotificationHashesAction(payload))

        verify(mockNotificationRestClient, never()).fetchNotifications(any())
    }

    @Test
    fun `given new hashes, when handle hashes completed, then fetches only new notifications`() = runTest {
        insertNotificationsFromJson()
        val newNoteId = 999L
        val existingNotification = databaseRule.db.notificationDao().getAllNotifications().first()
            .let { notificationMapper.toDomainModel(it) }
        val hashesMap = mapOf(
            existingNotification.remoteNoteId to existingNotification.noteHash,
            newNoteId to 123L
        )
        val payload = FetchNotificationHashesResponsePayload(hashesMap)

        store.onAction(NotificationActionBuilder.newFetchedNotificationHashesAction(payload))

        verify(mockNotificationRestClient).fetchNotifications(listOf(newNoteId))
    }

    @Test
    fun `given outdated hash, when handle hashes completed, then fetches updated notification`() = runTest {
        insertNotificationsFromJson()
        val existingNotification = databaseRule.db.notificationDao().getAllNotifications().first()
            .let { notificationMapper.toDomainModel(it) }
        val newHash = existingNotification.noteHash + 1
        val hashesMap = mapOf(existingNotification.remoteNoteId to newHash)
        val payload = FetchNotificationHashesResponsePayload(hashesMap)

        store.onAction(NotificationActionBuilder.newFetchedNotificationHashesAction(payload))

        verify(mockNotificationRestClient).fetchNotifications(listOf(existingNotification.remoteNoteId))
    }

    @Test
    fun `given notification removed from server, when handle hashes completed, then deletes from db`() = runTest {
        insertNotificationsFromJson()
        val countBefore = databaseRule.db.notificationDao().getNotificationsCount()
        val payload = FetchNotificationHashesResponsePayload(emptyMap())

        store.onAction(NotificationActionBuilder.newFetchedNotificationHashesAction(payload))

        val countAfter = databaseRule.db.notificationDao().getNotificationsCount()
        assertThat(countAfter).isLessThan(countBefore)
    }
    // endregion

    // region onAction - handleFetchNotificationsCompleted (via FETCHED_NOTIFICATIONS)
    @Test
    fun `given successful response, when handle notifications completed, then saves to db`() = runTest {
        val notifications = getNotificationsFromJson()
        val payload = FetchNotificationsResponsePayload(notifications)

        store.onAction(NotificationActionBuilder.newFetchedNotificationsAction(payload))

        val savedNotifications = databaseRule.db.notificationDao().getAllNotifications()
        assertThat(savedNotifications).hasSize(notifications.size)
    }

    @Test
    fun `given error response, when handle notifications completed, then does not save to db`() = runTest {
        val payload = FetchNotificationsResponsePayload().apply {
            error = NotificationError(NotificationErrorType.GENERIC_ERROR, "Error")
        }

        store.onAction(NotificationActionBuilder.newFetchedNotificationsAction(payload))

        val savedNotifications = databaseRule.db.notificationDao().getAllNotifications()
        assertThat(savedNotifications).isEmpty()
    }
    // endregion

    // region onAction - fetchNotification (via FETCH_NOTIFICATION)
    @Test
    fun `given remote note id, when fetch notification, then calls rest client`() = runTest {
        val remoteNoteId = 12345L
        val payload = FetchNotificationPayload(remoteNoteId)

        store.onAction(NotificationActionBuilder.newFetchNotificationAction(payload))

        verify(mockNotificationRestClient).fetchNotification(remoteNoteId)
    }
    // endregion

    // region onAction - handleFetchNotificationCompleted (via FETCHED_NOTIFICATION)
    @Test
    fun `given successful response, when handle notification completed, then saves to db`() = runTest {
        val notification = getNotificationsFromJson().first()
        val payload = FetchNotificationResponsePayload(notification)

        store.onAction(NotificationActionBuilder.newFetchedNotificationAction(payload))

        val savedNotification = databaseRule.db.notificationDao().getNotificationByRemoteId(
            RemoteId(notification.remoteNoteId)
        )
        assertThat(savedNotification).isNotNull
    }

    @Test
    fun `given error response, when handle notification completed, then does not save to db`() = runTest {
        val payload = FetchNotificationResponsePayload().apply {
            error = NotificationError(NotificationErrorType.NOT_FOUND, "Not found")
        }

        store.onAction(NotificationActionBuilder.newFetchedNotificationAction(payload))

        val savedNotifications = databaseRule.db.notificationDao().getAllNotifications()
        assertThat(savedNotifications).isEmpty()
    }

    @Test
    fun `given existing notification, when handle notification completed, then updates in db`() = runTest {
        insertNotificationsFromJson()
        val existingNotification = databaseRule.db.notificationDao().getAllNotifications().first()
            .let { notificationMapper.toDomainModel(it) }
        val updatedNotification = existingNotification.copy(title = "Updated Title")
        val payload = FetchNotificationResponsePayload(updatedNotification)

        store.onAction(NotificationActionBuilder.newFetchedNotificationAction(payload))

        val savedNotification = databaseRule.db.notificationDao().getNotificationByRemoteId(
            RemoteId(existingNotification.remoteNoteId)
        )
        assertThat(savedNotification?.title).isEqualTo("Updated Title")
    }
    // endregion

    // region onAction - updateNotification (via UPDATE_NOTIFICATION)
    @Test
    fun `given notification, when update notification, then saves to db`() = runTest {
        val notification = getNotificationsFromJson().first()

        store.onAction(NotificationActionBuilder.newUpdateNotificationAction(notification))

        val savedNotification = databaseRule.db.notificationDao().getNotificationByRemoteId(
            RemoteId(notification.remoteNoteId)
        )
        assertThat(savedNotification).isNotNull
    }

    @Test
    fun `given existing notification, when update notification, then updates in db`() = runTest {
        insertNotificationsFromJson()
        val existingNotification = databaseRule.db.notificationDao().getAllNotifications().first()
            .let { notificationMapper.toDomainModel(it) }
        val updatedNotification = existingNotification.copy(read = !existingNotification.read)

        store.onAction(NotificationActionBuilder.newUpdateNotificationAction(updatedNotification))

        val savedNotification = databaseRule.db.notificationDao().getNotificationByRemoteId(
            RemoteId(existingNotification.remoteNoteId)
        )
        assertThat(savedNotification?.read).isEqualTo(!existingNotification.read)
    }
    // endregion

    /* HELPER */

    private suspend fun insertNotificationsFromJson(): Int {
        val notesList = getNotificationsFromJson()
        val entities = notesList.map { notificationMapper.toEntity(it) }
        databaseRule.db.notificationDao().insertAll(entities)
        return notesList.size
    }

    private fun getNotificationsFromJson(): List<NotificationModel> {
        val jsonString = UnitTestUtils.getStringFromResourceFile(this.javaClass, API_RESPONSE)
            ?: error("Failed to load test resource: $API_RESPONSE")
        val apiResponse = NotificationTestUtils.parseNotificationsApiResponseFromJsonString(jsonString)
        return apiResponse.notes?.map {
            NotificationApiResponse.notificationResponseToNotificationModel(it)
        } ?: emptyList()
    }

    private suspend fun markAllNotificationsAsRead() {
        val notifications = databaseRule.db.notificationDao().getNotificationsForSite(RemoteId(site.siteId), null, null)
        notifications.forEach { notification ->
            databaseRule.db.notificationDao().insert(notification.copy(read = true))
        }
    }
}
