package com.woocommerce.android.ui.orders.creation.giftcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult

class OrderCreateEditGiftCardFragment : BaseFragment() {
    val viewModel: OrderCreateEditGiftCardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    EditGiftCardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_gift_card)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is ExitWithResult<*> -> navigateBackWithResult(GIFT_CARD_RESULT, event.data)
            }
        }
    }

    companion object {
        const val GIFT_CARD_RESULT = "gift-card-result"
    }
}
