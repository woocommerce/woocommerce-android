package com.woocommerce.android.ui.sitepicker.sitevisibility

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
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
    private var initiallySelectedSiteIds: List<Long> = emptyList()
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
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onSaveTapped() {
        trackerWrapper.track(
            stat = AnalyticsEvent.SITE_PICKER_LIST_SAVE_BUTTON_TAPPED,
            properties = mapOf(
                "hidden_site_count" to _wooStoresState.value.wooStores.count { !it.isSelected }
            )
        )
        _wooStoresState.value = _wooStoresState.value.copy(isLoading = true)
        launch {
            try {
                runCatching {
                    val currentSelectedSiteIds = _wooStoresState.value.wooStores
                        .filter { it.isSelected }
                        .map { it.siteId }
                        .toSet()
                    val initialSelectedSiteIds = initiallySelectedSiteIds.toSet()
                    val hiddenSiteIds = initialSelectedSiteIds - currentSelectedSiteIds
                    val unhiddenSiteIds = currentSelectedSiteIds - initialSelectedSiteIds
                    val sitesById = availableWooSites.associateBy { it.siteId }
                    val wooPushRegisteredSiteIds = pushNotificationRepository.getWooPushRegisteredSiteIds()

                    updateWpComNotificationSettings(wooPushRegisteredSiteIds)
                    syncWooPushVisibilityChanges(
                        hiddenSiteIds = hiddenSiteIds,
                        unhiddenSiteIds = unhiddenSiteIds,
                        wooPushRegisteredSiteIds = wooPushRegisteredSiteIds,
                        sitesById = sitesById
                    )
                    onSaveSuccess()
                }.onFailure(::showSaveFailureDialog)
            } finally {
                _wooStoresState.value = _wooStoresState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun onSaveSuccess() {
        trackerWrapper.track(stat = AnalyticsEvent.SITE_PICKER_LIST_SAVING_SUCCESS)
        visibleSitesDataStore.updateSiteVisibilityStatus(
            _wooStoresState.value.wooStores.associate { it.siteId to it.isSelected }
        )
        triggerEvent(ExitWithResult(data = true))
    }

    private suspend fun updateWpComNotificationSettings(wooPushRegisteredSiteIds: Set<Long>) {
        val sitesToUpdate = _wooStoresState.value.wooStores
            .filterNot { it.siteId in wooPushRegisteredSiteIds }
            .map {
                SiteNotificationSetting(
                    siteId = it.siteId,
                    newCommentEnabled = it.isSelected,
                    storeOrderEnabled = it.isSelected
                )
            }

        if (sitesToUpdate.isNotEmpty()) {
            notificationsStore.updateNotificationSettingsFor(sitesToUpdate).getOrThrow()
        }
    }

    private suspend fun syncWooPushVisibilityChanges(
        hiddenSiteIds: Set<Long>,
        unhiddenSiteIds: Set<Long>,
        wooPushRegisteredSiteIds: Set<Long>,
        sitesById: Map<Long, SiteModel>
    ) {
        hiddenSiteIds
            .filter { it in wooPushRegisteredSiteIds }
            .mapNotNull(sitesById::get)
            .forEach { pushNotificationRepository.unregisterWooPushTokenForSite(it) }

        if (!featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1)) {
            return
        }

        val token = appPrefsWrapper.getFCMToken().takeIf { it.isNotEmpty() } ?: return

        unhiddenSiteIds
            .filterNot { it in wooPushRegisteredSiteIds }
            .mapNotNull(sitesById::get)
            .filter { pushNotificationRepository.shouldRegisterWooPushForSite(token, it.siteId) }
            .forEach {
                pushNotificationRepository.registerPushTokenInWooCoreSystem(
                    token = token,
                    selectedSite = it,
                    allowWpComFallback = false
                )
            }
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
                .map { it.siteId } != initiallySelectedSiteIds
        )
    }

    private suspend fun isSiteVisible(siteId: Long): Boolean =
        visibleSitesDataStore.isSiteVisible(siteId).first()

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
