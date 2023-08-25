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
            addonsRepository.loadItemAddons(orderID, orderItemID, productID)
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
