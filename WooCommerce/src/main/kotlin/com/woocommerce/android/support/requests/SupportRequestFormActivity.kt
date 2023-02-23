package com.woocommerce.android.support.requests

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import com.woocommerce.android.databinding.ActivitySupportRequestFormBinding
import com.woocommerce.android.extensions.serializable
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.InPersonPayments
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.MobileApp
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.OtherPlugins
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.Payments
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.WooPlugin
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SupportRequestFormActivity : AppCompatActivity() {
    private val viewModel: SupportRequestFormViewModel by viewModels()

    private val helpOrigin by lazy {
        intent.extras?.serializable(ORIGIN_KEY) ?: HelpOrigin.UNKNOWN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivitySupportRequestFormBinding.inflate(layoutInflater).apply {
            setContentView(root)
            setupActionBar()
            observeViewEvents(this)
            observeViewModelEvents(this)
        }
    }

    private fun ActivitySupportRequestFormBinding.setupActionBar() {
        setSupportActionBar(toolbar.toolbar as Toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewEvents(binding: ActivitySupportRequestFormBinding) {
        binding.requestSubject.setOnTextChangedListener { viewModel.onSubjectChanged(it.toString()) }
        binding.requestMessage.doOnTextChanged { text, _, _, _ -> viewModel.onMessageChanged(text.toString()) }
        binding.helpOptionsGroup.setOnCheckedChangeListener { _, selectionID ->
            when (selectionID) {
                binding.mobileAppOption.id -> viewModel.onHelpOptionSelected(MobileApp)
                binding.ippOption.id -> viewModel.onHelpOptionSelected(InPersonPayments)
                binding.paymentsOption.id -> viewModel.onHelpOptionSelected(Payments)
                binding.wooPluginOption.id -> viewModel.onHelpOptionSelected(WooPlugin)
                binding.otherOption.id -> viewModel.onHelpOptionSelected(OtherPlugins)
            }
        }
        binding.submitRequestButton.setOnClickListener {
            viewModel.onSubmitRequestButtonClicked(this)
        }
    }

    private fun observeViewModelEvents(binding: ActivitySupportRequestFormBinding) {
        viewModel.isSubmitButtonEnabled.observe(this) { isEnabled ->
            binding.submitRequestButton.isEnabled = isEnabled
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
    }
}
