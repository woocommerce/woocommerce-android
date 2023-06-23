package com.woocommerce.android.ui.prefs.privacy.banner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.MainActivityViewModel
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacyBannerFragment : WCBottomSheetDialogFragment() {

    private val viewModel: PrivacyBannerViewModel by viewModels()
    private val mainViewModel: MainActivityViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WooThemeWithBackground {
                    PrivacyBannerScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PrivacyBannerViewModel.ShowError -> {
                    mainViewModel.onPrivacyPreferenceUpdateFailed(event.requestedAnalyticsValue)
                    dismiss()
                }

                is PrivacyBannerViewModel.Dismiss -> {
                    dismiss()
                }

                is PrivacyBannerViewModel.ShowSettings -> {
                    mainViewModel.onPrivacySettingsTapped()
                    dismiss()
                }

                is PrivacyBannerViewModel.ShowErrorOnSettings -> {
                    mainViewModel.onSettingsPrivacyPreferenceUpdateFailed(event.requestedAnalyticsValue)
                    dismiss()
                }
            }
        }
    }
}
