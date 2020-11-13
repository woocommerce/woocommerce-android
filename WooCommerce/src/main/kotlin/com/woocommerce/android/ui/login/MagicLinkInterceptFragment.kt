package com.woocommerce.android.ui.login

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

class MagicLinkInterceptFragment : Fragment() {
    companion object {
        private const val REQUEST_CODE_ADD_ACCOUNT = 100

        const val TAG = "MagicLinkInterceptFragment"
        private const val ARG_AUTH_TOKEN = "ARG_AUTH_TOKEN"

        fun newInstance(authToken: String): MagicLinkInterceptFragment {
            val fragment = MagicLinkInterceptFragment()
            val args = Bundle()
            args.putString(ARG_AUTH_TOKEN, authToken)
            fragment.arguments = args
            return fragment
        }
    }

    private var authToken: String? = null
    private var progressDialog: ProgressDialog? = null

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: MagicLinkInterceptViewModel by viewModels { viewModelFactory }

    private var retryButton: Button? = null
    private var retryContainer: ScrollView? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            authToken = it.getString(ARG_AUTH_TOKEN, null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.login_magic_link_sent_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        retryButton = view.findViewById(R.id.login_open_email_client)
        retryContainer = view.findViewById(R.id.login_magic_link_container)
        retryButton?.text = getString(R.string.retry)
        showRetryScreen(false)
        retryButton?.setOnClickListener {
            AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_INTERCEPT_RETRY_TAPPED)
            viewModel.fetchAccountInfo()
        }

        view.findViewById<TextView>(R.id.login_enter_password).visibility = View.GONE

        initializeViewModel()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_INTERCEPT_SCREEN_VIEWED)
    }

    private fun initializeViewModel() {
        setupObservers()
        authToken?.let { viewModel.updateMagicLinkAuthToken(it) }
    }

    // BaseTransientBottomBar.LENGTH_LONG is pointing to Snackabr.LENGTH_LONG which confuses checkstyle
    @Suppress("WrongConstant")
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            showProgressDialog(it)
        })

        viewModel.isAuthTokenUpdated.observe(viewLifecycleOwner, Observer { authTokenUpdated ->
            if (authTokenUpdated) {
                showSitePickerScreen()
            } else showLoginScreen()
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, Observer { messageId ->
            view?.let {
                Snackbar.make(it, getString(messageId), BaseTransientBottomBar.LENGTH_LONG).show()
            }
        })

        viewModel.showRetryOption.observe(viewLifecycleOwner, Observer {
            showRetryScreen(it)
        })
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = ProgressDialog.show(
                    activity, "", getString(R.string.login_magic_link_token_updating), true
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

    private fun showSitePickerScreen() {
        context?.let {
            SitePickerActivity.showSitePickerFromLogin(it)
            activity?.finish()
        }
    }

    private fun showLoginScreen() {
        val intent = Intent(context, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivityForResult(intent, REQUEST_CODE_ADD_ACCOUNT)
        activity?.finish()
    }

    private fun showRetryScreen(show: Boolean) {
        retryButton?.isVisible = show
        retryContainer?.isVisible = show
    }
}
