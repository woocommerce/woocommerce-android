package com.woocommerce.android.ui.products.details.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.findNavController
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductDetailWebViewFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: ProductDetailWebViewViewModel by viewModels()
    private val isInDetailPane: Boolean
        get() = requireContext().isTwoPanesShouldBeUsed && parentFragment?.id == R.id.detail_nav_container

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            ProductDetailWebViewScreen(
                viewModel = viewModel,
                showNavigationIcon = shouldShowNavigationIcon()
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> navigateBack()
            }
        }
    }

    private fun navigateBack() {
        if (isInDetailPane) {
            // If we are in the detail pane of a two-pane layout, we need to handle the back navigation
            // in the Activity's main nav controller and not the detail pane's nav controller.
            findNavController(R.id.nav_host_fragment_main).navigateUp()
        } else {
            findNavController().popBackStack(R.id.productDetailFragment, true)
        }
    }

    private fun shouldShowNavigationIcon(): Boolean = !isInDetailPane
}
