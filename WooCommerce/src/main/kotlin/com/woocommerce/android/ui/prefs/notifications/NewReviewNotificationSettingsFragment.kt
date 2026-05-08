package com.woocommerce.android.ui.prefs.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewReviewNotificationSettingsFragment : BaseFragment() {
    private val viewModel: NewReviewNotificationSettingsViewModel by viewModels()

    override fun getFragmentTitle() = getString(R.string.settings_notifs_new_reviews)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            NewReviewNotificationSettingsScreen(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
