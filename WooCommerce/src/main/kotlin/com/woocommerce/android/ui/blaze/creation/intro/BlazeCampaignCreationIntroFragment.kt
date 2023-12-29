package com.woocommerce.android.ui.blaze.creation.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationIntroFragment : BaseFragment() {
    private val navArgs by navArgs<BlazeCampaignCreationIntroFragmentArgs>()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            WooThemeWithBackground {
                BlazeCampaignCreationIntroScreen(
                    onContinueClick = {  },
                    onDismissClick = {  }
                )
            }
        }
    }
}
