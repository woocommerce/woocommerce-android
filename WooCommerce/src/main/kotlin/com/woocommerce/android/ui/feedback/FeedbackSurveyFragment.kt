package com.woocommerce.android.ui.feedback

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls.CROWDSIGNAL_SURVEY
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.configureToolbarWithCloseButton
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.main.MainActivity
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureWebView()
        savedInstanceState?.let {
            webView.restoreState(it)
        } ?: webView.loadUrl(CROWDSIGNAL_SURVEY)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.invalidateOptionsMenu()
            it.title = getString(R.string.feedback_survey_request_title)
            (it as? MainActivity)?.configureToolbarWithCloseButton()
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.invalidateOptionsMenu()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    private fun showProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(R.string.web_view_loading_title),
            getString(R.string.web_view_loading_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() = webView.apply {
        showProgressDialog()
        settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webViewClient = surveyWebViewClient
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
