package com.woocommerce.android.ui.prefs.notifications

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val showTestNotification: ShowTestNotification,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _newOrderNotificationSoundStatus = MutableStateFlow(
        notificationChannelsHandler.checkNewOrderNotificationSound()
    )
    val newOrderNotificationSoundStatus = _newOrderNotificationSoundStatus.asLiveData()

    private val _notificationTypeItems = MutableStateFlow(
        listOf(
            NotificationTypeItem(
                type = NotificationType.NEW_ORDERS,
                title = R.string.settings_notifs_new_orders,
                subtitle = R.string.settings_notifs_new_orders_subtitle,
                isEnabled = true
            ),
            NotificationTypeItem(
                type = NotificationType.NEW_REVIEWS,
                title = R.string.settings_notifs_new_reviews,
                subtitle = R.string.settings_notifs_new_reviews_subtitle,
                isEnabled = true
            ),
            NotificationTypeItem(
                type = NotificationType.STOCK,
                title = R.string.settings_notifs_stock,
                subtitle = R.string.settings_notifs_stock_subtitle,
                isEnabled = true
            )
        )
    )
    val notificationTypeItems = _notificationTypeItems.asLiveData()

    fun onNotificationTypeEnabledChanged(type: NotificationType, isEnabled: Boolean) {
        _notificationTypeItems.update { items ->
            items.map {
                if (it.type == type) it.copy(isEnabled = isEnabled) else it
            }
        }
    }

    fun onNotificationTypeClicked(type: NotificationType) {
        if (type == NotificationType.NEW_ORDERS) {
            triggerEvent(OpenNewOrderNotificationSettings)
        }
    }

    fun onManageNotificationsClicked() {
        triggerEvent(OpenDeviceNotificationSettings)
    }

    fun onEnableChaChingSoundClicked() {
        analyticsTracker.track(
            AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_FIX_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to "settings")
        )
        notificationChannelsHandler.recreateNotificationChannel(NotificationChannelType.NEW_ORDER)
        triggerEvent(
            MultiLiveEvent.Event.ShowActionStringSnackbar(
                message = resourceProvider.getString(R.string.cha_ching_sound_succcess_snackbar),
                actionText = resourceProvider.getString(R.string.cha_ching_sound_succcess_snackbar_action),
                action = {
                    launch {
                        showTestNotification(
                            title = resourceProvider.getString(R.string.cha_ching_sound_test_notification_title),
                            message = resourceProvider.getString(
                                R.string.cha_ching_sound_test_notification_message
                            ),
                            channelType = NotificationChannelType.NEW_ORDER,
                            dismissDelay = 10.seconds
                        )
                    }
                }
            )
        )
        _newOrderNotificationSoundStatus.value = notificationChannelsHandler.checkNewOrderNotificationSound()
    }

    object OpenDeviceNotificationSettings : MultiLiveEvent.Event()
    object OpenNewOrderNotificationSettings : MultiLiveEvent.Event()

    data class NotificationTypeItem(
        val type: NotificationType,
        @StringRes val title: Int,
        @StringRes val subtitle: Int,
        val isEnabled: Boolean
    )

    enum class NotificationType {
        NEW_ORDERS,
        STOCK,
        NEW_REVIEWS
    }
}
