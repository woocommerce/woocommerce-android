package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.ui.products.ParameterRepository
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
class NewOrderNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val resourceProvider: ResourceProvider,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val showTestNotification: ShowTestNotification,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(
        ViewState(
            currencySymbol = parameterRepository.getParameters().currencySymbol.orEmpty(),
            newOrderNotificationSoundStatus = notificationChannelsHandler.checkNewOrderNotificationSound()
        )
    )
    val viewState = _viewState.asLiveData()

    fun refreshNotificationSettings() {
        _viewState.update {
            it.copy(newOrderNotificationSoundStatus = notificationChannelsHandler.checkNewOrderNotificationSound())
        }
    }

    fun onEnableChaChingSoundClicked() {
        analyticsTracker.track(
            AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_FIX_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to "new_order_settings")
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
        refreshNotificationSettings()
    }

    data class ViewState(
        val currencySymbol: String,
        val newOrderNotificationSoundStatus: NewOrderNotificationSoundStatus
    )
}
