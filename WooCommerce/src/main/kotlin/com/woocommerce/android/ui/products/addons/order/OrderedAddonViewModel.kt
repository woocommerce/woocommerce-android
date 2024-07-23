package com.woocommerce.android.ui.products.addons.order

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.PRODUCT_ADDONS
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased
import javax.inject.Inject

@HiltViewModel
class OrderedAddonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val addonsRepository: AddonRepository,
    private val feedbackPrefs: FeedbackPrefs,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private val _orderedAddons = MutableLiveData<List<Addon>>()
    val orderedAddonsData: LiveData<List<Addon>> = _orderedAddons

    private val currentFeedbackSettings
        get() = feedbackPrefs.getFeatureFeedbackSettings(PRODUCT_ADDONS)
            ?: FeatureFeedbackSettings(PRODUCT_ADDONS)
                .apply { registerItself(feedbackPrefs) }

    /**
     * Provides the currencyCode for views who requires display prices
     */
    val currencyCode =
        parameterRepository
            .getParameters(KEY_PRODUCT_PARAMETERS, savedState)
            .currencyCode
            .orEmpty()

    fun start(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = viewState.copy(isSkeletonShown = true).let { viewState = it }.also {
        launch(dispatchers.computation) {
            addonsRepository.updateGlobalAddonsSuccessfully()
            loadOrderAddonsData(orderID, orderItemID, productID)
                ?.takeIf { it.isNotEmpty() }
                ?.let { dispatchResult(it) }
                ?: handleFailure()
        }
    }

    fun onGiveFeedbackClicked() {
        trackFeedback(AnalyticsTracker.VALUE_FEEDBACK_GIVEN)

        FeatureFeedbackSettings(
            PRODUCT_ADDONS,
            GIVEN
        ).registerItself(feedbackPrefs)

        triggerEvent(ShowSurveyView)
    }

    fun onDismissWIPCardClicked() {
        trackFeedback(AnalyticsTracker.VALUE_FEEDBACK_DISMISSED)

        FeatureFeedbackSettings(
            PRODUCT_ADDONS,
            DISMISSED
        ).registerItself(feedbackPrefs)

        viewState = viewState.copy(shouldDisplayFeedbackCard = false)
    }

    private suspend fun loadOrderAddonsData(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = addonsRepository.getOrderAddonsData(orderID, orderItemID, productID)
        ?.let { mapAddonsFromOrderAttributes(it.first, it.second) }

    private fun mapAddonsFromOrderAttributes(
        productAddons: List<Addon>,
        orderAttributes: List<Order.Item.Attribute>
    ): List<Addon> = orderAttributes.mapNotNull { findMatchingAddon(it, productAddons) }

    private fun findMatchingAddon(matchingTo: Order.Item.Attribute, addons: List<Addon>): Addon? =
        addons.firstOrNull { it.name == matchingTo.addonName }
            ?.asAddonWithSingleSelectedOption(matchingTo)

    private fun Addon.asAddonWithSingleSelectedOption(
        attribute: Order.Item.Attribute
    ): Addon {
        return when (this) {
            is Addon.HasOptions -> options.find { it.label == attribute.value }
                ?.takeIf { (this is Addon.MultipleChoice) or (this is Addon.Checkbox) }
                ?.handleOptionPriceType(attribute)
                ?.let { this.asSelectableAddon(it) }
                ?: mergeOrderAttributeWithAddon(this, attribute)
            else -> this
        }
    }

    /**
     * When displaying the price of an Ordered addon with the PercentageBased price
     * we don't want to display the percentage itself, but the price applied through the percentage.
     *
     * In this method we verify if that's the scenario and replace the percentage value with the price
     * defined by the Order Attribute, if it's not the case, the Addon is returned untouched.
     */
    private fun Addon.HasOptions.Option.handleOptionPriceType(
        attribute: Order.Item.Attribute
    ) = takeIf { it.price.priceType == PercentageBased }
        ?.copy(price = Adjusted(FlatFee, attribute.asAddonPrice))
        ?: this

    private fun Addon.asSelectableAddon(selectedOption: Addon.HasOptions.Option): Addon? =
        when (this) {
            is Addon.Checkbox -> this.copy(options = listOf(selectedOption))
            is Addon.MultipleChoice -> this.copy(options = listOf(selectedOption))
            else -> null
        }

    /**
     * If it isn't possible to find the respective option
     * through [Order.Item.Attribute.value] matching we will
     * have to merge the [Addon] data with the Attribute in order
     * to display the Ordered addon correctly, which is exactly
     * what this method does.
     *
     * Also, in this scenario there's no way to infer the image
     * information since it's something contained inside the options only
     */
    private fun mergeOrderAttributeWithAddon(
        addon: Addon,
        attribute: Order.Item.Attribute
    ): Addon {
        return when (addon) {
            is Addon.Checkbox -> addon.copy(options = prepareAddonOptionBasedOnAttribute(attribute))
            is Addon.MultipleChoice -> addon.copy(options = prepareAddonOptionBasedOnAttribute(attribute))
            else -> addon
        }
    }

    private fun prepareAddonOptionBasedOnAttribute(attribute: Order.Item.Attribute) = listOf(
        Addon.HasOptions.Option(
            label = attribute.value,
            price = Adjusted(
                priceType = FlatFee,
                value = attribute.asAddonPrice
            ),
            image = ""
        )
    )

    private suspend fun dispatchResult(result: List<Addon>) {
        withContext(dispatchers.main) {
            viewState = viewState.copy(
                isSkeletonShown = false,
                isLoadingFailure = false,
                shouldDisplayFeedbackCard = currentFeedbackSettings.feedbackState != DISMISSED
            )
            track(result)
            _orderedAddons.value = result
        }
    }

    private suspend fun handleFailure() {
        withContext(dispatchers.main) {
            viewState = viewState.copy(
                isSkeletonShown = false,
                isLoadingFailure = true,
                shouldDisplayFeedbackCard = false
            )
        }
    }

    private fun trackFeedback(feedbackAction: String) {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_PRODUCT_ADDONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to feedbackAction
            )
        )
    }

    private fun track(addons: List<Addon>) =
        addons.distinctBy { it.name }
            .map { it.name }
            .filter { it.isNotEmpty() }
            .joinToString(",")
            .let {
                AnalyticsTracker.track(
                    AnalyticsEvent.PRODUCT_ADDONS_ORDER_ADDONS_VIEWED,
                    mapOf(AnalyticsTracker.KEY_ADDONS to it)
                )
            }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingFailure: Boolean = false,
        val shouldDisplayFeedbackCard: Boolean = false
    ) : Parcelable

    object ShowSurveyView : Event()
}
