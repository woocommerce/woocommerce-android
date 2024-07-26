package com.woocommerce.android.support.requests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivitySupportRequestFormBinding
import com.woocommerce.android.extensions.serializable
import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationFailed
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationSucceeded
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.ShowSupportIdentityInputDialog
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SupportRequestFormActivity : AppCompatActivity() {
    @Inject lateinit var supportHelper: SupportHelper
    @Inject lateinit var zendeskSettings: ZendeskSettings

    private val viewModel: SupportRequestFormViewModel by viewModels()

    private val helpOrigin by lazy {
        intent.extras?.serializable(ORIGIN_KEY) ?: HelpOrigin.UNKNOWN
    }

    private val extraTags by lazy {
        intent.extras?.getStringArrayList(EXTRA_TAGS_KEY) ?: emptyList()
    }

    private var progressDialog: CustomProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        zendeskSettings.setup(context = this)

        ActivitySupportRequestFormBinding.inflate(layoutInflater).apply {
            setContentView(root)
            setupActionBar()
            observeViewEvents(this)
            observeViewModelEvents(this)
        }
        viewModel.onViewCreated()
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
                binding.mobileAppOption.id -> viewModel.onHelpOptionSelected(TicketType.MobileApp)
                binding.ippOption.id -> viewModel.onHelpOptionSelected(TicketType.InPersonPayments)
                binding.paymentsOption.id -> viewModel.onHelpOptionSelected(TicketType.Payments)
                binding.wooPluginOption.id -> viewModel.onHelpOptionSelected(TicketType.WooPlugin)
                binding.otherOption.id -> viewModel.onHelpOptionSelected(TicketType.OtherPlugins)
            }
        }
        binding.submitRequestButton.setOnClickListener {
            viewModel.submitSupportRequest(
                context = this,
                helpOrigin = helpOrigin,
                extraTags = extraTags
            )
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
                is ShowSupportIdentityInputDialog -> showSupportIdentityInputDialog(it.emailSuggestion)
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
            positiveButtonId = R.string.support_request_dialog_action,
            posBtnAction = { _, _ -> finish() }
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
            getString(R.string.support_request_loading_title),
            getString(R.string.support_request_loading_message)
        ).also { it.show(supportFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showSupportIdentityInputDialog(emailSuggestion: String) {
        supportHelper.showSupportIdentityInputDialog(this, emailSuggestion) { email, name ->
            viewModel.onUserIdentitySet(
                context = this,
                helpOrigin = helpOrigin,
                extraTags = extraTags,
                selectedEmail = email,
                selectedName = name
            )
        }
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_IDENTITY_FORM_VIEWED)
    }

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
        private const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"

        @JvmStatic
        fun createIntent(
            context: Context,
            origin: HelpOrigin,
            extraTags: java.util.ArrayList<String>
        ) = Intent(context, SupportRequestFormActivity::class.java).apply {
            putExtra(ORIGIN_KEY, origin)
            putStringArrayListExtra(EXTRA_TAGS_KEY, ArrayList(extraTags))
        }
    }
}
