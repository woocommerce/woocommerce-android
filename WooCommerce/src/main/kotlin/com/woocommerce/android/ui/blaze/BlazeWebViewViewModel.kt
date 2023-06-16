package com.woocommerce.android.ui.blaze

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_FLOW_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.Companion.BLAZEPRESS_WIDGET
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class BlazeWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeWebViewFragmentArgs by savedStateHandle.navArgs()

    private var currentBlazeStep = BlazeFlowStep.UNSPECIFIED
    private var isCompleted = false
    val viewState = navArgs.let {
        BlazeWebViewState(
            urlToLoad = it.urlToLoad,
            source = it.source
        )
    }

    init {
        analyticsTracker.track(
            stat = BLAZE_FLOW_STARTED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to viewState.source.trackingName)
        )
    }

    fun onUrlLoaded(url: String) {
        Log.d("BlazeWebViewViewModel", "onUrlLoaded: $url")
        currentBlazeStep = extractCurrentStep(url)
        if (currentBlazeStep == BlazeFlowStep.STEP_5) {
            isCompleted = true
            analyticsTracker.track(
                stat = BLAZE_FLOW_COMPLETED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to viewState.source.trackingName,
                    AnalyticsTracker.KEY_BLAZE_STEP to currentBlazeStep.label
                )
            )
        }
    }

    fun onClose() {
        if (!isCompleted) {
            analyticsTracker.track(
                stat = BLAZE_FLOW_CANCELED,
                properties = mapOf(
                    AnalyticsTracker.KEY_BLAZE_SOURCE to viewState.source.trackingName,
                    AnalyticsTracker.KEY_BLAZE_STEP to currentBlazeStep.label
                )
            )
        }
        triggerEvent(Exit)
    }

    private fun extractCurrentStep(url: String): BlazeFlowStep {
        val uri = Uri.parse(url)
        return when {
            uri.fragment != null -> BlazeFlowStep.fromString(url)
            findQueryParameter(uri.toString(), BLAZEPRESS_WIDGET) != null -> BlazeFlowStep.STEP_1
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

    @Suppress("SameParameterValue")
    private fun findQueryParameter(uri: String, parameterName: String): String? {
        val queryParams = uri.split("\\?".toRegex()).drop(1).joinToString("")
        val parameterRegex = "(^|&)${parameterName}=([^&]*)".toRegex()

        val parameterMatchResult = parameterRegex.find(queryParams)

        return parameterMatchResult?.groupValues?.getOrNull(2)
    }

    data class BlazeWebViewState(
        val urlToLoad: String,
        val source: BlazeFlowSource
    )

    enum class BlazeFlowStep(val label: String, val trackingName: String) {
        CAMPAIGNS_LIST("campaigns_list", "campaigns_list"),
        PRODUCTS_LIST("products_list", "posts_list"),
        STEP_1("step-1", "step_1"),
        STEP_2("step-2", "step_2"),
        STEP_3("step-3", "step_3"),
        STEP_4("step-4", "step_4"),
        STEP_5("step-5", "step_5"),
        UNSPECIFIED("unspecified", "unspecified");

        override fun toString() = label

        companion object {
            @JvmStatic
            fun fromString(strSource: String?): BlazeFlowStep =
                strSource?.let { source ->
                    values().firstOrNull { it.label.equals(source, ignoreCase = true) }
                } ?: UNSPECIFIED
        }
    }
}
