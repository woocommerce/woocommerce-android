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
import com.woocommerce.android.extensions.handleDialogNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigatePackageSelection
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToCustomsFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToFedExTermsOfService
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToHazmatFormEdit
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.NavigateToUPSDAPTermsOfService
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditAddressFlow
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressFragment.Companion.DESTINATION_ADDRESS_UPDATE_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormFragment.Companion.CUSTOMS_DATA_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.fedex.FedExTermsOfServiceBottomSheetFragment
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.Companion.HAZMAT_CATEGORY_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationFragment.Companion.PACKAGE_SELECTION_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.refund.WooShippingLabelRefundFragment
import com.woocommerce.android.ui.orders.wooshippinglabels.split.WooShippingSplitShipmentFragment
import com.woocommerce.android.ui.orders.wooshippinglabels.upsdap.UPSDAPTermsOfServiceBottomSheetFragment
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class WooShippingLabelCreationFragment : BaseFragment() {
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
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelPackageCreationFragment(
                            storeOptions = event.storeOptions
                        )
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
                            originCountryCode = event.originCountryCode,
                            destinationCountryCode = event.destinationCountryCode,
                            customsData = event.customData,
                            storeOptions = event.storeOptions
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

                is NavigateToHazmatFormEdit -> {
                    WooShippingLabelCreationFragmentDirections
                        .actionWooShippingLabelCreationFragmentToWooShippingLabelHazmatFormFragment(
                            event.selectedCategory?.name
                        ).let { findNavController().navigateSafely(it) }
                }

                is WooShippingLabelCreationViewModel.OpenShippingLabelFile -> openShippingLabelPreview(event.file)
                is WooShippingLabelCreationViewModel.OpenLearnMoreScreen -> openLearnMoreView()
                is WooShippingLabelCreationViewModel.NavigateToRefundRequest -> navigateToRefundRequest(
                    event.orderId,
                    event.labelId
                )

                is WooShippingLabelCreationViewModel.NavigateToPaymentMethodEdit -> navigateToPaymentMethodEdit()

                is WooShippingLabelCreationViewModel.OpenUrl -> openUrl(event.url)
                is WooShippingLabelCreationViewModel.ShowError -> showErrorDialog(event.errorResId)
                is NavigateToUPSDAPTermsOfService -> navigateToUPSDAPTermsOfService(event.originAddress)
                is NavigateToFedExTermsOfService -> navigateToFedExTermsOfService()

                is WooShippingLabelCreationViewModel.PrintCustomsForm -> printFile(event.file)
            }
        }
    }

    /**
     * This just opens the default PDF reader of the device
     */
    private fun printFile(file: File) {
        ActivityUtils.previewPDFFile(requireActivity(), file)
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

        handleResult<Long>(WooShippingLabelRefundFragment.KEY_REFUND_SHIPPING_LABEL_RESULT) {
            viewModel.onShippingLabelRefunded(it)
        }

        handleDialogNotice(
            FedExTermsOfServiceBottomSheetFragment.TOS_ACCEPTED_NOTICE_KEY,
            entryId = R.id.wooShippingLabelCreationFragment
        ) {
            viewModel.onCarrierTermsAccepted(WooShippingLabelCreationViewModel.Carrier.FEDEX)
        }
        handleDialogNotice(
            UPSDAPTermsOfServiceBottomSheetFragment.TOS_ACCEPTED_NOTICE_KEY,
            entryId = R.id.wooShippingLabelCreationFragment
        ) {
            viewModel.onCarrierTermsAccepted(WooShippingLabelCreationViewModel.Carrier.UPS)
        }
    }

    private fun openShippingLabelPreview(file: File) {
        ActivityUtils.previewPDFFile(requireActivity(), file)
    }

    private fun openLearnMoreView() {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToPrintShippingLabelInfoFragment()
        )
    }

    private fun navigateToRefundRequest(orderId: Long, labelId: Long) {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToWooShippingLabelRefundRequestFragment(orderId, labelId)
        )
    }

    private fun navigateToPaymentMethodEdit() {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToWooShippingEditPaymentDialogFragment()
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

    private fun navigateToUPSDAPTermsOfService(originAddress: OriginShippingAddress) {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToUpsdapTermsOfServiceBottomSheetFragment(originAddress)
        )
    }

    private fun navigateToFedExTermsOfService() {
        findNavController().navigate(
            WooShippingLabelCreationFragmentDirections
                .actionWooShippingLabelCreationFragmentToFedExTermsOfServiceBottomSheetFragment()
        )
    }
}
