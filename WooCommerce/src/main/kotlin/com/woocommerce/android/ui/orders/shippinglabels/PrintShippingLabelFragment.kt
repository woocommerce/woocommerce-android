package com.woocommerce.android.ui.orders.shippinglabels

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentPrintShippingLabelBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import java.io.File
import javax.inject.Inject

class PrintShippingLabelFragment : BaseFragment(R.layout.fragment_print_shipping_label) {
    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: PrintShippingLabelViewModel by viewModels { viewModelFactory }

    private var progressDialog: CustomProgressDialog? = null

    private var _binding: FragmentPrintShippingLabelBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_reprint)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPrintShippingLabelBinding.bind(view)

        setupObservers(viewModel)
        setupResultHandlers(viewModel)

        binding.shippingLabelPrintPaperSize.setClickListener { viewModel.onPaperSizeOptionsSelected() }
        binding.shippingLabelPrintBtn.setOnClickListener { viewModel.onPrintShippingLabelClicked() }
        binding.shippingLabelPrintInfoView.setOnClickListener { viewModel.onPrintShippingLabelInfoSelected() }
        binding.shippingLabelPrintPageOptionsView.setOnClickListener { viewModel.onViewLabelFormatOptionsClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers(viewModel: PrintShippingLabelViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.paperSize.takeIfNotEqualTo(old?.paperSize) {
                binding.shippingLabelPrintPaperSize.setText(getString(it.stringResource))
            }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.previewShippingLabel?.takeIfNotEqualTo(old?.previewShippingLabel) {
                writeShippingLabelToFile(it)
            }
            new.tempFile?.takeIfNotEqualTo(old?.tempFile) { openShippingLabelPreview(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> displayError(event.message)
                is OrderNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
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
        val context = requireContext()
        val pdfUri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )

        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.setDataAndType(pdfUri, "application/pdf")
        sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(sendIntent)

        viewModel.onPreviewLabelCompleted()
    }
}
