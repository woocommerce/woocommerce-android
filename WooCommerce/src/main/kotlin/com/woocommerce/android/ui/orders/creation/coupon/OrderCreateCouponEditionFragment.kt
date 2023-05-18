package com.woocommerce.android.ui.orders.creation.coupon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.coupon.OrderCreateCouponEditionViewModel.UpdateCouponCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderCreateCouponEditionFragment : BaseFragment() {
    private var doneMenuItem: MenuItem? = null
    private val menuProvider: MenuProvider by lazy {
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                menu.clear()
                inflater.inflate(R.menu.menu_done, menu)
                doneMenuItem = menu.findItem(R.id.menu_done)
                doneMenuItem?.isEnabled = viewModel.viewState.value?.isDoneButtonEnabled ?: false
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.menu_done -> {
                        lifecycleScope.launch {
                            viewModel.onDoneClicked()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }
    private val viewModel by viewModels<OrderCreateCouponEditionViewModel>()
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)

        return ComposeView(requireContext()).apply {
            id = R.id.more_menu_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state = viewModel.viewState.observeAsState()
                WooThemeWithBackground {
                    OrderCreateCouponEditionScreen(
                        state = state,
                        onCouponCodeChanged = viewModel::onCouponCodeChanged,
                        onCouponRemoved = viewModel::onCouponRemoved
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is UpdateCouponCode -> {
                    sharedViewModel.onCouponEntered(it.couponCode)
                    findNavController().navigateUp()
                }
            }
        }
        viewModel.viewState.observe(viewLifecycleOwner) {
            doneMenuItem?.isEnabled = it.isDoneButtonEnabled
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_coupon)
}
