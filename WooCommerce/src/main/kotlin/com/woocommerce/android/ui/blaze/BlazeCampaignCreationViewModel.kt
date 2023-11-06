package com.woocommerce.android.ui.blaze

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_INTRO_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource.INTRO_VIEW
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) : ScopedViewModel(savedStateHandle) {
    private companion object {
        const val BLAZE_CTA_TAPPED_TRACKED_KEY = "blaze_cta_tapped_tracked_key"
    }

    private val navArgs: BlazeCampaignCreationFragmentArgs by savedStateHandle.navArgs()

    private var currentBlazeStep = BlazeFlowStep.UNSPECIFIED
    private var isCompleted = false
    private var firstTimeLoading = false
    private var source = navArgs.source
    private var blazeCtaTappedTracked = savedStateHandle[BLAZE_CTA_TAPPED_TRACKED_KEY] ?: false

    private val isIntroDismissed = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "isIntroDismissed"
    )

    val viewState: LiveData<BlazeCreationViewState> = isIntroDismissed.map { introDismissed ->
        if (!introDismissed &&
            blazeCampaignsStore.getBlazeCampaigns(selectedSite.get()).campaigns.isEmpty()
        ) {
            analyticsTracker.track(
                stat = BLAZE_INTRO_DISPLAYED,
                properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to source.trackingName)
            )
            BlazeCreationViewState.Intro(
                onCreateCampaignClick = {
                    source = INTRO_VIEW
                    isIntroDismissed.value = true
                }
            )
        } else {
            if (!blazeCtaTappedTracked) {
                analyticsTracker.track(
                    stat = BLAZE_ENTRY_POINT_TAPPED,
                    properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to source.trackingName)
                )
                savedStateHandle[BLAZE_CTA_TAPPED_TRACKED_KEY] = true
            }
            BlazeCreationViewState.BlazeWebViewState(
                urlToLoad = navArgs.urlToLoad,
                source = source,
                onPageFinished = { url -> onPageFinished(url, source) }
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
        if (currentBlazeStep == BlazeFlowStep.STEP_5 && !isCompleted) {
            isCompleted = true
            analyticsTracker.track(
                stat = BLAZE_FLOW_COMPLETED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to source.trackingName,
                    AnalyticsTracker.KEY_BLAZE_STEP to currentBlazeStep.label
                )
            )
            triggerEvent(CampaignCreated)
        }
    }

    fun onClose() {
        if (viewState.value is BlazeCreationViewState.BlazeWebViewState && !isCompleted) {
            analyticsTracker.track(
                stat = BLAZE_FLOW_CANCELED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to source.trackingName,
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

    enum class BlazeFlowStep(val label: String) {
        CAMPAIGNS_LIST("campaigns-list"),
        PRODUCTS_LIST("products-list"),
        STEP_1("step-1"),
        STEP_2("step-2"),
        STEP_3("step-3"),
        STEP_4("step-4"),
        STEP_5("step-5"),
        UNSPECIFIED("unspecified");

        override fun toString() = label

        companion object {
            @JvmStatic
            fun fromString(source: String): BlazeFlowStep =
                values().firstOrNull { it.label.equals(source, ignoreCase = true) } ?: UNSPECIFIED
        }
    }

    object CampaignCreated : Event()
}
