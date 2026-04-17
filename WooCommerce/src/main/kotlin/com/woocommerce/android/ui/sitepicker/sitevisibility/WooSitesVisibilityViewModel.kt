package com.woocommerce.android.ui.sitepicker.sitevisibility

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationSettingsUpdateError
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.SiteNotificationSetting
import javax.inject.Inject

@HiltViewModel
class WooSitesVisibilityViewModel @Inject constructor(
    private val sitePickerRepository: SitePickerRepository,
    private val selectedSite: SelectedSite,
    private val visibleSitesDataStore: VisibleWooSitesDataStore,
    private val notificationsStore: WpComPushNotificationStore,
    private val pushNotificationRepository: PushNotificationRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val featureFlagRepository: FeatureFlagRepository,
    private val trackerWrapper: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private var availableWooSites: List<SiteModel> = emptyList()
    private var initiallySelectedSiteIds: Set<Long> = emptySet()
    private val _wooStoresState = MutableStateFlow(
        WooStoresUiState(
            wooStores = emptyList(),
            currentSite = selectedSite.get().toWooStoreUi(isSiteVisible = false),
            isSaveButtonEnabled = false,
            isLoading = false
        )
    )
    val viewState = _wooStoresState.asLiveData()

    init {
        launch {
            availableWooSites = sitePickerRepository.getSites()
                .filter { it.hasWooCommerce && it.siteId != selectedSite.get().siteId }

            _wooStoresState.value = _wooStoresState.value.copy(
                wooStores = availableWooSites.map { it.toWooStoreUi(isSiteVisible(it.siteId)) }
            )
            initiallySelectedSiteIds = _wooStoresState.value.wooStores
                .filter { it.isSelected }
                .map { it.siteId }
                .toSet()
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onSaveTapped() {
        trackSaveTapped()
        setLoading(isLoading = true)
        launch {
            try {
                performSave()
                    .onSuccess { onSaveSuccess() }
                    .onFailure(::showSaveFailureDialog)
            } finally {
                setLoading(isLoading = false)
            }
        }
    }

    private fun trackSaveTapped() {
        trackerWrapper.track(
            stat = AnalyticsEvent.SITE_PICKER_LIST_SAVE_BUTTON_TAPPED,
            properties = mapOf(
                "hidden_site_count" to _wooStoresState.value.wooStores.count { !it.isSelected }
            )
        )
    }

    private fun setLoading(isLoading: Boolean) {
        _wooStoresState.value = _wooStoresState.value.copy(isLoading = isLoading)
    }

    private suspend fun performSave(): Result<Unit> {
        val currentSelectedSiteIds = _wooStoresState.value.wooStores
            .filter { it.isSelected }
            .map { it.siteId }
            .toSet()
        val newlyVisibleSiteIds = currentSelectedSiteIds - initiallySelectedSiteIds
        val newlyHiddenSiteIds = initiallySelectedSiteIds - currentSelectedSiteIds
        val wooPushRegisteredSiteIds = pushNotificationRepository.getWooPushRegisteredSiteIds()

        unregisterNewlyHiddenWooPushSites(newlyHiddenSiteIds, wooPushRegisteredSiteIds)

        val registerResult = registerNewlyVisibleWooPushSites(newlyVisibleSiteIds, wooPushRegisteredSiteIds)
        if (registerResult.isFailure) return registerResult

        // Re-read to exclude sites just registered — WPCom notifications disabling was already handled for them
        // when registering them with Woo.
        return updateWpComNotificationSettings(
            excludedSiteIds = pushNotificationRepository.getWooPushRegisteredSiteIds()
        )
    }

    private suspend fun onSaveSuccess() {
        trackerWrapper.track(stat = AnalyticsEvent.SITE_PICKER_LIST_SAVING_SUCCESS)
        visibleSitesDataStore.updateSiteVisibilityStatus(
            _wooStoresState.value.wooStores.associate { it.siteId to it.isSelected }
        )
        triggerEvent(ExitWithResult(data = true))
    }

    private suspend fun updateWpComNotificationSettings(excludedSiteIds: Set<Long>): Result<Unit> {
        val sitesToUpdate = _wooStoresState.value.wooStores
            .filterNot { it.siteId in excludedSiteIds }
            .map { it.toNotificationSetting() }

        return if (sitesToUpdate.isNotEmpty()) {
            notificationsStore.updateNotificationSettingsFor(sitesToUpdate)
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun unregisterNewlyHiddenWooPushSites(
        newlyHiddenSiteIds: Set<Long>,
        wooPushRegisteredSiteIds: Set<Long>
    ) = coroutineScope {
        availableWooSites
            .filter { it.siteId in newlyHiddenSiteIds && it.siteId in wooPushRegisteredSiteIds }
            .map { async { pushNotificationRepository.unregisterWooPushTokenForSite(it) } }
            .awaitAll()
    }

    private suspend fun registerNewlyVisibleWooPushSites(
        newlyVisibleSiteIds: Set<Long>,
        wooPushRegisteredSiteIds: Set<Long>
    ): Result<Unit> = coroutineScope {
        if (!featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1)) {
            return@coroutineScope Result.success(Unit)
        }
        val token = appPrefsWrapper.getFCMToken().takeIf { it.isNotEmpty() }
            ?: return@coroutineScope Result.success(Unit)

        val results = availableWooSites
            .filter { it.siteId in newlyVisibleSiteIds && it.siteId !in wooPushRegisteredSiteIds }
            .map { site ->
                async {
                    if (pushNotificationRepository.shouldRegisterWooPushForSite(token, site.siteId)) {
                        pushNotificationRepository.registerPushTokenInWooCoreSystem(
                            token = token,
                            selectedSite = site,
                            allowWpComFallback = false
                        ).recoverCatching { error ->
                            // `rest_no_route` means the site doesn't have the wc-push-notifications
                            // endpoint — it will continue to use WPCom notifications instead.
                            if ((error as? WooException)?.error?.type == WooErrorType.API_NOT_FOUND) {
                                Unit
                            } else {
                                throw error
                            }
                        }
                    } else {
                        Result.success(Unit)
                    }
                }
            }
            .awaitAll()

        results.firstOrNull { it.isFailure } ?: Result.success(Unit)
    }

    private fun showSaveFailureDialog(error: Throwable) {
        if (error is NotificationSettingsUpdateError) {
            trackerWrapper.track(
                stat = AnalyticsEvent.SITE_PICKER_LIST_SAVING_FAILURE,
                properties = mapOf(KEY_ERROR to error.type.toString())
            )
        }
        triggerEvent(
            Event.ShowDialog(
                titleId = R.string.site_picker_edit_store_list_error_title,
                positiveButtonId = R.string.retry,
                positiveBtnAction = { dialog, _ ->
                    dialog.dismiss()
                    onSaveTapped()
                },
                negativeButtonId = R.string.cancel,
                negativeBtnAction = { dialog, _ ->
                    dialog.dismiss()
                    triggerEvent(Exit)
                }
            )
        )
    }

    fun onSiteTapped(wooStoreUi: WooStoreUi) {
        _wooStoresState.value = _wooStoresState.value.copy(
            wooStores = _wooStoresState.value.wooStores.map {
                when {
                    it.siteId == wooStoreUi.siteId -> it.copy(isSelected = !it.isSelected)
                    else -> it
                }
            }
        )
        _wooStoresState.value = _wooStoresState.value.copy(
            isSaveButtonEnabled = _wooStoresState.value.wooStores
                .filter { it.isSelected }
                .map { it.siteId }
                .toSet() != initiallySelectedSiteIds
        )
    }

    private suspend fun isSiteVisible(siteId: Long): Boolean =
        visibleSitesDataStore.isSiteVisible(siteId).first()

    private fun WooStoreUi.toNotificationSetting() = SiteNotificationSetting(
        siteId = siteId,
        newCommentEnabled = isSelected,
        storeOrderEnabled = isSelected
    )

    private fun SiteModel.toWooStoreUi(isSiteVisible: Boolean) = WooStoreUi(
        siteName = name,
        siteUrl = url,
        siteId = siteId,
        isSelected = isSiteVisible
    )

    data class WooStoresUiState(
        val wooStores: List<WooStoreUi>,
        val currentSite: WooStoreUi,
        val isSaveButtonEnabled: Boolean,
        val isLoading: Boolean
    )

    data class WooStoreUi(
        val siteName: String,
        val siteUrl: String,
        val siteId: Long,
        val isSelected: Boolean
    )
}
