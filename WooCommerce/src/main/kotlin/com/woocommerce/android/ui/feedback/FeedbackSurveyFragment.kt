package com.woocommerce.android.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import kotlinx.android.synthetic.main.fragment_licenses.*

class FeedbackSurveyFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "feedback_survey"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_feedback_survey, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let {
            webView.loadUrl(BuildConfig.CROWDSIGNAL_URL)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.title = getString(R.string.feedback_survey_request_title)
            (it as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        }
    }
}
