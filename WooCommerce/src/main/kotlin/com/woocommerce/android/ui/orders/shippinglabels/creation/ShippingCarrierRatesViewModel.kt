package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.Option
import com.woocommerce.android.model.ShippingRate.Option.ADULT_SIGNATURE
import com.woocommerce.android.model.ShippingRate.Option.DEFAULT
import com.woocommerce.android.model.ShippingRate.Option.SIGNATURE
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.PackageRateListItem
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult.ShippingPackage
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.ShippingRatesApiResponse.ShippingOption.Rate
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ShippingCarrierRatesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val shippingLabelRepository: ShippingLabelRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedState) {
    companion object {
        private const val DEFAULT_RATE_OPTION = "default"
        private const val SIGNATURE_RATE_OPTION = "signature_required"
        private const val ADULT_SIGNATURE_RATE_OPTION = "adult_signature_required"
        private const val CARRIER_USPS_KEY = "usps"
        private const val CARRIER_UPS_KEY = "ups"
        private const val CARRIER_FEDEX_KEY = "fedex"
        private const val CARRIER_DHL_EXPRESS_KEY = "dhlexpress"
        private const val CARRIER_DHL_ECOMMERCE_KEY = "dhlecommerce"
        private const val CARRIER_DHL_ECOMMERCE_ASIA_KEY = "dhlecommerceasia"
    }
    private val arguments: ShippingCarrierRatesFragmentArgs by savedState.navArgs()

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val _shippingRates = MutableLiveData<List<PackageRateListItem>>()
    val shippingRates: LiveData<List<PackageRateListItem>> = _shippingRates

    init {
        if (shippingRates.value.isNullOrEmpty()) {
            loadShippingRates()
        }
    }

    private fun loadShippingRates() {
        launch {
            viewState = viewState.copy(isSkeletonVisible = true)
            loadRates()
            viewState = viewState.copy(isSkeletonVisible = false)
        }
    }

    private suspend fun loadRates() {
        val carrierRatesResult = shippingLabelRepository.getShippingRates(
            arguments.order,
            arguments.originAddress,
            arguments.destinationAddress,
            arguments.packages.toList(),
            arguments.customsPackages?.toList()
        )

        if (carrierRatesResult.isError) {
            viewState = viewState.copy(isEmptyViewVisible = true, isDoneButtonVisible = false)
            if (carrierRatesResult.error.original != NOT_FOUND) {
                triggerEvent(ShowSnackbar(string.shipping_label_shipping_carrier_rates_generic_error))
                triggerEvent(Exit)
            }
        } else {
            updateRates(generateRateModels(carrierRatesResult.model!!))

            val banner = when {
                arguments.order.shippingLines.isEmpty() -> null
                arguments.order.shippingTotal.isEqualTo(BigDecimal.ZERO) -> resourceProvider.getString(
                    R.string.shipping_label_shipping_carrier_shipping_method_banner_message,
                    arguments.order.shippingLines.first().methodTitle
                )
                else -> resourceProvider.getString(
                    R.string.shipping_label_shipping_carrier_flat_fee_banner_message,
                    arguments.order.shippingLines.first().methodTitle,
                    arguments.order.shippingTotal.format()
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
                val defaultDiscount = default.retailRate.minus(default.rate)

                val signature = ratesWithSignature.firstOrNull { it.serviceId == default.serviceId }
                val signatureFee = signature?.rate?.minus(default.rate)
                val signatureDiscount = signature?.retailRate?.minus(signature.rate) ?: BigDecimal.ZERO

                val adultSignature = ratesWithAdultSignature.firstOrNull { it.serviceId == default.serviceId }
                val adultSignatureFee = adultSignature?.rate?.minus(default.rate)
                val adultSignatureDiscount = adultSignature?.retailRate?.minus(adultSignature.rate) ?: BigDecimal.ZERO

                // we can use the default rate as a base for most of the properties (these are the same for all
                // extra options for a particular carrier option) and we just calculate the price for each extra option
                val options = mapOf(
                    DEFAULT to ShippingRate(
                        pkg.boxId,
                        default.shipmentId,
                        default.rateId,
                        default.serviceId,
                        default.carrierId,
                        default.title,
                        default.deliveryDays,
                        default.rate,
                        default.rate.format(),
                        defaultDiscount,
                        "",
                        DEFAULT
                    ),
                    SIGNATURE to signature?.let { option ->
                        ShippingRate(
                            pkg.boxId,
                            option.shipmentId,
                            option.rateId,
                            option.serviceId,
                            option.carrierId,
                            option.title,
                            default.deliveryDays,
                            option.rate,
                            option.rate.format(),
                            signatureDiscount,
                            signatureFee.format(),
                            SIGNATURE
                        )
                    },
                    ADULT_SIGNATURE to adultSignature?.let { option ->
                        ShippingRate(
                            pkg.boxId,
                            option.shipmentId,
                            option.rateId,
                            option.serviceId,
                            option.carrierId,
                            option.title,
                            default.deliveryDays,
                            option.rate,
                            option.rate.format(),
                            adultSignatureDiscount,
                            adultSignatureFee.format(),
                            ADULT_SIGNATURE
                        )
                    }
                ).filterValues { it != null } as Map<Option, ShippingRate>

                val selectedOption = arguments.selectedRates.firstOrNull {
                    it.packageId == pkg.boxId && it.serviceId == default.serviceId
                }?.option

                val insuranceFormatted = default.insurance?.toBigDecimalOrNull()
                    ?.let { resourceProvider.getString(R.string.shipping_label_rate_insurance_up_to, it.format()) }
                    ?: default.insurance

                ShippingRateItem(
                    serviceId = default.serviceId,
                    title = default.title,
                    deliveryEstimate = default.deliveryDays,
                    deliveryDate = default.deliveryDate,
                    carrier = getCarrier(default),
                    isTrackingAvailable = default.hasTracking,
                    isFreePickupAvailable = default.isPickupFree,
                    isInsuranceAvailable = !insuranceFormatted.isNullOrEmpty(),
                    insuranceCoverage = insuranceFormatted,
                    options = options,
                    selectedOption = selectedOption
                )
            }

            PackageRateListItem(
                pkg.boxId,
                shippingPackage = arguments.packages[i],
                rateOptions = shippingRates
            )
        }
    }

    // TODO: Once we start supporting countries other than the US, we'll need to verify what currency the shipping labels purchases use
    private fun BigDecimal?.format(): String {
        return when {
            this == null -> "N/A"
            this.isEqualTo(BigDecimal.ZERO) -> resourceProvider.getString(R.string.free)
            else -> "+${PriceUtils.formatCurrency(this, arguments.order.currency, currencyFormatter)}"
        }
    }

    private fun getCarrier(it: Rate) =
        when (it.carrierId) {
            CARRIER_USPS_KEY -> ShippingCarrier.USPS
            CARRIER_FEDEX_KEY -> ShippingCarrier.FEDEX
            CARRIER_UPS_KEY -> ShippingCarrier.UPS
            CARRIER_DHL_EXPRESS_KEY, CARRIER_DHL_ECOMMERCE_KEY, CARRIER_DHL_ECOMMERCE_ASIA_KEY -> ShippingCarrier.DHL
            else -> ShippingCarrier.UNKNOWN
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
        val isSkeletonVisible: Boolean = false,
        val isEmptyViewVisible: Boolean = false,
        val isDoneButtonVisible: Boolean = false
    ) : Parcelable
}
