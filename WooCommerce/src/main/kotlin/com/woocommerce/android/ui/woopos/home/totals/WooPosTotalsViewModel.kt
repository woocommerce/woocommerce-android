package com.woocommerce.android.ui.woopos.home.totals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
) : ViewModel() {
    init {
        listenUpEvents()
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.OrderDraftCreated -> {
                        // update view state
                    }
                    else -> Unit
                }
            }
        }
    }
}
