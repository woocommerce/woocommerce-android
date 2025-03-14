package com.woocommerce.android.ui.orders.wooshippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.StartCustomsFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.StartPackageSelection
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditAddressFlow
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressFragment.Companion.DESTINATION_ADDRESS_UPDATE_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormFragment.Companion.CUSTOMS_DATA_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationFragment.Companion.PACKAGE_SELECTION_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooShippingLabelCreationFragment : BaseFragment(), BackPressListener {
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: WooShippingLabelCreationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Surface {
                        WooShippingLabelCreationScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupResultHandlers()
    }

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    @Suppress("CyclomaticComplexMethod")
    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is StartPackageSelection ->
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelPackageCreationFragment()
                        .let { findNavController().navigateSafely(it) }

                is WooShippingLabelCreationViewModel.LabelPurchased -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelPurchasedFragment(
                            purchaseData = event.purchaseData
                        ).let { findNavController().navigateSafely(it) }
                }

                is WooShippingLabelCreationViewModel.StartOriginAddressEdit ->
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingEditOriginAddressFragment(
                            flow = EditAddressFlow.EditOriginAddress(event.originAddress)
                        ).let { findNavController().navigateSafely(it) }

                is WooShippingLabelCreationViewModel.StartDestinationAddressEdit ->
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingEditOriginAddressFragment(
                            flow = EditAddressFlow.EditDestinationAddress(
                                address = event.destinationAddress,
                                orderId = event.orderId
                            )
                        ).let { findNavController().navigateSafely(it) }

                is StartCustomsFormEdit -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelCustomsFormFragment(
                            shippableItems = event.shippableItems.toTypedArray(),
                            customsData = event.customData
                        ).let { findNavController().navigateSafely(it) }
                }
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MultiLiveEvent.Event.ShowActionSnackbar -> uiMessageResolver.showActionSnack(
                    event.message,
                    event.actionText,
                    event.action
                )
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is WooShippingLabelCreationViewModel.StartSplitShipment -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingSplitShipmentFragment().let {
                            findNavController().navigateSafely(it)
                        }
                }
            }
        }
    }

    private fun setupResultHandlers() {
        handleResult<PackageData>(PACKAGE_SELECTION_RESULT) {
            viewModel.onPackageSelected(it)
        }
        handleResult<DestinationShippingAddress>(DESTINATION_ADDRESS_UPDATE_RESULT) {
            viewModel.onUpdateDestinationAddress(it)
        }

        handleResult<CustomsData>(CUSTOMS_DATA_RESULT) {
            viewModel.onCustomsDataAvailable(it)
        }
    }

    override fun onRequestAllowBackPress(): Boolean = viewModel.allowBackNavigation()
}
