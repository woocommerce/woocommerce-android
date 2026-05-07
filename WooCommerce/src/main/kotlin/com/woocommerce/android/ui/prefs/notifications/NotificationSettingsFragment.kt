package com.woocommerce.android.ui.prefs.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSettingsFragment : BaseFragment() {
    private val viewModel: NotificationSettingsViewModel by viewModels()
    private val navArgs: NotificationSettingsFragmentArgs by navArgs()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun getFragmentTitle() = getString(R.string.settings_push_notifications)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            NotificationSettingsScreen(
                viewModel = viewModel,
                showSmarterNotifications = navArgs.showSmarterNotifications
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNotificationSettings()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NotificationSettingsViewModel.OpenDeviceNotificationSettings -> openDeviceNotificationSettings()
                is NotificationSettingsViewModel.OpenNewOrderNotificationSettings -> openNewOrderNotificationSettings()
                is MultiLiveEvent.Event.ShowActionStringSnackbar -> uiMessageResolver.showActionSnack(
                    event.message,
                    event.actionText,
                    event.action
                )
            }
        }
    }

    private fun openDeviceNotificationSettings() {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", requireActivity().packageName)
        }
        requireActivity().startActivity(intent)
    }

    private fun openNewOrderNotificationSettings() {
        findNavController().navigateSafely(
            NotificationSettingsFragmentDirections
                .actionNotificationSettingsFragmentToNewOrderNotificationSettingsFragment()
        )
    }
}
