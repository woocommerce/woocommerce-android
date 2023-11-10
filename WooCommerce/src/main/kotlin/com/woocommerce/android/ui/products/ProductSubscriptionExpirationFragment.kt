package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductSubscriptions
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductSubscriptionExpirationFragment : BaseProductFragment() {

    private val navArgs: ProductSubscriptionExpirationFragmentArgs by navArgs()

    override fun getFragmentTitle() = getString(R.string.product_subscription_expiration_title)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val subscription = navArgs.subscription
            setContent {
                WooThemeWithBackground {
                    Text("Edit expiration here")
                    Text("Expire after: ${subscription.length}")
                }
            }
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductSubscriptions)
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> onRequestAllowBackPress()
            }
        }
    }
}
