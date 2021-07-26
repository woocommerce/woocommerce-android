package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentPrintShippingLabelBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PrintShippingLabelFragment : BaseFragment(R.layout.fragment_print_shipping_label), BackPressListener {
    companion object {
        const val KEY_LABEL_PURCHASED = "key-label-purchased"
    }
    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: PrintShippingLabelViewModel by viewModels()
    private val navArgs: PrintShippingLabelFragmentArgs by navArgs()

    private var progressDialog: CustomProgressDialog? = null

    override fun getFragmentTitle(): String {
        return getString(viewModel.screenTitle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentPrintShippingLabelBinding.bind(view)

        initUi(binding)
        setupObservers(viewModel, binding)
        setupResultHandlers(viewModel)
    }

    private fun initUi(binding: FragmentPrintShippingLabelBinding) {
        binding.reprintGroup.isVisible = navArgs.isReprint
        binding.purchaseGroup.isVisible = !navArgs.isReprint

        binding.labelPurchased.setText(
            if (navArgs.shippingLabelIds.size > 1) R.string.shipping_label_print_multiple_purchase_success
            else R.string.shipping_label_print_purchase_success
        )

        binding.shippingLabelPrintBtn.setText(
            if (navArgs.shippingLabelIds.size > 1) R.string.shipping_label_print_multiple_button
            else R.string.shipping_label_print_button
        )

        binding.shippingLabelPrintPaperSize.setClickListener { viewModel.onPaperSizeOptionsSelected() }
        binding.shippingLabelPrintBtn.setOnClickListener { viewModel.onPrintShippingLabelClicked() }
        binding.shippingLabelPrintInfoView.setOnClickListener { viewModel.onPrintShippingLabelInfoSelected() }
        binding.shippingLabelPrintPageOptionsView.setOnClickListener { viewModel.onViewLabelFormatOptionsClicked() }
        binding.saveForLaterButton.setOnClickListener { viewModel.onSaveForLaterClicked() }
    }

    private fun setupObservers(viewModel: PrintShippingLabelViewModel, binding: FragmentPrintShippingLabelBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.paperSize.takeIfNotEqualTo(old?.paperSize) {
                binding.shippingLabelPrintPaperSize.setText(getString(it.stringResource))
            }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.previewShippingLabel?.takeIfNotEqualTo(old?.previewShippingLabel) {
                writeShippingLabelToFile(it)
            }
            new.isLabelExpired?.takeIfNotEqualTo(old?.isLabelExpired) { isExpired ->
                binding.expirationWarningBanner.isVisible = isExpired
                binding.shippingLabelPrintPaperSize.isEnabled = !isExpired
                binding.shippingLabelPrintBtn.isEnabled = !isExpired
            }
            new.tempFile?.takeIfNotEqualTo(old?.tempFile) { openShippingLabelPreview(it) }
        }

        viewModel.event.observe(
            viewLifecycleOwner,
            Observer { event ->
                when (event) {
                    is ShowSnackbar -> displayError(event.message)
                    is OrderNavigationTarget -> navigator.navigate(this, event)
                    is ExitWithResult<*> -> navigateBackAndNotifyOrderDetails()
                    else -> event.isHandled = false
                }
            }
        )
    }

    private fun setupResultHandlers(viewModel: PrintShippingLabelViewModel) {
        handleDialogResult<ShippingLabelPaperSize>(
            ShippingLabelPaperSizeSelectorDialog.KEY_PAPER_SIZE_RESULT,
            R.id.printShippingLabelFragment
        ) {
            viewModel.onPaperSizeSelected(it)
        }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.web_view_loading_title),
                getString(R.string.web_view_loading_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun displayError(@StringRes messageId: Int) {
        uiMessageResolver.showSnack(messageId)
    }

    private fun writeShippingLabelToFile(base: String) {
        requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let {
            viewModel.writeShippingLabelToFile(it, base)
        } ?: displayError(R.string.shipping_label_preview_error)
    }

    private fun openShippingLabelPreview(file: File) {
        ActivityUtils.previewPDFFile(requireActivity(), file)
        viewModel.onPreviewLabelCompleted()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return if (navArgs.isReprint) {
            true
        } else {
            navigateBackAndNotifyOrderDetails()
            false
        }
    }

    private fun navigateBackAndNotifyOrderDetails() {
        navigateBackWithNotice(KEY_LABEL_PURCHASED, R.id.orderDetailFragment)
    }
}
