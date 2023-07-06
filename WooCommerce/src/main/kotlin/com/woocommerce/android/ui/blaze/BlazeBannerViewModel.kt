package com.woocommerce.android.ui.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_BANNER_DISMISSED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource.MY_STORE_BANNER
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeBannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val productRepository: ProductListRepository,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {

    private val _isBlazeBannerVisible = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = false)
    val isBlazeBannerVisible = _isBlazeBannerVisible.asLiveData()

    private var blazeBannerSource: BlazeFlowSource = MY_STORE_BANNER

    init {
        launch {
            val publishedProducts = productRepository.getProductList()
                .filter { it.status == PUBLISH }
            if (isBlazeEnabled() &&
                publishedProducts.isNotEmpty() &&
                !appPrefsWrapper.shouldHideBlazeBanner
            ) {
                _isBlazeBannerVisible.value = true
            }
        }
    }

    fun setBlazeBannerSource(source: BlazeFlowSource) {
        blazeBannerSource = source
    }

    fun onBlazeBannerDismissed() {
        analyticsTrackerWrapper.track(
            stat = BLAZE_BANNER_DISMISSED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to blazeBannerSource.trackingName)
        )
        appPrefsWrapper.shouldHideBlazeBanner = true
        triggerEvent(DismissBlazeBannerEvent)
    }

    fun onTryBlazeBannerClicked() {
        analyticsTrackerWrapper.track(
            stat = BLAZE_ENTRY_POINT_TAPPED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to blazeBannerSource.trackingName)
        )
        triggerEvent(
            OpenBlazeEvent(
                url = isBlazeEnabled.buildUrlForSite(MY_STORE_BANNER),
                source = blazeBannerSource
            )
        )
    }

    data class OpenBlazeEvent(val url: String, val source: BlazeFlowSource) : MultiLiveEvent.Event()
    object DismissBlazeBannerEvent : MultiLiveEvent.Event()
}
