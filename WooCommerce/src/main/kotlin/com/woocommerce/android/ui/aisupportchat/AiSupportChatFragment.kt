package com.woocommerce.android.ui.aisupportchat

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportAreaType
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatSupportArea
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AiSupportChatFragment : Fragment(), MenuProvider {
    private val viewModel: AiSupportChatViewModel by viewModels()
    private var progressDialog: CustomProgressDialog? = null
    private var contactSupportMenuItem: MenuItem? = null
    private var markResolvedMenuItem: MenuItem? = null

    @Inject lateinit var zendeskSettings: ZendeskSettings

    @Inject lateinit var zendeskTicketRepository: ZendeskTicketRepository

    @Inject lateinit var selectedSite: SelectedSite

    private val supportRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            viewModel.onSupportTicketCreated()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        composeView {
            AiSupportChatScreen(viewModel = viewModel)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        zendeskSettings.setup(context = requireContext())
        setupContactSupportToolbarAction()
        observeViewState()
        observeViewEvents()
        viewModel.onLaunchModeLoaded(AiSupportChatActivity.launchModeFrom(requireActivity().intent))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        contactSupportMenuItem = null
        markResolvedMenuItem = null
        hideProgressDialog()
    }

    private fun setupContactSupportToolbarAction() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_ai_support_chat, menu)
        contactSupportMenuItem = menu.findItem(R.id.menu_contact_support)
        markResolvedMenuItem = menu.findItem(R.id.menu_mark_resolved)
        contactSupportMenuItem?.isVisible = viewModel.viewState.value.canContactHumanSupportFromToolbar
        markResolvedMenuItem?.isVisible = viewModel.viewState.value.shouldShowResolvedButton
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_contact_support -> {
                viewModel.onContactSupportClicked(HumanSupportContactSource.TOOLBAR)
                return true
            }
            R.id.menu_mark_resolved -> {
                viewModel.onMarkResolvedClicked()
                return true
            }
        }

        return false
    }

    private fun observeViewState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { state ->
                    contactSupportMenuItem?.isVisible = state.canContactHumanSupportFromToolbar
                    markResolvedMenuItem?.isVisible = state.shouldShowResolvedButton
                }
            }
        }
    }

    private fun observeViewEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ContactHumanSupport -> handleContactHumanSupport(event)
            }
        }
    }

    private fun handleContactHumanSupport(event: ContactHumanSupport) {
        val supportArea = event.supportArea
        zendeskSettings.refreshIdentity()
        if (supportArea != null && supportArea.isHighConfidence && zendeskSettings.isIdentitySet) {
            createTicketDirectly(event, supportArea)
        } else {
            openSupportRequestForm(event)
        }
    }

    private fun createTicketDirectly(event: ContactHumanSupport, supportArea: SupportChatSupportArea) {
        showProgressDialog()
        viewLifecycleOwner.lifecycleScope.launch {
            zendeskTicketRepository.createRequest(
                context = requireContext(),
                origin = HelpOrigin.AI_TROUBLESHOOTING,
                ticketType = supportArea.ticketType,
                selectedSite = selectedSite.getIfExists(),
                subject = supportArea.subject,
                description = event.description,
                extraTags = event.extraTags,
                siteAddress = selectedSite.getIfExists()?.url.orEmpty()
            ).collect { result ->
                hideProgressDialog()
                result
                    .onSuccess {
                        viewModel.onSupportTicketCreated()
                        showTicketCreatedDialog()
                    }
                    .onFailure {
                        openSupportRequestForm(event)
                    }
            }
        }
    }

    private fun openSupportRequestForm(event: ContactHumanSupport) {
        supportRequestLauncher.launch(
            SupportRequestFormActivity.createIntent(
                context = requireContext(),
                origin = HelpOrigin.AI_TROUBLESHOOTING,
                extraTags = ArrayList(event.extraTags),
                preselectedTicketType = event.supportArea?.ticketType,
                prefilledSubject = event.supportArea?.subject,
                prefilledMessage = event.description,
                prefilledSiteAddress = selectedSite.getIfExists()?.url.orEmpty()
            )
        )
    }

    private fun showProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(R.string.support_request_loading_title),
            getString(R.string.support_request_loading_message)
        ).also {
            it.isCancelable = false
            it.show(childFragmentManager, CustomProgressDialog.TAG)
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showTicketCreatedDialog() {
        WooDialog.showDialog(
            activity = requireActivity(),
            titleId = R.string.support_request_success_title,
            messageId = R.string.support_request_success_message,
            positiveButtonId = R.string.support_request_dialog_action,
            posBtnAction = { _, _ -> requireActivity().finish() }
        )
    }

    private val ContactHumanSupport.description: String
        get() = listOf(getString(R.string.ai_support_chat_escalation_transcript_header), transcript)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")

    private val ContactHumanSupport.extraTags: List<String>
        get() = buildList {
            add(SOURCE_TAG)
            add(AI_SKIP_TAG)
            supportArea?.topic?.takeIf { it.isNotBlank() }?.let { add(it) }
        }

    private val SupportChatSupportArea.ticketType: TicketType
        get() = when (areaType) {
            SupportAreaType.MOBILE_APP -> TicketType.MobileApp
            SupportAreaType.CARD_READER -> TicketType.InPersonPayments
            SupportAreaType.WOO_PAYMENTS -> TicketType.Payments
            SupportAreaType.WOO_COMMERCE_PLUGIN -> TicketType.WooPlugin
            SupportAreaType.OTHER_EXTENSION_PLUGIN -> TicketType.OtherPlugins
        }

    private val SupportChatSupportArea.subject: String
        get() = getString(
            when (areaType) {
                SupportAreaType.MOBILE_APP -> R.string.ai_support_chat_support_request_subject_mobile_app
                SupportAreaType.CARD_READER -> R.string.ai_support_chat_support_request_subject_card_reader
                SupportAreaType.WOO_PAYMENTS -> R.string.ai_support_chat_support_request_subject_woo_payments
                SupportAreaType.WOO_COMMERCE_PLUGIN -> R.string.ai_support_chat_support_request_subject_woo_plugin
                SupportAreaType.OTHER_EXTENSION_PLUGIN -> R.string.ai_support_chat_support_request_subject_other_plugin
            }
        )

    private companion object {
        const val SOURCE_TAG = "in_app_support_escalate"
        const val AI_SKIP_TAG = "ai_skip"
    }
}
