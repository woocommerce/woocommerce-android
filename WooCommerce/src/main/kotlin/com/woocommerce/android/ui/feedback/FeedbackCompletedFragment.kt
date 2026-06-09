package com.woocommerce.android.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.analytics.AnalyticsEvent.SURVEY_SCREEN
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_COMPLETED
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.help.HelpOrigin.FEEDBACK_SURVEY
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus

class FeedbackCompletedFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val surveyType by lazy {
        navArgs<FeedbackCompletedFragmentArgs>().value.surveyType
    }

    private val feedbackContext by lazy {
        surveyType.feedbackContext
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            FeedbackCompletedScreen(
                onCloseClick = { findNavController().navigateUp() },
                onBackToStoreClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
                onContactUsClick = { activity?.startHelpActivity(FEEDBACK_SURVEY) },
            )
        }
    }

    override fun onResume() {
        super.onResume()

        trackSurveyCompletedScreenAnalytics()
    }

    override fun onStop() {
        super.onStop()
        activity?.invalidateOptionsMenu()
    }

    private fun trackSurveyCompletedScreenAnalytics() {
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(
            SURVEY_SCREEN,
            mapOf(
                KEY_FEEDBACK_CONTEXT to feedbackContext,
                KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_COMPLETED
            )
        )
    }

    companion object {
        const val TAG = "survey_completed"
    }
}
