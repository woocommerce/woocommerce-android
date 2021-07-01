package com.woocommerce.android.ui.orders.cardreader

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentReceiptPreviewBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.SendReceipt
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.PrintHtmlHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReceiptPreviewFragment : BaseFragment(R.layout.fragment_receipt_preview) {
    val viewModel: ReceiptPreviewViewModel by viewModels()

    @Inject lateinit var printHtmlHelper: PrintHtmlHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val binding = FragmentReceiptPreviewBinding.bind(view)
        initObsevers(binding)
    }

    private fun initObsevers(binding: FragmentReceiptPreviewBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) {
            binding.previewWebview.loadUrl(it)
        }
        viewModel.event.observe(viewLifecycleOwner) {
            when(it) {
                is PrintReceipt -> printHtmlHelper.printReceipt(requireActivity(), it.receiptUrl, it.documentName)
                is SendReceipt -> composeEmail(it)
                else -> it.isHandled = false
            }
        }
    }

    private fun composeEmail(event: SendReceipt) {
        val success = ActivityUtils.composeEmail(requireActivity(), event.address, event.subject, event.content)
        if(!success) viewModel.onEmailActivityNotFound()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_receipt_preview, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_print -> {
                viewModel.onPrintClicked()
                true
            }
            R.id.menu_send -> {
                viewModel.onSendEmailClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        printHtmlHelper.getAndClearPrintJobResult()?.let {
            viewModel.onPrintResult(it)
        }
    }
}
