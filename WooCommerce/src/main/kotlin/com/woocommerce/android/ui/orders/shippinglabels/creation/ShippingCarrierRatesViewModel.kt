package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.Option
import com.woocommerce.android.model.ShippingRate.Option.ADULT_SIGNATURE
import com.woocommerce.android.model.ShippingRate.Option.DEFAULT
import com.woocommerce.android.model.ShippingRate.Option.SIGNATURE
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.PackageRateListItem
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.FEDEX
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.UPS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.USPS
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult.ShippingPackage
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
        private const val DEFAULT_RATE_OPTION = "default"
        private const val SIGNATURE_RATE_OPTION = "signature_required"
        private const val ADULT_SIGNATURE_RATE_OPTION = "adult_signature_required"
        private const val CARRIER_USPS_KEY = "usps"
        private const val CARRIER_UPS_KEY = "ups"
        private const val CARRIER_FEDEX_KEY = "fedex"
        private const val FLAT_RATE_KEY = "flat_rate"
        private const val FREE_SHIPPING_KEY = "free_shipping"
        private const val LOCAL_PICKUP_KEY = "local_pickup"
        private const val SHIPPING_METHOD_USPS_TITLE = "USPS"
        private const val SHIPPING_METHOD_DHL_TITLE = "DHL Express"
        private const val SHIPPING_METHOD_FEDEX_TITLE = "Fedex"
        private const val SHIPPING_METHOD_USPS_KEY = "wc_services_usps"
        private const val SHIPPING_METHOD_DHL_KEY = "wc_services_dhlexpress"
        private const val SHIPPING_METHOD_FEDEX_KEY = "wc_services_fedex"
        private const val KEY_SHIPPING_CARRIERS_PARAMETERS = "key_shipping_carriers_parameters"
    }
    private val arguments: ShippingCarrierRatesFragmentArgs by savedState.navArgs()

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_SHIPPING_CARRIERS_PARAMETERS, savedState)
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val _shippingRates = MutableLiveData<List<PackageRateListItem>>()
    val shippingRates: LiveData<List<PackageRateListItem>> = _shippingRates

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
            updateRates(generateRateModels(carrierRatesResult.model!!))

            var banner: String? = null
            if (arguments.order.shippingTotal > BigDecimal.ZERO) {
                banner = resourceProvider.getString(
                    R.string.shipping_label_shipping_carrier_flat_fee_banner_message,
                    arguments.order.shippingTotal.format(),
                    getShippingMethods(arguments.order).joinToString()
                )
            }
            viewState = viewState.copy(isEmptyViewVisible = false, bannerMessage = banner)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateRateModels(packageRates: List<ShippingPackage>): List<PackageRateListItem> {
        // different shipping options for each package (each package contains a list of different carrier options,
        // such as Express shipping, Priority shipping, etc.)
        return packageRates.mapIndexed { i, pkg ->
            // rates from across different carrier options, grouped by type (here, a default option without any extras)
            val defaultRates = pkg.shippingOptions.first { it.optionId == DEFAULT_RATE_OPTION }.rates

            // rate group (across different carrier options) that require signature option selected
            val ratesWithSignature = pkg.shippingOptions.first { it.optionId == SIGNATURE_RATE_OPTION }.rates

            // rate group (across different carrier options) that require adult signature option selected
            val ratesWithAdultSignature = pkg.shippingOptions.first { it.optionId == ADULT_SIGNATURE_RATE_OPTION }.rates

            val shippingRates = defaultRates.map { default ->
                val optionWithSignature = ratesWithSignature.firstOrNull { it.serviceId == default.serviceId }
                val optionWithAdultSignature = ratesWithAdultSignature.firstOrNull { it.serviceId == default.serviceId }
                val signaturePrice = optionWithSignature?.rate?.minus(default.rate)
                val adultSignaturePrice = optionWithAdultSignature?.rate?.minus(default.rate)

                // we can use the default rate as a base for most of the properties (these are the same for all
                // extra options for a particular carrier option) and we just calculate the price for each extra option
                ShippingRateItem(
                    serviceId = default.serviceId,
                    title = default.title,
                    deliveryEstimate = default.deliveryDays,
                    deliveryDate = default.deliveryDate,
                    carrier = getCarrier(default),
                    isTrackingAvailable = default.hasTracking,
                    isFreePickupAvailable = default.isPickupFree,
                    isInsuranceAvailable = default.insurance > BigDecimal.ZERO,
                    insuranceCoverage = default.insurance.format(),
                    options = mapOf(
                        DEFAULT to ShippingRate(
                            pkg.boxId,
                            default.rateId,
                            default.serviceId,
                            default.carrierId,
                            default.title,
                            default.deliveryDays,
                            default.rate,
                            default.rate.format(),
                            DEFAULT
                        ),
                        SIGNATURE to optionWithSignature?.let { rate ->
                            ShippingRate(
                                pkg.boxId,
                                rate.rateId,
                                rate.serviceId,
                                rate.carrierId,
                                rate.title,
                                default.deliveryDays,
                                signaturePrice ?: BigDecimal.ZERO,
                                signaturePrice.format(),
                                SIGNATURE
                            )
                        },
                        ADULT_SIGNATURE to optionWithAdultSignature?.let { rate ->
                            ShippingRate(
                                pkg.boxId,
                                rate.rateId,
                                rate.serviceId,
                                rate.carrierId,
                                rate.title,
                                default.deliveryDays,
                                adultSignaturePrice ?: BigDecimal.ZERO,
                                adultSignaturePrice.format(),
                                ADULT_SIGNATURE
                            )
                        }
                    ).filterValues { it != null } as Map<Option, ShippingRate>
                )
            }

            PackageRateListItem(
                pkg.boxId,
                itemCount = arguments.packages[i].items.size,
                rateOptions = shippingRates
            )
        }
    }

    private fun getShippingMethods(order: Order): List<String> {
        return order.shippingMethods.map {
            when (it.id) {
                FLAT_RATE_KEY -> resourceProvider.getString(R.string.shipping_label_shipping_method_flat_rate)
                FREE_SHIPPING_KEY -> resourceProvider.getString(R.string.shipping_label_shipping_method_free_shipping)
                LOCAL_PICKUP_KEY -> resourceProvider.getString(R.string.shipping_label_shipping_method_local_pickup)
                SHIPPING_METHOD_USPS_KEY -> SHIPPING_METHOD_USPS_TITLE
                SHIPPING_METHOD_FEDEX_KEY -> SHIPPING_METHOD_FEDEX_TITLE
                SHIPPING_METHOD_DHL_KEY -> SHIPPING_METHOD_DHL_TITLE
                else -> resourceProvider.getString(R.string.other)
            }
        }
    }

    private fun BigDecimal?.format(): String {
        return PriceUtils.formatCurrencyOrNull(this, parameters.currencyCode, currencyFormatter) ?: "0"
    }

    private fun getCarrier(it: Rate) =
        when (it.carrierId) {
            CARRIER_USPS_KEY -> USPS
            CARRIER_FEDEX_KEY -> FEDEX
            CARRIER_UPS_KEY -> UPS
            else -> throw IllegalArgumentException("Unsupported carrier ID: `${it.carrierId}`")
        }

    fun onShippingRateSelected(rate: ShippingRate) {
        shippingRates.value?.let { packageRates ->
            updateRates(
                packageRates.map {
                    // update the selected rate for the specific package
                    if (it.id == rate.packageId) {
                        it.updateSelectedRateAndCopy(rate)
                    } else {
                        it
                    }
                }
            )
        }
    }

    private fun updateRates(packageRates: List<PackageRateListItem>) {
        _shippingRates.value = packageRates
        viewState = viewState.copy(isDoneButtonVisible = packageRates.all { it.hasSelectedOption })
    }

    fun onDoneButtonClicked() {
        val selectedRates = shippingRates.value?.let { rates ->
            rates.map { it.selectedRate }
        }
        triggerEvent(ExitWithResult(selectedRates))
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

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ShippingCarrierRatesViewModel>
}
