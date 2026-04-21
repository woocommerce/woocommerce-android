@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.doOnApplyWindowInsets
import com.woocommerce.android.ui.login.MagicLinkInterceptViewModel.CancelJetpackActivation
import com.woocommerce.android.ui.login.MagicLinkInterceptViewModel.ContinueJetpackActivation
import com.woocommerce.android.ui.login.MagicLinkInterceptViewModel.OpenLogin
import com.woocommerce.android.ui.login.MagicLinkInterceptViewModel.OpenSitePicker
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherFragmentArgs
import com.woocommerce.android.ui.login.wpcom.WPComLoginMagicLinkHandlerFragmentArgs
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

@AndroidEntryPoint
class MagicLinkInterceptActivity : AppCompatActivity() {
    companion object {
        private const val TOKEN_PARAMETER = "token"
        private const val FLOW_PARAMETER = "flow"
    }

    @Inject
    lateinit var loginAnalyticsListener: LoginAnalyticsListener

    private var progressDialog: ProgressDialog? = null

    private val viewModel: MagicLinkInterceptViewModel by viewModels()

    private var retryButton: Button? = null
    private var retryContainer: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginAnalyticsListener.trackLoginMagicLinkOpened()

        setContentView(org.wordpress.android.login.R.layout.login_magic_link_sent_screen)

        val rootLayout = findViewById<View>(org.wordpress.android.login.R.id.login_magic_link_root_layout)
        rootLayout.doOnApplyWindowInsets {
            rootLayout.updatePadding(
                left = it.left,
                top = it.top,
                right = it.right,
                bottom = it.bottom
            )
        }

        retryButton = findViewById(R.id.login_open_email_client)
        retryContainer = findViewById(R.id.login_magic_link_container)
        retryButton?.text = getString(R.string.retry)
        showRetryScreen(false)
        retryButton?.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_INTERCEPT_RETRY_TAPPED)
            viewModel.fetchAccountInfo()
        }

        findViewById<TextView>(R.id.login_magic_link_fallback_button).visibility = View.GONE

        initializeViewModel()
    }

    private fun initializeViewModel() {
        setupObservers()

        val uri = requireNotNull(intent.data)
        val authToken = uri.getQueryParameter(TOKEN_PARAMETER)
        val flow = uri.getQueryParameter(FLOW_PARAMETER)?.let {
            MagicLinkFlow.fromString(it)
        }

        authToken?.let { viewModel.handleMagicLink(authToken = it, flow = flow) }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) {
            showProgressDialog(it)
        }

        viewModel.event.observe(this) { event ->
            when (event) {
                OpenSitePicker, CancelJetpackActivation -> openMainActivity()
                OpenLogin -> showLoginScreen()
                is ContinueJetpackActivation -> continueJetpackActivation(event)
                is ShowSnackbar -> showSnackBar(event.message)
            }
        }

        viewModel.showRetryOption.observe(this) {
            showRetryScreen(it)
        }
    }

    private fun showSnackBar(@StringRes messageId: Int) {
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(messageId),
            BaseTransientBottomBar.LENGTH_LONG
        ).show()
    }

    @Suppress("DEPRECATION")
    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = ProgressDialog.show(
                this, "", getString(R.string.login_magic_link_token_updating), true
            )
            progressDialog?.setCancelable(false)
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.apply {
            if (isShowing) {
                cancel()
                progressDialog = null
            }
        }
    }

    private fun showRetryScreen(show: Boolean) {
        retryButton?.isVisible = show
        retryContainer?.isVisible = show
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun continueJetpackActivation(event: ContinueJetpackActivation) {
        NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph_main)
            .addDestination(
                R.id.jetpackActivationDispatcherFragment,
                JetpackActivationDispatcherFragmentArgs(
                    jetpackStatus = event.jetpackStatus,
                    siteUrl = event.siteUrl
                ).toBundle()
            )
            .addDestination(
                R.id.wPComLoginMagicLinkHandlerFragment,
                WPComLoginMagicLinkHandlerFragmentArgs(
                    jetpackStatus = event.jetpackStatus
                ).toBundle()
            )
            .createPendingIntent()
            .send()

        finish()
    }

}
