package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.shipping.OrderShippingMethodsFragment.Companion.SELECTED_METHOD_RESULT
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderShippingFragment : BaseFragment() {
    val viewModel: OrderShippingViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Surface {
                        UpdateShippingScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_shipping_title_add)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is UpdateShipping -> navigateBackWithResult(UPDATE_SHIPPING_RESULT, event.shippingUpdate)
                is RemoveShipping -> navigateBackWithResult(REMOVE_SHIPPING_RESULT, event.id)
                is SelectShippingMethod -> {
                    val action = OrderShippingFragmentDirections
                        .actionOrderShippingFragmentToOrderShippingMethodsFragment(event.currentMethodId)
                    findNavController().navigate(action)
                }
            }
        }
        handleResult<ShippingMethod>(SELECTED_METHOD_RESULT) { selected ->
            viewModel.onMethodSelected(selected)
        }
    }

    companion object {
        const val UPDATE_SHIPPING_RESULT = "update_shipping-result"
        const val REMOVE_SHIPPING_RESULT = "remove_shipping-result"
    }
}
