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
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigatePackageSelection
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToCustomsFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.StartHazmatFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditAddressFlow
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressFragment.Companion.DESTINATION_ADDRESS_UPDATE_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormFragment.Companion.CUSTOMS_DATA_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.Companion.HAZMAT_CATEGORY_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationFragment.Companion.PACKAGE_SELECTION_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.split.WooShippingSplitShipmentFragment
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigatePackageSelection ->
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelPackageCreationFragment()
                        .let { findNavController().navigateSafely(it) }

                is WooShippingLabelCreationViewModel.NavigateToOriginAddressEdit ->
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingEditOriginAddressFragment(
                            flow = EditAddressFlow.EditOriginAddress(event.originAddress)
                        ).let { findNavController().navigateSafely(it) }

                is WooShippingLabelCreationViewModel.NavigateToDestinationAddressEdit ->
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingEditOriginAddressFragment(
                            flow = EditAddressFlow.EditDestinationAddress(
                                address = event.destinationAddress,
                                orderId = event.orderId
                            )
                        ).let { findNavController().navigateSafely(it) }

                is NavigateToCustomsFormEdit -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelCustomsFormFragment(
                            shippableItems = event.shippableItems.toTypedArray(),
                            destinationCountryCode = event.destinationCountryCode,
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
                is WooShippingLabelCreationViewModel.NavigateToSplitShipment -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingSplitShipmentFragment(
                            shipmentArgs = event.shipmentArgs
                        ).let {
                            findNavController().navigateSafely(it)
                        }
                }

                is StartHazmatFormEdit -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelHazmatFormFragment(
                            event.selectedCategory?.name
                        ).let { findNavController().navigateSafely(it) }
                }

                is WooShippingLabelCreationViewModel.OpenShippingLabelFile -> openShippingLabelPreview(event.file)
                is WooShippingLabelCreationViewModel.OpenLearnMoreScreen -> openLearnMoreView()
                is WooShippingLabelCreationViewModel.StartRefundRequest -> startRefundRequest(
                    event.orderId,
                    event.shipment
                )

                is WooShippingLabelCreationViewModel.OpenUrl -> openUrl(event.url)
                is WooShippingLabelCreationViewModel.ShowError -> showErrorDialog(event.errorResId)
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

        handleResult<String>(HAZMAT_CATEGORY_RESULT) {
            val selectedCategory = runCatching { ShippingLabelHazmatCategory.valueOf(it) }.getOrNull()
            viewModel.onHazmatCategorySelected(selectedCategory)
        }
        handleResult<List<ShipmentUIModel>>(WooShippingSplitShipmentFragment.SPLIT_SHIPMENT_RESULT) {
            viewModel.onShipmentSplit(it)
        }
    }

    override fun onRequestAllowBackPress(): Boolean = viewModel.allowBackNavigation()

    private fun openShippingLabelPreview(file: File) {
        ActivityUtils.previewPDFFile(requireActivity(), file)
    }

    private fun openLearnMoreView() {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToPrintShippingLabelInfoFragment()
        )
    }

    private fun startRefundRequest(orderId: Long, shipment: ShipmentUIModel) {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToWooShippingLabelRefundRequestFragment(orderId, shipment)
        )
    }

    private fun openUrl(url: String) {
        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }

    private fun showErrorDialog(messageResId: Int) {
        WooDialog.showDialog(
            activity = requireActivity(),
            titleId = R.string.error_generic,
            messageId = messageResId,
            positiveButtonId = R.string.dialog_ok
        )
    }
}
