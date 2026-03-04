package org.wordpress.android.fluxc.store

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient.DevicesDto
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient.SiteNotificationSettingDto
import org.wordpress.android.fluxc.persistence.NotificationMapper
import org.wordpress.android.fluxc.persistence.dao.NotificationDao
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationSettingErrorType.UnregisteredDevice
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.util.AppLog
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WpComPushNotificationStore @Inject internal constructor(
    dispatcher: Dispatcher,
    private val context: Context,
    private val notificationRestClient: NotificationRestClient,
    private val notificationDao: NotificationDao,
    private val notificationMapper: NotificationMapper,
    private val coroutineEngine: CoroutineEngine
) : Store(dispatcher) {
    companion object {
        const val WPCOM_PUSH_DEVICE_UUID = "NOTIFICATIONS_UUID_PREF_KEY"
        const val WPCOM_PUSH_DEVICE_SERVER_ID = "NOTIFICATIONS_SERVER_ID_PREF_KEY"
    }

    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    enum class NotificationAppKey(val value: String) {
        WOOCOMMERCE("com.woocommerce.android")
    }

    class RegisterDeviceResponsePayload(
        val deviceId: String? = null
    ) : Payload<DeviceRegistrationError>() {
        constructor(error: DeviceRegistrationError, deviceId: String? = null) : this(deviceId) {
            this.error = error
        }
    }

    class UnregisterDeviceResponsePayload() : Payload<DeviceUnregistrationError>() {
        constructor(error: DeviceUnregistrationError) : this() {
            this.error = error
        }
    }

    class DeviceRegistrationError(
        val type: DeviceRegistrationErrorType = DeviceRegistrationErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class DeviceRegistrationErrorType {
        INVALID_RESPONSE,
        MISSING_DEVICE_ID,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(DeviceRegistrationErrorType::name)
            fun fromString(type: String) = reverseMap[type.uppercase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class DeviceUnregistrationError(
        val type: DeviceUnregistrationErrorType = DeviceUnregistrationErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class DeviceUnregistrationErrorType { GENERIC_ERROR; }

    class FetchNotificationsPayload : Payload<BaseNetworkError>()

    @Suppress("unused")
    class FetchNotificationsResponsePayload(
        val notifs: List<NotificationModel> = emptyList()
    ) : Payload<NotificationError>() {
        constructor(error: NotificationError) : this() {
            this.error = error
        }
    }

    class FetchNotificationPayload(
        val remoteNoteId: Long
    ) : Payload<BaseNetworkError>()

    class FetchNotificationResponsePayload(
        val notification: NotificationModel? = null
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() {
            this.error = error
        }
    }

    class FetchNotificationHashesResponsePayload(
        val hashesMap: Map<Long, Long> = emptyMap()
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() {
            this.error = error
        }
    }

    class MarkNotificationsReadPayload(
        val notifications: List<NotificationModel>
    ) : Payload<BaseNetworkError>()

    class MarkNotificationsReadResponsePayload(
        val notifications: List<NotificationModel>? = null,
        val success: Boolean = false
    ) : Payload<NotificationError>() {
        @Suppress("unused")
        constructor(error: NotificationError) : this() {
            this.error = error
        }
    }

    class NotificationError(val type: NotificationErrorType, val message: String = "") :
        OnChangedError

    enum class NotificationErrorType {
        BAD_REQUEST,
        NOT_FOUND,
        AUTHORIZATION_REQUIRED,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(NotificationErrorType::name)
            fun fromString(type: String) = reverseMap[type.uppercase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    // OnChanged events
    class OnNotificationChanged : OnChanged<NotificationError>() {
        var causeOfChange: NotificationAction? = null
        var success: Boolean = true
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? NotificationAction ?: return
        when (actionType) {
            // remote actions
            NotificationAction.FETCH_NOTIFICATIONS ->
                coroutineEngine.launch(AppLog.T.API, this, "synchronizeNotifications") {
                    synchronizeNotifications()
                }
            NotificationAction.FETCH_NOTIFICATION ->
                coroutineEngine.launch(AppLog.T.API, this, "fetchNotification") {
                    fetchNotification(action.payload as FetchNotificationPayload)
                }
            // remote responses
            NotificationAction.FETCHED_NOTIFICATIONS ->
                coroutineEngine.launch(AppLog.T.API, this, "handleFetchNotificationsCompleted") {
                    handleFetchNotificationsCompleted(action.payload as FetchNotificationsResponsePayload)
                }
            NotificationAction.FETCHED_NOTIFICATION_HASHES ->
                coroutineEngine.launch(AppLog.T.API, this, "handleFetchNotificationHashesCompleted") {
                    handleFetchNotificationHashesCompleted(action.payload as FetchNotificationHashesResponsePayload)
                }
            NotificationAction.FETCHED_NOTIFICATION ->
                coroutineEngine.launch(AppLog.T.API, this, "handleFetchNotificationCompleted") {
                    handleFetchNotificationCompleted(action.payload as FetchNotificationResponsePayload)
                }
            // local actions
            NotificationAction.UPDATE_NOTIFICATION ->
                coroutineEngine.launch(AppLog.T.API, this, "updateNotification") {
                    updateNotification(action.payload as NotificationModel)
                }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, WpComPushNotificationStore::class.java.simpleName + " onRegister")
    }

    /**
     * Fetch all notifications for the given site.
     *
     * Filtering. Filtering is done by fetching all records that match the strings in [filterByType] OR
     * [filterBySubtype].
     *
     * @param site The [SiteModel] to fetch notifications for
     * @param filterByType Optional. A list of notification type strings to filter by
     * @param filterBySubtype Optional. A list of notification subtype strings to filter by
     */
    suspend fun getNotificationsForSite(
        site: SiteModel,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): List<NotificationModel> =
        notificationDao.getNotificationsForSite(RemoteId(site.siteId), filterByType, filterBySubtype)
            .map { notificationMapper.toDomainModel(it) }

    fun observeNotificationsForSite(
        site: SiteModel,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): Flow<List<NotificationModel>> =
        notificationDao.observeNotificationsForSite(RemoteId(site.siteId), filterByType, filterBySubtype)
            .map { entities -> entities.map { notificationMapper.toDomainModel(it) } }

    /**
     * Returns true if the given site has unread notifications
     *
     * @param site The [SiteModel] to check notifications for
     * @param filterByType Optional. A list of notification type strings to filter by
     * @param filterBySubtype Optional. A list of notification subtype strings to filter by
     */
    suspend fun hasUnreadNotificationsForSite(
        site: SiteModel,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): Boolean =
        notificationDao.hasUnreadNotificationsForSite(RemoteId(site.siteId), filterByType, filterBySubtype)

    suspend fun registerDevice(
        token: String,
        appKey: NotificationAppKey
    ): RegisterDeviceResponsePayload {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "registerDevice") {
            val uuid = preferences.getString(WPCOM_PUSH_DEVICE_UUID, null) ?: generateAndStoreUUID()

            notificationRestClient.registerDevice(
                fcmToken = token,
                appKey = appKey,
                uuid = uuid
            ).apply {
                if (isError || deviceId.isNullOrEmpty()) {
                    when (error.type) {
                        DeviceRegistrationErrorType.MISSING_DEVICE_ID ->
                            AppLog.e(
                                AppLog.T.NOTIFS,
                                "Server response missing device_id - registration skipped!"
                            )

                        DeviceRegistrationErrorType.GENERIC_ERROR ->
                            AppLog.e(
                                AppLog.T.NOTIFS,
                                "Error trying to register device: ${error.type} - ${error.message}"
                            )

                        DeviceRegistrationErrorType.INVALID_RESPONSE ->
                            AppLog.e(
                                AppLog.T.NOTIFS,
                                "Server response missing response object: ${error.type} - ${error.message}"
                            )
                    }
                } else {
                    preferences.edit().putString(WPCOM_PUSH_DEVICE_SERVER_ID, deviceId).apply()
                    AppLog.i(AppLog.T.NOTIFS, "Server response OK. Device ID: $deviceId")
                }
            }
        }
    }

    suspend fun unregisterWpComPushToken(): UnregisterDeviceResponsePayload {
        val payload = coroutineEngine.withDefaultContext(AppLog.T.API, this, "unregisterWpComDevice") {
            val deviceId = preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null)
            if (deviceId.isNullOrEmpty()) {
                UnregisterDeviceResponsePayload(
                    DeviceUnregistrationError(DeviceUnregistrationErrorType.GENERIC_ERROR, "Missing device id")
                )
            } else {
                notificationRestClient.unregisterDevice(deviceId)
            }
        }

        return handleUnregisteredDevicePayload(payload)
    }

    private fun handleUnregisteredDevicePayload(
        payload: UnregisterDeviceResponsePayload
    ): UnregisterDeviceResponsePayload {
        preferences.edit().apply {
            remove(WPCOM_PUSH_DEVICE_SERVER_ID)
            remove(WPCOM_PUSH_DEVICE_UUID)
            apply()
        }

        if (payload.isError) {
            with(payload.error) {
                AppLog.e(AppLog.T.NOTIFS, "Unregister device from WP.com pushes failed: $type - $message")
            }
        } else {
            AppLog.i(AppLog.T.NOTIFS, "Unregister device from WP.com pushes succeeded")
        }

        return payload
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            preferences.edit().putString(WPCOM_PUSH_DEVICE_UUID, it).apply()
        }
    }

    /**
     * Determines the optimal route for fetching new notifications and synchronizing the local database.
     *
     * No cached notifications in the database: skip fetching hashes and just fetch full notifications
     * from the remote.
     *
     * Cached notifications exist: fetch only the notification id and note_hash from the remote API
     * and use the smaller, faster results to build a list of notifications to be fetched, and delete
     * notifications in the database that no longer exist.
     */
    private suspend fun synchronizeNotifications() {
        val cachedCount = notificationDao.getNotificationsCount()

        if (cachedCount > 0) {
            // Fetch only the hashes to determine which notifications need to be fully fetched, and which
            // should be deleted
            notificationRestClient.fetchNotificationHashes()
        } else {
            // Fetch all notifications from the remote
            notificationRestClient.fetchNotifications()
        }
    }

    /**
     * Use the condensed map of newly fetched notification ids and hashes to determine which notifications are missing
     * from cache, require updates, or should be deleted.
     */
    private suspend fun handleFetchNotificationHashesCompleted(payload: FetchNotificationHashesResponsePayload) {
        if (payload.isError) {
            // Unable to synchronize notifications with remote. Emit error event.
            val onNotificationChanged = OnNotificationChanged().also {
                it.error = payload.error
                it.causeOfChange = NotificationAction.FETCH_NOTIFICATIONS
            }
            emitChange(onNotificationChanged)
            return
        }

        // Create a mutable copy of freshly fetched notifications map
        val notifsToFetch = payload.hashesMap.toMutableMap()

        // Pull cached notifications from the database and build a map of remoteNoteId to noteHash
        val existingNotifsByRemoteIdMap = notificationDao.getAllNotifications()
            .map { notificationMapper.toDomainModel(it) }
            .associateBy { it.remoteNoteId }

        // Collect IDs of notifications to delete (exist in cache but not in fetched hashes)
        val notifsToDelete = mutableListOf<RemoteId>()

        // Scrub the newly fetched list against the cached db records. Remove any entries for records that
        // do not require an update from the remote API
        existingNotifsByRemoteIdMap.entries.forEach { cached ->
            // Compare new note_hash values against cached values
            notifsToFetch[cached.key]?.let { newNoteHash ->
                if (cached.value.noteHash == newNoteHash) {
                    // Notifications are identical. No update needed, remove from
                    // list of notifs to fetch
                    notifsToFetch.remove(cached.key)
                }
            } ?: notifsToDelete.add(RemoteId(cached.key)) // Mark for deletion
        }

        // Delete notifications in a single batch operation
        if (notifsToDelete.isNotEmpty()) {
            notificationDao.deleteAllByRemoteIds(notifsToDelete)
        }

        // Fetch new and updated notifications from the remote api
        notificationRestClient.fetchNotifications(notifsToFetch.keys.toList())
    }

    private suspend fun handleFetchNotificationsCompleted(payload: FetchNotificationsResponsePayload) {
        val onNotificationChanged = if (payload.isError) {
            // Notification error
            OnNotificationChanged().also { it.error = payload.error }
        } else {
            // Save notifications to the database in a single batch operation
            val entities = payload.notifs.map { notificationMapper.toEntity(it) }
            notificationDao.insertAll(entities)
            OnNotificationChanged()
        }.apply {
            causeOfChange = NotificationAction.FETCH_NOTIFICATIONS
        }

        emitChange(onNotificationChanged)
    }

    private suspend fun fetchNotification(payload: FetchNotificationPayload) {
        notificationRestClient.fetchNotification(payload.remoteNoteId)
    }

    private suspend fun handleFetchNotificationCompleted(payload: FetchNotificationResponsePayload) {
        val onNotificationChanged = if (payload.isError) {
            OnNotificationChanged().also { it.error = payload.error }
        } else {
            // Save to the db
            payload.notification?.let {
                notificationDao.insert(notificationMapper.toEntity(it))
            }
            OnNotificationChanged()
        }.apply {
            causeOfChange = NotificationAction.FETCH_NOTIFICATION
        }
        emitChange(onNotificationChanged)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun markNotificationsRead(payload: MarkNotificationsReadPayload): OnNotificationChanged {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "markNotificationsRead") {
            val result = notificationRestClient.markNotificationRead(payload.notifications)
            // Update the notification in the database
            if (result.success) {
                result.notifications?.forEach {
                    // Just in case it wasn't set by the calling client
                    val note = it.copy(read = true)
                    notificationDao.insert(notificationMapper.toEntity(note))
                }
            }

            // Create and dispatch result
            if (result.isError) {
                OnNotificationChanged().apply {
                    error = result.error
                    success = false
                }
            } else {
                OnNotificationChanged().apply {
                    success = true
                }
            }
        }
    }

    private suspend fun updateNotification(payload: NotificationModel) {
        // save notification to the db
        notificationDao.insert(notificationMapper.toEntity(payload))
        val onNotificationChanged = OnNotificationChanged().apply {
            causeOfChange = NotificationAction.UPDATE_NOTIFICATION
        }
        emitChange(onNotificationChanged)
    }

    suspend fun updateNotificationSettingsFor(
        siteNotificationsEnabled: List<SiteNotificationSetting>
    ): Result<Unit> = coroutineEngine.withDefaultContext(
        AppLog.T.API,
        this,
        "Update notification settings for sites: ${siteNotificationsEnabled.joinToString(",")}}"
    ) {
        val deviceId = preferences.getString(WPCOM_PUSH_DEVICE_SERVER_ID, null)
            ?: return@withDefaultContext Result.failure(
                NotificationSettingsUpdateError(type = UnregisteredDevice)
            )

        val payload = siteNotificationsEnabled.map {
            SiteNotificationSettingDto(
                siteId = it.siteId,
                devices = listOf(
                    DevicesDto(
                        deviceId = deviceId,
                        newComment = it.newCommentEnabled,
                        storeOrder = it.storeOrderEnabled
                    )
                )
            )
        }
        when (val result = notificationRestClient.disableNotificationsFor(payload)) {
            is Success -> {
                AppLog.i(AppLog.T.NOTIFS, "Server response OK. Notifications disabled for device: $deviceId")
                Result.success(Unit)
            }

            is Response.Error -> {
                AppLog.e(
                    AppLog.T.NOTIFS,
                    "Error updating notification settings: ${result.error} - ${result.error.message}"
                )
                Result.failure(
                    NotificationSettingsUpdateError(
                        type = NotificationSettingErrorType.ApiError(
                            apiErrorMessage = result.error.message,
                            apiErrorCode = result.error.apiError
                        )
                    )
                )
            }
        }
    }

    data class SiteNotificationSetting(
        val siteId: Long,
        val newCommentEnabled: Boolean,
        val storeOrderEnabled: Boolean
    )

    data class NotificationSettingsUpdateError(
        val type: NotificationSettingErrorType
    ) : Exception(type.message)

    sealed class NotificationSettingErrorType(val message: String) {
        object UnregisteredDevice : NotificationSettingErrorType("Device not registered.")
        data class ApiError(
            val apiErrorMessage: String,
            val apiErrorCode: String? = null
        ) : NotificationSettingErrorType(apiErrorMessage)
    }
}
