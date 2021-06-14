package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentPrintLabelCustomsFormBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelCustomsFormViewModel.PrintCustomsForm
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PrintShippingLabelCustomsFormFragment :
    BaseFragment(R.layout.fragment_print_label_customs_form), BackPressListener {
    private val viewModel: PrintShippingLabelCustomsFormViewModel by viewModels()
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var progressDialog: CustomProgressDialog? = null
    private val navArgs: PrintShippingLabelCustomsFormFragmentArgs by navArgs()

    override fun getFragmentTitle(): String = getString(R.string.shipping_label_print_customs_form_screen_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.storageDirectory = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: requireContext().filesDir
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPrintLabelCustomsFormBinding.bind(view)
        setupObservers()
        setupView(binding)
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) {
                showProgressDialog(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                is ExitWithResult<*> ->
                    navigateBackWithNotice(PrintShippingLabelFragment.KEY_LABEL_PURCHASED, R.id.orderDetailFragment)
                is PrintCustomsForm -> printFile(event.file)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        }
    }

    /**
     * This just opens the default PDF reader of the device
     */
    private fun printFile(file: File) {
        ActivityUtils.previewPDFFile(requireActivity(), file)
    }

    private fun setupView(binding: FragmentPrintLabelCustomsFormBinding) {
        binding.saveForLaterButton.isVisible = !navArgs.isReprint
        binding.printButton.setOnClickListener {
            viewModel.onPrintButtonClicked()
        }
        binding.saveForLaterButton.setOnClickListener {
            viewModel.onSaveForLaterClicked()
        }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            progressDialog?.dismiss()
            progressDialog = CustomProgressDialog.show(
                title = getString(R.string.web_view_loading_title),
                message = getString(R.string.web_view_loading_message),
                onCancelListener = { viewModel.onDownloadCanceled() }
            ).also {
                it.show(parentFragmentManager, CustomProgressDialog.TAG)
            }
        } else {
            progressDialog?.dismiss()
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }
}
