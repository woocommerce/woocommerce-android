package com.woocommerce.android.ui.woopos.settings.details.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls.WOO_POS_DOCUMENTATION_URL
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHelpDetailViewModel @Inject constructor(
    private val getSupportFacade: WooPosGetSupportFacade,
) : ViewModel() {
    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    fun onProductLimitationsClicked() {
    }

    fun onDocumentationClicked() {
        viewModelScope.launch {
            _openUrlEvent.emit(WOO_POS_DOCUMENTATION_URL)
        }
    }

    fun onGetSupportClicked() {
        getSupportFacade.openSupportForm()
    }
}
