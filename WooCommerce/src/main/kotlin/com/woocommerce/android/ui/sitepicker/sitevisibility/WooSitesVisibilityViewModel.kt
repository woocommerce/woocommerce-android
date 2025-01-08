package com.woocommerce.android.ui.sitepicker.sitevisibility

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.NotificationSettingsUpdateError
import org.wordpress.android.fluxc.store.NotificationStore.SiteNotificationSetting
import javax.inject.Inject

@HiltViewModel
class WooSitesVisibilityViewModel @Inject constructor(
    private val sitePickerRepository: SitePickerRepository,
    private val selectedSite: SelectedSite,
    private val visibleSitesDataStore: VisibleWooSitesDataStore,
    private val notificationsStore: NotificationStore,
    private val trackerWrapper: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
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
            _wooStoresState.value = _wooStoresState.value.copy(
                wooStores = sitePickerRepository.getSites()
                    .filter { it.hasWooCommerce && it.siteId != selectedSite.get().siteId }
                    .map { it.toWooStoreUi(isSiteVisible(it.siteId)) }
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
            notificationsStore.updateNotificationSettingsFor(
                _wooStoresState.value.wooStores.map {
                    SiteNotificationSetting(
                        siteId = it.siteId,
                        newCommentEnabled = it.isSelected,
                        storeOrderEnabled = it.isSelected
                    )
                }
            ).fold(
                onSuccess = {
                    trackerWrapper.track(stat = AnalyticsEvent.SITE_PICKER_LIST_SAVING_SUCCESS)
                    visibleSitesDataStore.updateSiteVisibilityStatus(
                        _wooStoresState.value.wooStores
                            .associate { it.siteId to it.isSelected }
                    )
                    triggerEvent(ExitWithResult(data = true))
                },
                onFailure = {
                    if (it is NotificationSettingsUpdateError) {
                        trackerWrapper.track(
                            stat = AnalyticsEvent.SITE_PICKER_LIST_SAVING_FAILURE,
                            properties = mapOf(KEY_ERROR to it.type.toString())
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
            ).also {
                _wooStoresState.value = _wooStoresState.value.copy(isLoading = false)
            }
        }
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
