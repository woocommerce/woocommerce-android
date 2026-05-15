package com.woocommerce.android.support.requests

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivitySupportRequestFormBinding
import com.woocommerce.android.extensions.adjustActivityTransition
import com.woocommerce.android.extensions.doOnApplyWindowInsets
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.extensions.serializable
import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationFailed
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationSucceeded
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.ShowSupportIdentityInputDialog
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskSettings
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

    private val diagnosticLog by lazy {
        intent.extras?.getString(DIAGNOSTIC_LOG_KEY)
    }

    private val prefill by lazy {
        SupportRequestFormViewModel.Prefill(
            ticketType = intent.extras?.parcelable(PREFILL_TICKET_TYPE_KEY),
            subject = intent.extras?.getString(PREFILL_SUBJECT_KEY).orEmpty(),
            siteAddress = intent.extras?.getString(PREFILL_SITE_ADDRESS_KEY).orEmpty(),
            message = intent.extras?.getString(PREFILL_MESSAGE_KEY).orEmpty()
        )
    }

    private var progressDialog: CustomProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        zendeskSettings.setup(context = this)

        ActivitySupportRequestFormBinding.inflate(layoutInflater).apply {
            this.root.doOnApplyWindowInsets(
                insetsMask = WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.ime(),
                consumeInsets = true
            ) { insets ->
                this.root.updatePadding(
                    left = insets.left,
                    right = insets.right,
                    bottom = insets.bottom
                )
                this.appBarLayout.updatePadding(
                    top = insets.top
                )
            }
            setContentView(root)
            setupActionBar()
            observeViewEvents(this)
            observeViewModelEvents(this)
            applyPrefill(this)
        }
        viewModel.onViewCreated()

        if (isPOS()) {
            adjustActivityTransition(
                overrideTransitionOpen = true,
                enterAnim = R.anim.woopos_slide_in_right,
                exitAnim = R.anim.woopos_slide_out_left,
            )
        }
    }

    override fun finish() {
        super.finish()
        adjustExitTransition()
    }

    private fun adjustExitTransition() {
        if (isPOS()) {
            adjustActivityTransition(
                overrideTransitionOpen = false,
                R.anim.woopos_slide_in_left,
                R.anim.woopos_slide_out_right
            )
        }
    }

    private fun isPOS(): Boolean {
        val origin: HelpOrigin? = intent.extras?.serializable(ORIGIN_KEY)
        return origin == HelpOrigin.POS
    }

    private fun ActivitySupportRequestFormBinding.setupActionBar() {
        setSupportActionBar(toolbar.toolbar as Toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewEvents(binding: ActivitySupportRequestFormBinding) {
        binding.requestSubject.setOnTextChangedListener { viewModel.onSubjectChanged(it.toString()) }
        binding.requestSiteAddress.setOnTextChangedListener { viewModel.onSiteAddressChanged(it.toString()) }
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
                extraTags = extraTags,
                diagnosticLog = diagnosticLog
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
        setResult(Activity.RESULT_OK)
        AlertDialog.Builder(this)
            .setTitle(R.string.support_request_success_title)
            .setMessage(R.string.support_request_success_message)
            .setPositiveButton(R.string.support_request_dialog_action) { _, _ ->
                finish()
            }
            .show()
    }

    private fun showRequestCreationFailureDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.support_request_error_title)
            .setMessage(R.string.support_request_error_message)
            .setPositiveButton(R.string.support_request_dialog_action, null)
            .show()
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
                diagnosticLog = diagnosticLog,
                selectedEmail = email,
                selectedName = name
            )
        }
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_IDENTITY_FORM_VIEWED)
    }

    private fun applyPrefill(binding: ActivitySupportRequestFormBinding) {
        viewModel.onPrefillReceived(prefill)

        binding.requestSubject.setTextIfDifferent(prefill.subject)
        binding.requestSiteAddress.setTextIfDifferent(prefill.siteAddress)
        binding.requestMessage.setText(prefill.message)
        when (prefill.ticketType) {
            TicketType.MobileApp -> binding.helpOptionsGroup.check(binding.mobileAppOption.id)
            TicketType.InPersonPayments -> binding.helpOptionsGroup.check(binding.ippOption.id)
            TicketType.Payments -> binding.helpOptionsGroup.check(binding.paymentsOption.id)
            TicketType.WooPlugin -> binding.helpOptionsGroup.check(binding.wooPluginOption.id)
            TicketType.OtherPlugins -> binding.helpOptionsGroup.check(binding.otherOption.id)
            null -> Unit
        }
    }

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
        private const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"
        private const val DIAGNOSTIC_LOG_KEY = "DIAGNOSTIC_LOG_KEY"
        private const val PREFILL_TICKET_TYPE_KEY = "PREFILL_TICKET_TYPE_KEY"
        private const val PREFILL_SUBJECT_KEY = "PREFILL_SUBJECT_KEY"
        private const val PREFILL_SITE_ADDRESS_KEY = "PREFILL_SITE_ADDRESS_KEY"
        private const val PREFILL_MESSAGE_KEY = "PREFILL_MESSAGE_KEY"

        @JvmStatic
        @Suppress("LongParameterList")
        fun createIntent(
            context: Context,
            origin: HelpOrigin,
            extraTags: java.util.ArrayList<String>,
            diagnosticLog: String? = null,
            preselectedTicketType: TicketType? = null,
            prefilledSubject: String? = null,
            prefilledSiteAddress: String? = null,
            prefilledMessage: String? = null
        ) = Intent(context, SupportRequestFormActivity::class.java).apply {
            putExtra(ORIGIN_KEY, origin)
            putStringArrayListExtra(EXTRA_TAGS_KEY, ArrayList(extraTags))
            diagnosticLog?.let { putExtra(DIAGNOSTIC_LOG_KEY, it) }
            preselectedTicketType?.let { putExtra(PREFILL_TICKET_TYPE_KEY, it) }
            prefilledSubject?.let { putExtra(PREFILL_SUBJECT_KEY, it) }
            prefilledSiteAddress?.let { putExtra(PREFILL_SITE_ADDRESS_KEY, it) }
            prefilledMessage?.let { putExtra(PREFILL_MESSAGE_KEY, it) }
        }
    }
}
