package com.woocommerce.android.ui.blaze

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationFragmentArgs by savedStateHandle.navArgs()

    private var currentBlazeStep = BlazeFlowStep.UNSPECIFIED
    private var isCompleted = false
    private var firstTimeLoading = false

    private val isIntroDismissed = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "isIntroDismissed"
    )

    val viewState: LiveData<BlazeCreationViewState> = isIntroDismissed.map { introDismissed ->
        if (!introDismissed &&
            blazeCampaignsStore.getBlazeCampaigns(selectedSite.get()).campaigns.isEmpty() &&
            FeatureFlag.BLAZE_ITERATION_2.isEnabled()
        ) {
            BlazeCreationViewState.Intro(
                onCreateCampaignClick = { isIntroDismissed.value = true }
            )
        } else {
            BlazeCreationViewState.BlazeWebViewState(
                urlToLoad = navArgs.urlToLoad,
                source = navArgs.source,
                onPageFinished = { url -> onPageFinished(url, navArgs.source) }
            )
        }
    }.asLiveData()

    private fun onPageFinished(url: String, source: BlazeFlowSource) {
        if (!firstTimeLoading) {
            firstTimeLoading = true
            analyticsTracker.track(
                stat = BLAZE_FLOW_STARTED,
                properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to source.trackingName)
            )
        }
        currentBlazeStep = extractCurrentStep(url)
        if (currentBlazeStep == BlazeFlowStep.STEP_5) {
            isCompleted = true
            analyticsTracker.track(
                stat = BLAZE_FLOW_COMPLETED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to source.trackingName,
                    AnalyticsTracker.KEY_BLAZE_STEP to currentBlazeStep.label
                )
            )
            appPrefsWrapper.setBlazeBannerHidden(selectedSite.getSelectedSiteId(), hide = true)
        }
    }

    fun onClose() {
        if (viewState.value is BlazeCreationViewState.BlazeWebViewState && !isCompleted) {
            analyticsTracker.track(
                stat = BLAZE_FLOW_CANCELED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to navArgs.source.trackingName,
                    AnalyticsTracker.KEY_BLAZE_STEP to currentBlazeStep.label
                )
            )
        }
        triggerEvent(Exit)
    }

    private fun extractCurrentStep(url: String): BlazeFlowStep {
        val uri = Uri.parse(url)
        return when {
            uri.fragment != null -> BlazeFlowStep.fromString(uri.fragment!!)
            uri.getQueryParameter(BlazeUrlsHelper.BLAZEPRESS_WIDGET) != null -> BlazeFlowStep.STEP_1
            isAdvertisingCampaign(uri.toString()) -> BlazeFlowStep.CAMPAIGNS_LIST
            matchAdvertisingPath(uri.path) -> BlazeFlowStep.PRODUCTS_LIST
            else -> BlazeFlowStep.UNSPECIFIED
        }
    }

    private fun isAdvertisingCampaign(uri: String): Boolean {
        val pattern = "https://wordpress.com/advertising/\\w+/campaigns$".toRegex()
        return pattern.matches(uri)
    }

    private fun matchAdvertisingPath(path: String?): Boolean {
        path?.let {
            val advertisingRegex = "^/advertising/[^/]+(/posts)?$".toRegex()
            return advertisingRegex.matches(it)
        } ?: return false
    }

    sealed interface BlazeCreationViewState {
        data class Intro(
            val onCreateCampaignClick: () -> Unit,
        ) : BlazeCreationViewState

        data class BlazeWebViewState(
            val urlToLoad: String,
            val source: BlazeFlowSource,
            val onPageFinished: (String) -> Unit
        ) : BlazeCreationViewState
    }

    enum class BlazeFlowStep(val label: String, val trackingName: String) {
        CAMPAIGNS_LIST("campaigns_list", "campaigns_list"),
        PRODUCTS_LIST("products_list", "products_list"),
        STEP_1("step-1", "step_1"),
        STEP_2("step-2", "step_2"),
        STEP_3("step-3", "step_3"),
        STEP_4("step-4", "step_4"),
        STEP_5("step-5", "step_5"),
        UNSPECIFIED("unspecified", "unspecified");

        override fun toString() = label

        companion object {
            @JvmStatic
            fun fromString(source: String): BlazeFlowStep =
                values().firstOrNull { it.label.equals(source, ignoreCase = true) } ?: UNSPECIFIED
        }
    }
}
