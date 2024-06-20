package com.woocommerce.android.ui.products.quantityRules

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductQuantityRulesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {

    private val navArgs: ProductQuantityRulesFragmentArgs by savedState.navArgs()
    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            quantityRules = navArgs.quantityRules
        )
    )

    private val originalQuantityRules = navArgs.quantityRules

    private var viewState by viewStateData

    val quantityRules
        get() = viewState.quantityRules

    private val hasChanges: Boolean
        get() = quantityRules != originalQuantityRules

    fun onDataChanged(
        min: Int? = quantityRules.min,
        max: Int? = quantityRules.max,
        groupOf: Int? = quantityRules.groupOf
    ) {
        viewState = viewState.copy(
            quantityRules = quantityRules.copy(
                min = min,
                max = max,
                groupOf = groupOf
            )
        )
    }

    fun onExit() {
        analyticsTracker.track(
            navArgs.exitAnalyticsEvent,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges)
        )
        if (hasChanges) {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(quantityRules))
        } else {
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    @Parcelize
    data class ViewState(
        val quantityRules: QuantityRules = QuantityRules()
    ) : Parcelable
}
