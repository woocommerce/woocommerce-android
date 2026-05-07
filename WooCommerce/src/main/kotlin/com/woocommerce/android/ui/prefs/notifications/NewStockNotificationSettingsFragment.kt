package com.woocommerce.android.ui.prefs.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewLauncher
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NewStockNotificationSettingsFragment : BaseFragment() {
    private val viewModel: NewStockNotificationSettingsViewModel by viewModels()

    @Inject
    lateinit var authenticatedWebViewLauncher: AuthenticatedWebViewLauncher

    override fun getFragmentTitle() = getString(R.string.settings_notifs_stock)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            NewStockNotificationSettingsScreen(viewModel = viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView ->
                    authenticatedWebViewLauncher.showAuthenticatedWebView(event)
            }
        }
    }
}
