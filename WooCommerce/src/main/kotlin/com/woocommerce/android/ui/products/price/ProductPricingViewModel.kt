package com.woocommerce.android.ui.products.price

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.greaterThan
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ProductPricingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductDetailRepository,
    wooCommerceStore: WooCommerceStore,
    selectedSite: SelectedSite,
    parameterRepository: ParameterRepository,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }

    private var originalPricingData: PricingData
    private val navArgs: ProductPricingFragmentArgs by savedState.navArgs()
    private val isProductPricing = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_PRICING

    val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_PRODUCT_PARAMETERS, savedState)
    }

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState(pricingData = navArgs.pricingData))
    private var viewState by viewStateData

    val pricingData
        get() = viewState.pricingData

    private val hasChanges: Boolean
        get() = pricingData != originalPricingData

    init {
        val decimals = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
            ?: DEFAULT_DECIMAL_PRECISION

        viewState = viewState.copy(
            currency = parameters.currencySymbol,
            currencyPosition = parameters.currencyFormattingParameters?.currencyPosition,
            decimals = decimals,
            taxClassList = if (isProductPricing) productRepository.getTaxClassesForSite() else null,
            isTaxSectionVisible = isProductPricing
        )

        originalPricingData = navArgs.pricingData

        if (pricingData.isSubscription) {
            viewState = viewState.copy(
                pricingData = pricingData.copy(
                    subscriptionPeriod = viewState.pricingData.subscriptionPeriod ?: SubscriptionPeriod.Month,
                    subscriptionInterval = viewState.pricingData.subscriptionInterval ?: 1
                )
            )
        }
    }

    fun getTaxClassBySlug(slug: String): TaxClass? {
        return viewState.taxClassList?.filter { it.slug == slug }?.getOrNull(0)
    }

    fun onDataChanged(
        regularPrice: BigDecimal? = pricingData.regularPrice,
        salePrice: BigDecimal? = pricingData.salePrice,
        isSaleScheduled: Boolean? = pricingData.isSaleScheduled,
        saleStartDate: Date? = pricingData.saleStartDate,
        saleEndDate: Date? = pricingData.saleEndDate,
        taxStatus: ProductTaxStatus? = pricingData.taxStatus,
        taxClass: String? = pricingData.taxClass,
        subscriptionPeriod: SubscriptionPeriod? = pricingData.subscriptionPeriod,
        subscriptionInterval: Int? = pricingData.subscriptionInterval,
        subscriptionSignupFee: BigDecimal? = pricingData.subscriptionSignUpFee,
    ) {
        viewState = viewState.copy(
            pricingData = pricingData.copy(
                regularPrice = regularPrice,
                salePrice = salePrice,
                isSaleScheduled = isSaleScheduled,
                saleStartDate = saleStartDate,
                saleEndDate = fixEndDateIfNecessary(saleStartDate, saleEndDate),
                taxStatus = taxStatus,
                taxClass = taxClass,
                subscriptionPeriod = subscriptionPeriod,
                subscriptionInterval = subscriptionInterval,
                subscriptionSignUpFee = subscriptionSignupFee
            )
        )
    }

    fun onRegularPriceEntered(inputValue: BigDecimal?) {
        onDataChanged(regularPrice = inputValue)

        val salePrice = pricingData.salePrice
        viewState = if (salePrice.isSet() && inputValue.isSet() && salePrice.greaterThan(inputValue)) {
            viewState.copy(salePriceErrorMessage = string.product_pricing_update_sale_price_error)
        } else {
            viewState.copy(salePriceErrorMessage = 0)
        }
    }

    fun onScheduledSaleChanged(isSaleScheduled: Boolean) {
        if (isSaleScheduled && pricingData.salePrice.isNotSet()) {
            viewState = viewState.copy(salePriceErrorMessage = string.product_pricing_scheduled_sale_price_error)
        } else if (viewState.salePriceErrorMessage == string.product_pricing_scheduled_sale_price_error) {
            viewState = viewState.copy(salePriceErrorMessage = 0)
        }
        onDataChanged(isSaleScheduled = isSaleScheduled)
    }

    fun onSalePriceEntered(inputValue: BigDecimal?) {
        onDataChanged(salePrice = inputValue)

        val regularPrice = pricingData.regularPrice
        viewState = if (inputValue.isSet() && regularPrice.isSet() && inputValue.greaterThan(regularPrice)) {
            viewState.copy(salePriceErrorMessage = string.product_pricing_update_sale_price_error)
        } else if (pricingData.isSaleScheduled == true && inputValue.isNotSet()) {
            viewState.copy(salePriceErrorMessage = string.product_pricing_scheduled_sale_price_error)
        } else {
            viewState.copy(salePriceErrorMessage = 0)
        }
    }

    private fun fixEndDateIfNecessary(startDate: Date?, endDate: Date?): Date? {
        return endDate?.let {
            if (startDate != null && endDate.before(startDate)) {
                startDate
            } else {
                endDate
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    fun onExit() {
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges)
        )
        if (hasChanges && viewState.canSaveChanges) {
            val isSaleScheduled = pricingData.isSaleScheduled == true &&
                (pricingData.saleStartDate != null || pricingData.saleEndDate != null)
            val resultPricing = pricingData.copy(
                isSaleScheduled = isSaleScheduled,
                saleStartDate = if (isSaleScheduled) pricingData.saleStartDate else null,
                saleEndDate = if (isSaleScheduled) pricingData.saleEndDate else null
            )
            triggerEvent(ExitWithResult(resultPricing))
        } else {
            triggerEvent(Exit)
        }
    }

    fun onRemoveEndDateClicked() {
        onDataChanged(saleEndDate = null)
    }

    @Parcelize
    data class ViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION,
        val taxClassList: List<TaxClass>? = null,
        val salePriceErrorMessage: Int? = null,
        val pricingData: PricingData = PricingData(),
        val isTaxSectionVisible: Boolean? = null,
        private val currencyPosition: CurrencyPosition? = null
    ) : Parcelable {
        val isRemoveEndDateButtonVisible: Boolean
            get() = pricingData.saleEndDate != null
        val canSaveChanges: Boolean
            get() = salePriceErrorMessage == 0 || salePriceErrorMessage == null
        val isCurrencyPrefix: Boolean
            get() = currencyPosition == LEFT || currencyPosition == LEFT_SPACE
    }

    @Parcelize
    data class PricingData(
        val taxClass: String? = null,
        val taxStatus: ProductTaxStatus? = null,
        val isSaleScheduled: Boolean? = null,
        val saleStartDate: Date? = null,
        val saleEndDate: Date? = null,
        val regularPrice: BigDecimal? = null,
        val salePrice: BigDecimal? = null,
        val isSubscription: Boolean = false,
        val subscriptionPeriod: SubscriptionPeriod? = null,
        val subscriptionInterval: Int? = null,
        val subscriptionSignUpFee: BigDecimal? = null,
    ) : Parcelable {
        override fun equals(other: Any?): Boolean {
            val data = other as? PricingData
            return data?.let {
                taxClass == it.taxClass &&
                    taxStatus == it.taxStatus &&
                    isSaleScheduled == it.isSaleScheduled &&
                    saleStartDate == it.saleStartDate &&
                    saleEndDate == it.saleEndDate &&
                    regularPrice isEquivalentTo it.regularPrice &&
                    salePrice isEquivalentTo it.salePrice &&
                    isSubscription == it.isSubscription &&
                    subscriptionPeriod == it.subscriptionPeriod &&
                    subscriptionInterval == it.subscriptionInterval &&
                    subscriptionSignUpFee isEquivalentTo it.subscriptionSignUpFee
            } ?: false
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }
}
