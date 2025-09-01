package com.woocommerce.android.ui.woopos.settings.details.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_DOCUMENTATION_URL
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHelpDetailViewModel @Inject constructor(
    private val getSupportFacade: WooPosGetSupportFacade,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    fun onDocumentationClicked() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.ViewDocsTapped)
            _openUrlEvent.emit(WOO_POS_DOCUMENTATION_URL)
        }
    }

    fun onGetSupportClicked() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.GetSupportTapped)
        }
        getSupportFacade.openSupportForm()
    }
}
