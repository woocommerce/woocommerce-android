package com.woocommerce.android.ui.products.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductShippingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val productRepository: ProductDetailRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }
    private val navArgs: ProductShippingFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            shippingData = navArgs.shippingData,
            isShippingClassSectionVisible = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_SHIPPING
        )
    )
    private var viewState by viewStateData

    val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_PRODUCT_PARAMETERS, savedState)
    }

    private val originalShippingData = navArgs.shippingData

    val shippingData
        get() = viewState.shippingData

    private val hasChanges: Boolean
        get() = shippingData != originalShippingData

    fun onDataChanged(
        weight: Float? = shippingData.weight,
        length: Float? = shippingData.length,
        width: Float? = shippingData.width,
        height: Float? = shippingData.height,
        shippingClassSlug: String? = shippingData.shippingClassSlug,
        shippingClassId: Long? = shippingData.shippingClassId,
        oneTimeShipping: Boolean? = shippingData.subscriptionShippingData?.oneTimeShipping
    ) {
        viewState = viewState.copy(
            shippingData = shippingData.copy(
                weight = weight,
                length = length,
                width = width,
                height = height,
                shippingClassSlug = shippingClassSlug,
                shippingClassId = shippingClassId
            )
        )
        oneTimeShipping?.let {
            viewState = viewState.copy(
                shippingData = shippingData.copy(
                    subscriptionShippingData = shippingData.subscriptionShippingData?.copy(
                        oneTimeShipping = it
                    )
                )
            )
        }
    }

    fun onExit() {
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges)
        )
        if (hasChanges) {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(shippingData))
        } else {
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun getShippingClassByRemoteShippingClassId(remoteShippingClassId: Long) =
        productRepository.getProductShippingClassByRemoteId(remoteShippingClassId)?.name
            ?: shippingData.shippingClassSlug ?: ""

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    @Parcelize
    data class ViewState(
        val shippingData: ShippingData = ShippingData(),
        val isShippingClassSectionVisible: Boolean? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isOneTimeShippingSectionVisible = shippingData.subscriptionShippingData != null
    }

    @Parcelize
    data class ShippingData(
        val weight: Float? = null,
        val length: Float? = null,
        val width: Float? = null,
        val height: Float? = null,
        val shippingClassSlug: String? = null,
        val shippingClassId: Long? = null,
        val subscriptionShippingData: SubscriptionShippingData? = null
    ) : Parcelable {

        @Parcelize
        data class SubscriptionShippingData(
            val oneTimeShipping: Boolean,
            val canEnableOneTimeShipping: Boolean
        ) : Parcelable
    }
}
