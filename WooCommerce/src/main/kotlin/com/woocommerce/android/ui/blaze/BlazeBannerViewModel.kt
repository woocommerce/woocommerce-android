package com.woocommerce.android.ui.blaze

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource.MORE_MENU_ITEM
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource.MY_STORE_BANNER
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeBannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val isBlazeEnabled: IsBlazeEnabled
) : ScopedViewModel(savedStateHandle) {

    private val _isBlazeBannerVisible = MutableLiveData(false)
    val isBlazeBannerVisible = _isBlazeBannerVisible

    init {
        launch {
            if (isBlazeEnabled()) {
                _isBlazeBannerVisible.value = true
            }
        }
    }

    fun onBlazeBannerDismissed() {
        TODO()
    }

    fun onTryBlazeBannerClicked() {
        triggerEvent(
            OpenBlazeEvent(
                url = isBlazeEnabled.buildUrlForSite(MORE_MENU_ITEM),
                source = MY_STORE_BANNER
            )
        )
    }

    data class OpenBlazeEvent(val url: String, val source: BlazeFlowSource) : MultiLiveEvent.Event()
}
