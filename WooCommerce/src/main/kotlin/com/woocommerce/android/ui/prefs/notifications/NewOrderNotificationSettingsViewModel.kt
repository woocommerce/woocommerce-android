package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@HiltViewModel
class NewOrderNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val resourceProvider: ResourceProvider,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val showTestNotification: ShowTestNotification,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    selectedSite: SelectedSite,
    private val pushNotificationRepository: PushNotificationRepository,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val coroutineDispatchers: CoroutineDispatchers
) : ScopedViewModel(savedStateHandle) {
    private val site: SiteModel = selectedSite.get()

    private val _viewState = MutableStateFlow(
        ViewState(
            currencySymbol = parameterRepository.getParameters().currencySymbol.orEmpty(),
            newOrderNotificationSoundStatus = notificationChannelsHandler.checkNewOrderNotificationSound()
        )
    )
    private val saveOrderPreferencesTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var savedOrderPreferences = _viewState.value.toStoreOrderPreferences()

    val viewState = _viewState.asLiveData()

    init {
        observeCachedNotificationPreferences()
        observeOrderPreferencesChanges()
    }

    fun onNotificationsEnabledChanged(isEnabled: Boolean) {
        updateOrderPreferences(_viewState.value.copy(notificationsEnabled = isEnabled))
    }

    fun onNotificationPreferenceChanged(preference: NotificationPreference) {
        updateOrderPreferences(_viewState.value.copy(notificationPreference = preference))
    }

    fun onThresholdAmountChanged(amount: BigDecimal) {
        updateOrderPreferences(_viewState.value.copy(thresholdAmount = amount.coerceAtLeast(MIN_THRESHOLD_AMOUNT)))
    }

    fun savePendingOrderPreferences() {
        val orderPreferences = _viewState.value.toStoreOrderPreferences()
        appCoroutineScope.launch(coroutineDispatchers.main) {
            saveOrderPreferences(orderPreferences)
        }
    }

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

    private fun observeCachedNotificationPreferences() {
        launch {
            pushNotificationRepository.observeWooNotificationPreferences(site).collect { preferences ->
                preferences?.storeOrder?.let { orderPreferences ->
                    if (hasUnsavedOrderPreferences(_viewState.value.toStoreOrderPreferences())) {
                        savedOrderPreferences = orderPreferences
                    } else {
                        applyOrderPreferences(orderPreferences)
                    }
                }
            }
        }
    }

    private fun observeOrderPreferencesChanges() {
        launch {
            saveOrderPreferencesTrigger
                .debounce(ORDER_PREFERENCES_SAVE_DEBOUNCE_MS)
                .conflate()
                .collect { saveOrderPreferences(_viewState.value.toStoreOrderPreferences()) }
        }
    }

    private fun updateOrderPreferences(updatedViewState: ViewState) {
        if (updatedViewState == _viewState.value) return

        _viewState.value = updatedViewState
        saveOrderPreferencesTrigger.tryEmit(Unit)
    }

    private suspend fun saveOrderPreferences(orderPreferences: StoreOrderPreferences) {
        if (!hasUnsavedOrderPreferences(orderPreferences)) return

        val result = appCoroutineScope.async(coroutineDispatchers.main) {
            pushNotificationRepository.updateWooNotificationPreferences(
                site = site,
                preferences = WooPushNotificationPreferences(storeOrder = orderPreferences)
            )
        }.await()
        val hasNewerOrderPreferences = _viewState.value.toStoreOrderPreferences() != orderPreferences

        result.onSuccess { savedOrderPreferences = orderPreferences }
            .onFailure {
                if (!hasNewerOrderPreferences) {
                    _viewState.update { savedOrderPreferences.toViewState(it) }
                    showUpdateError(orderPreferences)
                }
            }
    }

    private fun showUpdateError(orderPreferences: StoreOrderPreferences) {
        triggerEvent(
            MultiLiveEvent.Event.ShowActionStringSnackbar(
                message = resourceProvider.getString(R.string.settings_notifs_error_update),
                actionText = resourceProvider.getString(R.string.retry),
            ) {
                _viewState.update { orderPreferences.toViewState(it) }
                saveOrderPreferencesTrigger.tryEmit(Unit)
            }
        )
    }

    private fun applyOrderPreferences(orderPreferences: StoreOrderPreferences) {
        val updatedViewState = orderPreferences.toViewState(_viewState.value)
        savedOrderPreferences = updatedViewState.toStoreOrderPreferences()
        _viewState.value = updatedViewState
    }

    private fun hasUnsavedOrderPreferences(orderPreferences: StoreOrderPreferences) =
        orderPreferences != savedOrderPreferences

    private fun StoreOrderPreferences.toViewState(current: ViewState): ViewState =
        current.copy(
            notificationsEnabled = enabled ?: current.notificationsEnabled,
            notificationPreference = if (minAmount == null) {
                NotificationPreference.AllOrders
            } else {
                NotificationPreference.HighValueOrders
            },
            thresholdAmount = minAmount ?: current.thresholdAmount
        )

    private fun ViewState.toStoreOrderPreferences() = StoreOrderPreferences(
        enabled = notificationsEnabled,
        minAmount = when (notificationPreference) {
            NotificationPreference.AllOrders -> null
            NotificationPreference.HighValueOrders -> thresholdAmount
        }
    )

    data class ViewState(
        val notificationsEnabled: Boolean = true,
        val notificationPreference: NotificationPreference = NotificationPreference.AllOrders,
        val thresholdAmount: BigDecimal = BigDecimal(DEFAULT_THRESHOLD_AMOUNT),
        val currencySymbol: String,
        val newOrderNotificationSoundStatus: NewOrderNotificationSoundStatus
    )

    enum class NotificationPreference {
        AllOrders,
        HighValueOrders
    }

    companion object {
        private const val DEFAULT_THRESHOLD_AMOUNT = 100
        private val MIN_THRESHOLD_AMOUNT = BigDecimal.ONE
        private const val ORDER_PREFERENCES_SAVE_DEBOUNCE_MS = 500L
    }
}
