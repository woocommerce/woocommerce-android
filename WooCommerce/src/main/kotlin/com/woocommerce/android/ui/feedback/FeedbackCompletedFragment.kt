package com.woocommerce.android.ui.feedback

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.SURVEY_SCREEN
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_COMPLETED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_GENERAL_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_PRODUCT_M3_CONTEXT
import com.woocommerce.android.databinding.FragmentFeedbackCompletedBinding
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.help.HelpOrigin.FEEDBACK_SURVEY
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.feedback.SurveyType.MAIN
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.widgets.WooClickableSpan

class FeedbackCompletedFragment : BaseFragment(R.layout.fragment_feedback_completed) {
    companion object {
        const val TAG = "survey_completed"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val feedbackContext by lazy {
        (navArgs<FeedbackCompletedFragmentArgs>().value).let {
            if (it.surveyType == MAIN) {
                VALUE_FEEDBACK_GENERAL_CONTEXT
            } else {
                VALUE_FEEDBACK_PRODUCT_M3_CONTEXT
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentFeedbackCompletedBinding.bind(view)
        val contactUsText = getString(R.string.feedback_completed_contact_us)
        binding.completionHelpGuide.setClickableText(
            content = getString(R.string.feedback_completed_description, contactUsText),
            clickableContent = contactUsText,
            clickAction = WooClickableSpan { activity?.startHelpActivity(FEEDBACK_SURVEY) }
        )
        binding.btnBackToStore.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        setupToolbar(binding.toolbar)
    }

    private fun setupToolbar(toolbar: MaterialToolbar) {
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(R.string.feedback_completed_title)
        toolbar.setNavigationIcon(R.drawable.ic_gridicons_cross_24dp)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        activity?.invalidateOptionsMenu()
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
}
