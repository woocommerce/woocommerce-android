package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentPrintLabelCustomsFormBinding
import com.woocommerce.android.databinding.PrintCustomsFormListItemBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.PrintCustomsFormAdapter.PrintCustomsFormViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelCustomsFormViewModel.PrintCustomsForm
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
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

    private val invoicesAdapter by lazy { PrintCustomsFormAdapter(viewModel::onInvoicePrintButtonClicked) }

    override fun getFragmentTitle(): String = getString(R.string.shipping_label_print_customs_form_screen_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.storageDirectory = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: requireContext().filesDir
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPrintLabelCustomsFormBinding.bind(view)
        setupObservers(binding)
        setupView(binding)
    }

    private fun setupObservers(binding: FragmentPrintLabelCustomsFormBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) {
                showProgressDialog(it)
            }
            new.showInvoicesAsAList.takeIfNotEqualTo(old?.showInvoicesAsAList) {
                binding.invoicesList.isVisible = it
                binding.printButton.isVisible = !it
            }
            new.commercialInvoices.takeIfNotEqualTo(old?.commercialInvoices) {
                invoicesAdapter.submitList(it)
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
        binding.invoicesList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = invoicesAdapter
        }
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

class PrintCustomsFormAdapter(
    private val onPrintClicked: (String) -> Unit
) : ListAdapter<String, PrintCustomsFormViewHolder>(PrintCustomsFormDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrintCustomsFormViewHolder {
        return PrintCustomsFormViewHolder(
            PrintCustomsFormListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PrintCustomsFormViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PrintCustomsFormViewHolder(val binding: PrintCustomsFormListItemBinding) : ViewHolder(binding.root) {
        fun bind(position: Int) {
            val context = binding.root.context
            binding.packageTitle.text = context.getString(
                R.string.shipping_label_package_details_title_template,
                position + 1
            )
            binding.printButton.setOnClickListener {
                onPrintClicked(getItem(position))
            }
        }
    }

    object PrintCustomsFormDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean = areItemsTheSame(oldItem, newItem)
    }
}
