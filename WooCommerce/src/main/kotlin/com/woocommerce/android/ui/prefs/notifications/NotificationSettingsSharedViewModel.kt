package com.woocommerce.android.ui.prefs.notifications

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreReviewPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreStockPreferences
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsSharedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    private val pushNotificationRepository: PushNotificationRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val wooPushNotificationPreferences = MutableStateFlow<WooPushNotificationPreferences?>(null)
    private val _isNotificationSettingsLoading = MutableStateFlow(true)
    val isNotificationSettingsLoading = _isNotificationSettingsLoading.asLiveData()
    private val _isNotificationTypeSelectionEnabled = MutableStateFlow(false)
    val isNotificationTypeSelectionEnabled = _isNotificationTypeSelectionEnabled.asLiveData()

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

    init {
        val site = selectedSite.get()
        observeWooPushNotificationPreferences(site)
        fetchWooPushNotificationPreferences(site)
    }

    fun onNotificationTypeEnabledChanged(type: NotificationType, isEnabled: Boolean) {
        val preferences = wooPushNotificationPreferences.value ?: return
        val updatedPreferences = when (type) {
            NotificationType.NEW_ORDERS -> preferences.copy(
                storeOrder = (preferences.storeOrder ?: StoreOrderPreferences()).copy(enabled = isEnabled)
            )
            NotificationType.NEW_REVIEWS -> preferences.copy(
                storeReview = (preferences.storeReview ?: StoreReviewPreferences()).copy(enabled = isEnabled)
            )
            NotificationType.STOCK -> preferences.copy(
                storeStock = (preferences.storeStock ?: StoreStockPreferences()).copy(enabled = isEnabled)
            )
        }

        applyWooPushNotificationPreferences(updatedPreferences)
    }

    fun onNotificationTypeClicked(type: NotificationType) {
        when (type) {
            NotificationType.NEW_ORDERS -> triggerEvent(OpenNewOrderNotificationSettings)
            NotificationType.NEW_REVIEWS -> triggerEvent(OpenNewReviewNotificationSettings)
            NotificationType.STOCK -> triggerEvent(OpenStockNotificationSettings)
        }
    }

    private fun observeWooPushNotificationPreferences(site: SiteModel) {
        launch {
            pushNotificationRepository.observeWooNotificationPreferences(site)
                .collect { preferences ->
                    preferences?.let {
                        applyWooPushNotificationPreferences(it)
                        _isNotificationSettingsLoading.value = false
                    }
                }
        }
    }

    private fun fetchWooPushNotificationPreferences(site: SiteModel) {
        launch {
            try {
                pushNotificationRepository.fetchWooNotificationPreferences(site)
                    .onSuccess { applyWooPushNotificationPreferences(it) }
                    .onFailure { showFetchError(site) }
            } finally {
                _isNotificationSettingsLoading.value = false
            }
        }
    }

    private fun showFetchError(site: SiteModel) {
        triggerEvent(
            MultiLiveEvent.Event.ShowActionStringSnackbar(
                message = resourceProvider.getString(R.string.settings_notifs_error_fetch),
                actionText = resourceProvider.getString(R.string.retry),
            ) {
                fetchWooPushNotificationPreferences(site)
            }
        )
    }

    private fun applyWooPushNotificationPreferences(preferences: WooPushNotificationPreferences) {
        wooPushNotificationPreferences.value = preferences
        _isNotificationTypeSelectionEnabled.value = true
        _notificationTypeItems.update { items ->
            items.map { item ->
                item.copy(isEnabled = preferences.isEnabled(item.type) ?: item.isEnabled)
            }
        }
    }

    private fun WooPushNotificationPreferences.isEnabled(type: NotificationType): Boolean? =
        when (type) {
            NotificationType.NEW_ORDERS -> storeOrder?.enabled
            NotificationType.NEW_REVIEWS -> storeReview?.enabled
            NotificationType.STOCK -> storeStock?.enabled
        }

    object OpenNewOrderNotificationSettings : MultiLiveEvent.Event()
    object OpenNewReviewNotificationSettings : MultiLiveEvent.Event()
    object OpenStockNotificationSettings : MultiLiveEvent.Event()

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
