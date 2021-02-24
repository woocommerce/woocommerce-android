package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.FEDEX
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.UPS
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.USPS
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.ShippingRatesApiResponse.ShippingOption.Rate
import java.math.BigDecimal

class ShippingCarrierRatesViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val shippingLabelRepository: ShippingLabelRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val FLAT_RATE_SHIPPING = "Flat rate"
        private const val CARRIER_USPS = "usps"
        private const val CARRIER_UPS = "ups"
        private const val CARRIER_FEDEX = "fedex"
        private const val KEY_SHIPPING_CARRIERS_PARAMETERS = "key_shipping_carriers_parameters"
    }
    private val arguments: ShippingCarrierRatesFragmentArgs by savedState.navArgs()

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_SHIPPING_CARRIERS_PARAMETERS, savedState)
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val _shippingRates = MutableLiveData<List<PackageRateList>>()
    val shippingRates: LiveData<List<PackageRateList>> = _shippingRates

    init {
        loadShippingRates()
    }

    private fun loadShippingRates() {
        launch {
            viewState = viewState.copy(isLoading = true)
            loadRates()
            viewState = viewState.copy(isLoading = false)
        }
    }

    private suspend fun loadRates() {
        val carrierRatesResult = shippingLabelRepository.getShippingRates(
            arguments.order,
            arguments.originAddress,
            arguments.destinationAddress,
            arguments.packages.toList()
        )

        if (carrierRatesResult.isError) {
            viewState = viewState.copy(isEmptyViewVisible = true, isDoneButtonVisible = false)
            if (carrierRatesResult.error.original != NOT_FOUND) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_shipping_carrier_rates_generic_error))
                triggerEvent(Exit)
            }
        } else {
            _shippingRates.value = carrierRatesResult.model!!.mapIndexed { i, pkg ->
                PackageRateList(
                    pkg.boxId,
                    title = pkg.shippingOptions.first().optionId,
                    itemCount = arguments.packages[i].items.size,
                    rateOptions = pkg.shippingOptions.first().rates.map {
                        ShippingRate(
                            it.rateId,
                            it.title,
                            it.deliveryDays,
                            it.rate,
                            getCarrier(it)
                        )
                    }
                )
            }

            var banner: String? = null
            if (arguments.order.shippingTotal > BigDecimal.ZERO) {
                val shipping = arguments.order.shippingMethodList.joinToString()
                banner = resourceProvider.getString(
                    R.string.shipping_label_shipping_carrier_flat_fee_banner_message,
                    PriceUtils.formatCurrency(
                        arguments.order.shippingTotal,
                        parameters.currencyCode,
                        currencyFormatter
                    ),
                    shipping
                )
            }
            viewState = viewState.copy(isEmptyViewVisible = false, isDoneButtonVisible = true, bannerMessage = banner)
        }
    }

    private fun getCarrier(it: Rate) =
        when (it.carrierId) {
            CARRIER_USPS -> USPS
            CARRIER_FEDEX -> FEDEX
            CARRIER_UPS -> UPS
            else -> throw IllegalArgumentException("Unsupported carrier ID: `${it.carrierId}`")
        }

    fun onShippingRateSelected(packageId: String, rateId: String) {
        val list = shippingRates.value?.toMutableList()
        list?.indexOfFirst { it.id == packageId }?.let { i ->
            list[i] = list[i].copy(selectedRate = list[i].rateOptions.first { it.id == rateId })
        }
        _shippingRates.value = list
    }

    fun onDoneButtonClicked() {
    }

    fun onExit() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val bannerMessage: String? = null,
        val isLoading: Boolean = false,
        val isEmptyViewVisible: Boolean = false,
        val isDoneButtonVisible: Boolean = false
    ) : Parcelable

    @Parcelize
    data class PackageRateList(
        val id: String,
        val title: String,
        val itemCount: Int,
        val rateOptions: List<ShippingRate>,
        val selectedRate: ShippingRate? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ShippingCarrierRatesViewModel>
}
