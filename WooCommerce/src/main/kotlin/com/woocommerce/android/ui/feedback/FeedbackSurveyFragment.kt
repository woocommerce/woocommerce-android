package com.woocommerce.android.ui.feedback

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls.CROWDSIGNAL_SURVEY
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.widgets.CustomProgressDialog
import kotlinx.android.synthetic.main.fragment_licenses.*

class FeedbackSurveyFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "feedback_survey"
        private const val QUERY_PARAMETER_MESSAGE = "msg"
        private const val SURVEY_DONE_QUERY_MESSAGE = "done"
    }

    private var progressDialog: CustomProgressDialog? = null
    private val surveyWebViewClient = SurveyWebViewClient()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_feedback_survey, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let {
            showProgressDialog()
            webView.settings.apply { javaScriptEnabled = true }
            webView.webViewClient = surveyWebViewClient
            webView.loadUrl(CROWDSIGNAL_SURVEY)
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

    private fun showProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(R.string.web_view_loading_title),
            getString(R.string.web_view_loading_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun completeSurvey() {
        FeedbackSurveyFragmentDirections.actionFeedbackSurveyFragmentToFeedbackCompletedFragment().apply {
            findNavController().navigateSafely(this)
        }
    }

    private inner class SurveyWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            hideProgressDialog()
            super.onPageFinished(view, url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            request?.url?.getQueryParameter(QUERY_PARAMETER_MESSAGE)
                ?.takeIf { it == SURVEY_DONE_QUERY_MESSAGE }
                ?.let { completeSurvey() }
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
}
