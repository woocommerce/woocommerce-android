package com.woocommerce.android.ui.prefs.notifications

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreReviewPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreStockPreferences
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class NotificationSettingsSharedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pushNotificationRepository: PushNotificationRepository,
    private val resourceProvider: ResourceProvider,
    private val coroutineDispatchers: CoroutineDispatchers
) : ScopedViewModel(savedStateHandle) {
    private val wooPushNotificationPreferences = MutableStateFlow<WooPushNotificationPreferences?>(null)
    private var savedWooPushNotificationPreferences: WooPushNotificationPreferences? = null
    private var saveInProgressWooPushNotificationPreferences: WooPushNotificationPreferences? = null
    private val saveNotificationPreferencesTrigger = MutableSharedFlow<Long>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
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
    private val displayedOrderThresholdAmount = MutableStateFlow(BigDecimal(DEFAULT_ORDER_THRESHOLD_AMOUNT))
    val newOrderNotificationSettingsViewState = combine(
        wooPushNotificationPreferences,
        displayedOrderThresholdAmount
    ) { preferences, thresholdAmount ->
        preferences?.toNewOrderNotificationSettingsViewState(thresholdAmount)
            ?: NewOrderNotificationSettingsViewState(thresholdAmount = thresholdAmount)
    }.asLiveData()
    private val _newReviewNotificationSettingsViewState = MutableStateFlow(NewReviewNotificationSettingsViewState())
    val newReviewNotificationSettingsViewState = _newReviewNotificationSettingsViewState.asLiveData()

    init {
        observeWooPushNotificationPreferences()
        fetchWooPushNotificationPreferences()
        observeNotificationPreferencesChanges()
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

        updateDisplayedWooPushNotificationPreferences(updatedPreferences)
    }

    fun savePendingNotificationPreferences() {
        saveNotificationPreferencesTrigger.tryEmit(0L)
    }

    fun onNewOrderNotificationsEnabledChanged(isEnabled: Boolean) {
        onNotificationTypeEnabledChanged(NotificationType.NEW_ORDERS, isEnabled)
    }

    fun onNewOrderNotificationPreferenceChanged(preference: NewOrderNotificationPreference) {
        val preferences = wooPushNotificationPreferences.value ?: return
        val updatedViewState = preferences.toNewOrderNotificationSettingsViewState(
            displayedThresholdAmount = displayedOrderThresholdAmount.value
        ).copy(notificationPreference = preference)
        updateDisplayedWooPushNotificationPreferences(
            preferences.copy(storeOrder = updatedViewState.toStoreOrderPreferences())
        )
    }

    fun onNewOrderThresholdAmountChanged(amount: BigDecimal) {
        val preferences = wooPushNotificationPreferences.value ?: return
        val thresholdAmount = amount.coerceAtLeast(MIN_ORDER_THRESHOLD_AMOUNT)
        val updatedViewState = preferences.toNewOrderNotificationSettingsViewState(
            displayedThresholdAmount = displayedOrderThresholdAmount.value
        ).copy(thresholdAmount = thresholdAmount)
        displayedOrderThresholdAmount.value = thresholdAmount
        updateDisplayedWooPushNotificationPreferences(
            preferences.copy(storeOrder = updatedViewState.toStoreOrderPreferences())
        )
    }

    fun onNewReviewNotificationsEnabledChanged(isEnabled: Boolean) {
        onNotificationTypeEnabledChanged(NotificationType.NEW_REVIEWS, isEnabled)
    }

    fun onNewReviewNotificationPreferenceChanged(preference: NewReviewNotificationPreference) {
        val preferences = wooPushNotificationPreferences.value ?: return
        val updatedViewState = _newReviewNotificationSettingsViewState.value.copy(notificationPreference = preference)
        updateDisplayedWooPushNotificationPreferences(
            preferences.copy(storeReview = updatedViewState.toStoreReviewPreferences())
        )
    }

    fun onNewReviewSelectedRatingChanged(rating: Int) {
        val preferences = wooPushNotificationPreferences.value ?: return
        val updatedViewState = _newReviewNotificationSettingsViewState.value.copy(
            selectedRating = rating.coerceIn(MIN_REVIEW_RATING, MAX_REVIEW_RATING)
        )
        updateDisplayedWooPushNotificationPreferences(
            preferences.copy(storeReview = updatedViewState.toStoreReviewPreferences())
        )
    }

    fun onNotificationTypeClicked(type: NotificationType) {
        when (type) {
            NotificationType.NEW_ORDERS -> triggerEvent(OpenNewOrderNotificationSettings)
            NotificationType.NEW_REVIEWS -> triggerEvent(OpenNewReviewNotificationSettings)
            NotificationType.STOCK -> triggerEvent(OpenStockNotificationSettings)
        }
    }

    private fun observeWooPushNotificationPreferences() {
        launch {
            pushNotificationRepository.observeWooNotificationPreferences()
                .collect { preferences ->
                    preferences?.let {
                        applyStoredWooPushNotificationPreferences(it)
                        _isNotificationSettingsLoading.value = false
                    }
                }
        }
    }

    private fun fetchWooPushNotificationPreferences() {
        launch {
            try {
                pushNotificationRepository.fetchWooNotificationPreferences()
                    .onSuccess { applyStoredWooPushNotificationPreferences(it) }
                    .onFailure { showFetchError() }
            } finally {
                _isNotificationSettingsLoading.value = false
            }
        }
    }

    private fun showFetchError() {
        triggerEvent(
            MultiLiveEvent.Event.ShowActionStringSnackbar(
                message = resourceProvider.getString(R.string.settings_notifs_error_fetch),
                actionText = resourceProvider.getString(R.string.retry),
            ) {
                fetchWooPushNotificationPreferences()
            }
        )
    }

    private fun observeNotificationPreferencesChanges() {
        launch {
            saveNotificationPreferencesTrigger
                .debounce { it }
                .conflate()
                .collect {
                    wooPushNotificationPreferences.value?.let { preferences ->
                        saveNotificationPreferences(preferences)
                    }
                }
        }
    }

    private suspend fun saveNotificationPreferences(preferencesToSave: WooPushNotificationPreferences) {
        val savedPreferences = savedWooPushNotificationPreferences ?: return
        val updateRequest = preferencesToSave.diffFrom(savedPreferences)
        if (updateRequest.isEmpty()) {
            return
        }

        saveInProgressWooPushNotificationPreferences = preferencesToSave
        // Once started, let the save request finish even if the screen is closed.
        withContext(NonCancellable + coroutineDispatchers.main) {
            pushNotificationRepository.updateWooNotificationPreferences(
                preferences = updateRequest
            )
        }.onSuccess {
            savedWooPushNotificationPreferences = preferencesToSave
        }.onFailure {
            if (wooPushNotificationPreferences.value != preferencesToSave) {
                // User changed preferences after this save started, so don't rollback over the newer state.
                return@onFailure
            }
            rollbackNotificationPreferences()
            showUpdateError(preferencesToSave)
        }
        saveInProgressWooPushNotificationPreferences = null
    }

    private fun showUpdateError(preferencesToSave: WooPushNotificationPreferences) {
        triggerEvent(
            MultiLiveEvent.Event.ShowActionStringSnackbar(
                message = resourceProvider.getString(R.string.settings_notifs_error_update),
                actionText = resourceProvider.getString(R.string.retry),
            ) {
                applyDisplayedWooPushNotificationPreferences(preferencesToSave)
                saveNotificationPreferencesTrigger.tryEmit(0L)
            }
        )
    }

    private fun applyStoredWooPushNotificationPreferences(preferences: WooPushNotificationPreferences) {
        if (hasUnsavedNotificationPreferences()) {
            return
        }
        if (saveInProgressWooPushNotificationPreferences != null) {
            return
        }

        savedWooPushNotificationPreferences = preferences
        applyDisplayedWooPushNotificationPreferences(preferences)
    }

    private fun updateDisplayedWooPushNotificationPreferences(preferences: WooPushNotificationPreferences) {
        applyDisplayedWooPushNotificationPreferences(preferences)
        saveNotificationPreferencesTrigger.tryEmit(NOTIFICATION_PREFERENCES_SAVE_DEBOUNCE_MS)
    }

    private fun applyDisplayedWooPushNotificationPreferences(preferences: WooPushNotificationPreferences) {
        wooPushNotificationPreferences.value = preferences
        _isNotificationTypeSelectionEnabled.value = true
        preferences.storeOrder?.minAmount?.let { displayedOrderThresholdAmount.value = it }
        preferences.storeReview?.let { reviewPreferences ->
            _newReviewNotificationSettingsViewState.update { it.copyWith(reviewPreferences) }
        }
        _notificationTypeItems.update { items ->
            items.map { item ->
                item.copy(isEnabled = preferences.isEnabled(item.type) ?: item.isEnabled)
            }
        }
    }

    private fun rollbackNotificationPreferences() {
        savedWooPushNotificationPreferences?.let { applyDisplayedWooPushNotificationPreferences(it) }
    }

    private fun hasUnsavedNotificationPreferences(): Boolean {
        return wooPushNotificationPreferences.value?.let { displayedPreferences ->
            savedWooPushNotificationPreferences?.let { savedPreferences ->
                displayedPreferences != savedPreferences
            }
        } ?: false
    }

    private fun WooPushNotificationPreferences.diffFrom(
        savedPreferences: WooPushNotificationPreferences
    ): WooPushNotificationPreferences = WooPushNotificationPreferences(
        storeOrder = storeOrder.takeIf { storeOrder != savedPreferences.storeOrder },
        storeReview = storeReview.takeIf { storeReview != savedPreferences.storeReview },
        storeStock = storeStock.takeIf { storeStock != savedPreferences.storeStock }
    )

    private fun WooPushNotificationPreferences.isEmpty(): Boolean =
        storeOrder == null && storeReview == null && storeStock == null

    private fun WooPushNotificationPreferences.toNewOrderNotificationSettingsViewState(
        displayedThresholdAmount: BigDecimal
    ) = NewOrderNotificationSettingsViewState(
        notificationsEnabled = storeOrder?.enabled ?: true,
        notificationPreference = if (storeOrder?.minAmount == null) {
            NewOrderNotificationPreference.AllOrders
        } else {
            NewOrderNotificationPreference.HighValueOrders
        },
        thresholdAmount = storeOrder?.minAmount ?: displayedThresholdAmount
    )

    private fun NewOrderNotificationSettingsViewState.toStoreOrderPreferences() = StoreOrderPreferences(
        enabled = notificationsEnabled,
        minAmount = when (notificationPreference) {
            NewOrderNotificationPreference.AllOrders -> null
            NewOrderNotificationPreference.HighValueOrders -> thresholdAmount
        }
    )

    private fun NewReviewNotificationSettingsViewState.copyWith(
        reviewPreferences: StoreReviewPreferences
    ): NewReviewNotificationSettingsViewState = copy(
        notificationsEnabled = reviewPreferences.enabled ?: notificationsEnabled,
        notificationPreference = if (reviewPreferences.maxRating == null) {
            NewReviewNotificationPreference.AllReviews
        } else {
            NewReviewNotificationPreference.RatingFilteredReviews
        },
        selectedRating = reviewPreferences.maxRating ?: selectedRating
    )

    private fun NewReviewNotificationSettingsViewState.toStoreReviewPreferences() = StoreReviewPreferences(
        enabled = notificationsEnabled,
        maxRating = when (notificationPreference) {
            NewReviewNotificationPreference.AllReviews -> null
            NewReviewNotificationPreference.RatingFilteredReviews -> selectedRating
        }
    )

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

    data class NewOrderNotificationSettingsViewState(
        val notificationsEnabled: Boolean = true,
        val notificationPreference: NewOrderNotificationPreference = NewOrderNotificationPreference.AllOrders,
        val thresholdAmount: BigDecimal = BigDecimal(DEFAULT_ORDER_THRESHOLD_AMOUNT)
    )

    enum class NewOrderNotificationPreference {
        AllOrders,
        HighValueOrders
    }

    data class NewReviewNotificationSettingsViewState(
        val notificationsEnabled: Boolean = true,
        val notificationPreference: NewReviewNotificationPreference = NewReviewNotificationPreference.AllReviews,
        val selectedRating: Int = DEFAULT_SELECTED_REVIEW_RATING
    )

    enum class NewReviewNotificationPreference {
        AllReviews,
        RatingFilteredReviews
    }

    enum class NotificationType {
        NEW_ORDERS,
        STOCK,
        NEW_REVIEWS
    }

    companion object {
        const val MIN_REVIEW_RATING = 1
        const val MAX_REVIEW_RATING = 5
        private const val DEFAULT_SELECTED_REVIEW_RATING = 2
        private const val DEFAULT_ORDER_THRESHOLD_AMOUNT = 100
        private val MIN_ORDER_THRESHOLD_AMOUNT = BigDecimal.ONE
        private const val NOTIFICATION_PREFERENCES_SAVE_DEBOUNCE_MS = 1000L
    }
}
