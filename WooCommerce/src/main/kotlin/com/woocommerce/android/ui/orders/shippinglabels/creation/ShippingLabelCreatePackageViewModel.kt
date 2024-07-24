package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelCreatePackageViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val arguments: ShippingLabelCreatePackageFragmentArgs by savedState.navArgs()

    fun onPackageCreated(madePackage: ShippingPackage) {
        val type = if (madePackage.category == ShippingPackage.CUSTOM_PACKAGE_CATEGORY) "custom" else "predefined"
        AnalyticsTracker.track(
            stat = AnalyticsEvent.SHIPPING_LABEL_PACKAGE_ADDED_SUCCESSFULLY,
            properties = mapOf("type" to type)
        )

        triggerEvent(
            ShowSnackbar(
                message = R.string.shipping_label_create_custom_package_success_message,
                args = arrayOf(madePackage.title)
            )
        )

        triggerEvent(
            ExitWithResult(
                ShippingPackageSelectorResult(
                    position = arguments.position,
                    selectedPackage = madePackage
                )
            )
        )
    }

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreatePackageViewState())

    @Parcelize
    data class ShippingLabelCreatePackageViewState(
        val selectedTab: PackageType = PackageType.CUSTOM
    ) : Parcelable

    enum class PackageType {
        CUSTOM,
        SERVICE
    }
}
