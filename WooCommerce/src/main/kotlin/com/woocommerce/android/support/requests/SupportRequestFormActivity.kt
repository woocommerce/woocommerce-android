package com.woocommerce.android.support.requests

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivitySupportRequestFormBinding
import com.woocommerce.android.extensions.serializable
import com.woocommerce.android.support.HelpOption
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationFailed
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationSucceeded
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SupportRequestFormActivity : AppCompatActivity() {
    private val viewModel: SupportRequestFormViewModel by viewModels()

    private val helpOrigin by lazy {
        intent.extras?.serializable(ORIGIN_KEY) ?: HelpOrigin.UNKNOWN
    }

    private var progressDialog: CustomProgressDialog? = null

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
                binding.mobileAppOption.id -> viewModel.onHelpOptionSelected(HelpOption.MobileApp)
                binding.ippOption.id -> viewModel.onHelpOptionSelected(HelpOption.InPersonPayments)
                binding.paymentsOption.id -> viewModel.onHelpOptionSelected(HelpOption.Payments)
                binding.wooPluginOption.id -> viewModel.onHelpOptionSelected(HelpOption.WooPlugin)
                binding.otherOption.id -> viewModel.onHelpOptionSelected(HelpOption.OtherPlugins)
            }
        }
        binding.submitRequestButton.setOnClickListener {
            viewModel.onSubmitRequestButtonClicked(this, helpOrigin)
        }
    }

    private fun observeViewModelEvents(binding: ActivitySupportRequestFormBinding) {
        viewModel.isSubmitButtonEnabled.observe(this) { isEnabled ->
            binding.submitRequestButton.isEnabled = isEnabled
        }
        viewModel.isRequestLoading.observe(this) { isLoading ->
            if (isLoading) showProgressDialog() else hideProgressDialog()
        }
        viewModel.event.observe(this) {
            when (it) {
                is RequestCreationSucceeded -> showRequestCreationSuccessDialog()
                is RequestCreationFailed -> showRequestCreationFailureDialog()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showRequestCreationSuccessDialog() {
        WooDialog.showDialog(
            activity = this,
            titleId = R.string.support_request_success_title,
            messageId = R.string.support_request_success_message,
            positiveButtonId = R.string.support_request_dialog_action
        )
    }

    private fun showRequestCreationFailureDialog() {
        WooDialog.showDialog(
            activity = this,
            titleId = R.string.support_request_error_title,
            messageId = R.string.support_request_error_message,
            positiveButtonId = R.string.support_request_dialog_action
        )
    }

    private fun showProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            "Sending your request",
            "Please wait..."
        ).also { it.show(supportFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
    }
}
