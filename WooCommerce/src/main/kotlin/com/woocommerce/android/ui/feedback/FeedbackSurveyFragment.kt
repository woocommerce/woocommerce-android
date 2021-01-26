package com.woocommerce.android.ui.feedback

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_CANCELED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_GENERAL_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_OPENED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_PRODUCT_M3_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SURVEY_SCREEN
import com.woocommerce.android.databinding.FragmentFeedbackSurveyBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.feedback.SurveyType.MAIN
import com.woocommerce.android.widgets.CustomProgressDialog

class FeedbackSurveyFragment : androidx.fragment.app.Fragment(R.layout.fragment_feedback_survey) {
    companion object {
        const val TAG = "feedback_survey"
        private const val QUERY_PARAMETER_MESSAGE = "msg"
        private const val SURVEY_DONE_QUERY_MESSAGE = "done"
    }

    private var progressDialog: CustomProgressDialog? = null
    private var surveyCompleted: Boolean = false
    private val surveyWebViewClient = SurveyWebViewClient()
    private val arguments: FeedbackSurveyFragmentArgs by navArgs()
    private val feedbackContext by lazy {
        if (arguments.surveyType == MAIN) VALUE_FEEDBACK_GENERAL_CONTEXT
        else VALUE_FEEDBACK_PRODUCT_M3_CONTEXT
    }

    private var _binding: FragmentFeedbackSurveyBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        _binding = FragmentFeedbackSurveyBinding.bind(view)

        configureWebView()
        savedInstanceState?.let {
            binding.webView.restoreState(it)
        } ?: binding.webView.loadUrl(arguments.surveyType.url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(
            SURVEY_SCREEN, mapOf(
            KEY_FEEDBACK_CONTEXT to feedbackContext,
            KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_OPENED
        )
        )

        activity?.let {
            it.invalidateOptionsMenu()
            it.title = getString(R.string.feedback_survey_request_title)
            (it as? AppCompatActivity)
                ?.supportActionBar
                ?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.invalidateOptionsMenu()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            binding.webView.restoreState(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroy() {
        if (surveyCompleted.not()) {
            AnalyticsTracker.track(
                SURVEY_SCREEN, mapOf(
                KEY_FEEDBACK_CONTEXT to feedbackContext,
                KEY_FEEDBACK_ACTION to VALUE_FEEDBACK_CANCELED
            )
            )
        }
        super.onDestroy()
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
    private fun configureWebView() = binding.webView.apply {
        showProgressDialog()
        settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webViewClient = surveyWebViewClient
    }

    /**
     * We use this dismissAllowingStateLoss for dialog dismissal to avoid any kind of commit operation
     * from the [FragmentManager] after the `onSaveInstanceState` is called
     */
    private fun hideProgressDialog() {
        progressDialog?.dismissAllowingStateLoss()
        progressDialog = null
    }

    private fun completeSurvey() {
        surveyCompleted = true
        FeedbackSurveyFragmentDirections
            .actionFeedbackSurveyFragmentToFeedbackCompletedFragment(arguments.surveyType)
            .apply { findNavController().navigateSafely(this) }
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
