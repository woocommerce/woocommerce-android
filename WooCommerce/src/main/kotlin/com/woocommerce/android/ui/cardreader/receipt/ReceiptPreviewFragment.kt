package com.woocommerce.android.ui.cardreader.receipt

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentReceiptPreviewBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.cardreader.receipt.ReceiptPreviewViewModel.ReceiptPreviewEvent.LoadUrl
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.PrintHtmlHelper
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReceiptPreviewFragment : BaseFragment(R.layout.fragment_receipt_preview) {
    val viewModel: ReceiptPreviewViewModel by viewModels()

    @Inject lateinit var printHtmlHelper: PrintHtmlHelper
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var _binding: FragmentReceiptPreviewBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReceiptPreviewBinding.bind(view)
        initViews(binding, savedInstanceState)
        initObservers(binding)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.receiptPreviewPreviewWebview.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews(binding: FragmentReceiptPreviewBinding, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            binding.receiptPreviewPreviewWebview.restoreState(savedInstanceState)
        } else {
            with(binding.receiptPreviewPreviewWebview) {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        viewModel.onReceiptLoaded()
                    }
                }
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }
        }
    }

    private fun initObservers(binding: FragmentReceiptPreviewBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) {
            UiHelpers.updateVisibility(binding.receiptPreviewPreviewWebview, it.isContentVisible)
            UiHelpers.updateVisibility(binding.receiptPreviewProgressBar, it.isProgressVisible)
        }
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is LoadUrl -> binding.receiptPreviewPreviewWebview.loadUrl(it.url)
                is PrintReceipt -> printHtmlHelper.printReceipt(requireActivity(), it.receiptUrl, it.documentName)
                is SendReceipt -> composeEmail(it)
                is ShowSnackbar -> uiMessageResolver.showSnack(it.message)
                else -> it.isHandled = false
            }
        }
    }

    private fun composeEmail(event: SendReceipt) {
        val success = ActivityUtils.composeEmail(requireActivity(), event.address, event.subject, event.content)
        if (!success) viewModel.onEmailActivityNotFound()
    }
}
