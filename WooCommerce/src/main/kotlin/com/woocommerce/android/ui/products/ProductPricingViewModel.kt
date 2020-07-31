package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date

class ProductPricingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val wooCommerceStore: WooCommerceStore,
    private val productRepository: ProductDetailRepository,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }

    private lateinit var initialState: ProductPricingViewState
    private val navArgs: ProductPricingFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ProductPricingViewState())
    private var viewState by viewStateData

    val parameters: SiteParameters by lazy {
        val params = savedState.get<SiteParameters>(KEY_PRODUCT_PARAMETERS) ?: loadParameters()
        savedState[KEY_PRODUCT_PARAMETERS] = params
        params
    }

    val pricingData
        get() = viewState.pricingData

    init {
        initializePricing()
    }

    private val hasChanges: Boolean
        get() = viewState != initialState

    private fun loadParameters(): SiteParameters {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get())?.let { settings ->
            return@let Pair(settings.weightUnit, settings.dimensionUnit)
        } ?: Pair(null, null)

        return SiteParameters(
            currencyCode,
            weightUnit,
            dimensionUnit,
            gmtOffset
        )
    }

    private fun initializePricing() {
        val decimals = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
            ?: DEFAULT_DECIMAL_PRECISION
        initialState = viewState.copy(
            currency = parameters.currencyCode,
            decimals = decimals,
            taxClassList = productRepository.getTaxClassesForSite(),
            pricingData = navArgs.pricingData
        )
        viewState = initialState
    }

    fun getTaxClassBySlug(slug: String): TaxClass? {
        return viewState.taxClassList?.filter { it.slug == slug }?.getOrNull(0)
    }

    fun onDataChanged(
        regularPrice: BigDecimal? = viewState.pricingData.regularPrice,
        salePrice: BigDecimal? = viewState.pricingData.regularPrice,
        isSaleScheduled: Boolean? = viewState.pricingData.isSaleScheduled,
        saleStartDate: Date? = viewState.pricingData.saleStartDate,
        saleEndDate: Date? = viewState.pricingData.saleEndDate,
        taxStatus: ProductTaxStatus? = viewState.pricingData.taxStatus,
        taxClass: TaxClass? = viewState.pricingData.taxClass
    ) {
        viewState = viewState.copy(
            pricingData = PricingData(
                regularPrice = regularPrice,
                salePrice = salePrice,
                isSaleScheduled = isSaleScheduled,
                saleStartDate = saleStartDate,
                saleEndDate = saleEndDate,
                taxStatus = taxStatus,
                taxClass = taxClass
            )
        )
        viewState = viewState.copy(isDoneButtonVisible = hasChanges)
    }

    fun onDoneButtonClicked() {
        AnalyticsTracker.track(
            Stat.PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to true)
        )

        triggerEvent(ExitWithResult(viewState.pricingData))
    }

    fun onExit() {
        if (hasChanges) {
            triggerEvent(ShowDiscardDialog(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
        } else {
            triggerEvent(Exit)
        }
    }

    fun onRemoveEndDateClicked() {
        onDataChanged(saleEndDate = null)
    }

    @Parcelize
    data class ProductPricingViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION,
        val taxClassList: List<TaxClass>? = null,
        val salePriceErrorMessage: Int? = null,
        val isDoneButtonVisible: Boolean? = null,
        val pricingData: PricingData = PricingData()
    ) : Parcelable {
        val isRemoveEndDateButtonVisible: Boolean
            get() = pricingData.saleEndDate != null
    }

    @Parcelize
    data class PricingData(
        val taxClass: TaxClass? = null,
        val taxStatus: ProductTaxStatus? = null,
        val isSaleScheduled: Boolean? = null,
        val saleStartDate: Date? = null,
        val saleEndDate: Date? = null,
        val regularPrice: BigDecimal? = null,
        val salePrice: BigDecimal? = null) : Parcelable {
        override fun equals(other: Any?): Boolean {
            val data = other as? PricingData
            return data?.let {
                taxClass == it.taxClass &&
                taxStatus == it.taxStatus &&
                isSaleScheduled == it.isSaleScheduled &&
                saleStartDate == it.saleStartDate &&
                saleEndDate == it.saleEndDate &&
                regularPrice isEquivalentTo it.regularPrice &&
                salePrice isEquivalentTo it.salePrice
            } ?: false
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    data class ExitWithResult(val data: PricingData) : Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductPricingViewModel>
}
