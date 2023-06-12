package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacySettingsPolicesFragment : BaseFragment() {

    override fun getFragmentTitle(): String {
        return resources.getString(R.string.settings_privacy_cookies_polices)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WooThemeWithBackground {
                    PrivacySettingsPolicesScreen(
                        onPrivacyPolicyClicked = {
                            AnalyticsTracker.track(AnalyticsEvent.PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED)
                            ChromeCustomTabUtils.launchUrl(
                                requireActivity(),
                                AppUrls.AUTOMATTIC_PRIVACY_POLICY
                            )
                        },
                        onCookiePolicyClicked = {
                            AnalyticsTracker.track(
                                AnalyticsEvent.PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED
                            )
                            ChromeCustomTabUtils.launchUrl(
                                requireActivity(),
                                AppUrls.AUTOMATTIC_COOKIE_POLICY
                            )
                        }
                    )
                }
            }
        }
    }
}
